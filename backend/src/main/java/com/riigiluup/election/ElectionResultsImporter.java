package com.riigiluup.election;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Imports the RK_2023 election results and attaches each currently seated MP's
 * personal result (votes, mandate type, district) by matching on name. Elected
 * candidates who are no longer seated (e.g. became ministers and were replaced)
 * simply have no matching member and are skipped; the data is immutable, so this
 * is an idempotent upsert meant to run once (admin-triggered).
 */
@Service
public class ElectionResultsImporter {

    private static final Logger log = LoggerFactory.getLogger(ElectionResultsImporter.class);
    private static final String ELECTION_CODE = "RK_2023";
    private static final String SOURCE_NAME = "valimised";
    private static final String JOB_NAME = "elections.rk2023-import";

    private final ElectionResultsClient client;
    private final PlenaryMemberRepository memberRepo;
    private final ElectionResultRepository repo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public ElectionResultsImporter(ElectionResultsClient client,
                                   PlenaryMemberRepository memberRepo,
                                   ElectionResultRepository repo,
                                   ImportRunLogRepository runLogRepo,
                                   PlatformTransactionManager txManager) {
        this.client = client;
        this.memberRepo = memberRepo;
        this.repo = repo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    public int importRk2023() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(SOURCE_NAME)
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int matched = 0;
        try {
            List<ElectionCandidateDto> candidates = client.fetchRk2023Results(); // network, outside any tx
            seen = candidates.size();
            // An empty candidate list means a broken/changed source, not an empty election —
            // a full refresh here would wipe the table. Keep the existing rows and fail the
            // run, so callers see the same FAILED outcome as the run log.
            if (candidates.isEmpty()) {
                log.warn("{} returned zero candidates — keeping existing election-result rows", ELECTION_CODE);
                throw new IllegalStateException(
                        ELECTION_CODE + " source returned zero candidates; existing rows kept");
            }
            matched = tx.execute(status -> replaceAll(candidates));
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(matched);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return matched;
    }

    /** The electoral-footprint layer: other published elections matched to current MPs by name. */
    public static final List<String> CAMPAIGN_CODES = List.of("EP_2024", "KOV_2021", "KOV_2025");

    /**
     * Imports the electoral footprint (EP / KOV campaigns) for current MPs. Each code is a
     * separate immutable election; a failure on one does not abort the others. Returns the
     * matched-row count per code.
     */
    public Map<String, Integer> importCampaigns() {
        Map<String, Integer> result = new HashMap<>();
        for (String code : CAMPAIGN_CODES) {
            try {
                result.put(code, importCampaign(code));
            } catch (Exception e) {
                log.warn("campaign import {} failed: {}", code, e.toString());
                result.put(code, -1); // -1 signals a failed code, distinct from 0 matches
            }
        }
        return result;
    }

    /**
     * One election as a footprint layer: a current MP gets a row only when their full name is
     * unique in the roster AND matches exactly one candidate of that name in the election
     * (elected or not). Ambiguous names — a roster collision or several same-name candidates —
     * are skipped rather than guessed, since the feed carries no birth date to disambiguate.
     * Immutable source, so this is an idempotent full refresh per code.
     */
    public int importCampaign(String code) {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(SOURCE_NAME)
                .jobName("elections." + code.toLowerCase(Locale.ROOT) + "-import")
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int matched = 0;
        try {
            List<ElectionCandidateDto> candidates = client.fetchResults(code); // network, outside tx
            seen = candidates.size();
            if (candidates.isEmpty()) {
                throw new IllegalStateException(code + " source returned zero candidates; existing rows kept");
            }
            matched = tx.execute(status -> replaceCampaign(code, candidates));
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(matched);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return matched;
    }

    private int replaceCampaign(String code, List<ElectionCandidateDto> candidates) {
        List<CampaignMatch> matches = matchCampaign(memberRepo.findAll(), candidates);
        repo.deleteByElectionCode(code); // full refresh — immutable source, keeps this idempotent
        Instant now = Instant.now();
        for (CampaignMatch match : matches) {
            PlenaryMember m = match.member();
            ElectionCandidateDto c = match.candidate();
            repo.save(ElectionResult.builder()
                    .memberExternalId(m.getExternalId())
                    .electionCode(code)
                    .elected(c.elected())
                    .personalVotes(c.votes())
                    .mandateType(c.elected() ? c.mandateType() : null)
                    .districtNumber(c.districtNumber())
                    .partyName(c.partyName())
                    .ballotNumber(c.registrationNumber())
                    .importedAt(now)
                    .build());
        }
        log.info("Campaign import {}: {} MPs matched", code, matches.size());
        return matches.size();
    }

    /** A confident MP-to-candidate pairing for a campaign import. */
    public record CampaignMatch(PlenaryMember member, ElectionCandidateDto candidate) {}

    /**
     * High-confidence name matches between the MP roster and an election's candidates: a pairing
     * is kept only when the full name is unique on BOTH sides (one roster member, one candidate).
     * A shared name — a roster collision or several same-name candidates — is dropped rather than
     * guessed, because the feed has no birth date to disambiguate. No surname-only fallback here,
     * unlike the RK seat import: the KOV candidate pool is large and a loose match would risk
     * attributing a namesake's candidacy to an MP.
     */
    static List<CampaignMatch> matchCampaign(List<PlenaryMember> members,
                                             List<ElectionCandidateDto> candidates) {
        Map<String, List<PlenaryMember>> rosterByName = new HashMap<>();
        for (PlenaryMember m : members) {
            rosterByName.computeIfAbsent(nameKey(m.getFirstName(), m.getLastName()), k -> new ArrayList<>())
                    .add(m);
        }
        Map<String, List<ElectionCandidateDto>> candByName = new HashMap<>();
        for (ElectionCandidateDto c : candidates) {
            if (c.surname() == null || c.surname().isBlank()) continue;
            candByName.computeIfAbsent(nameKey(c.forename(), c.surname()), k -> new ArrayList<>())
                    .add(c);
        }
        List<CampaignMatch> out = new ArrayList<>();
        for (Map.Entry<String, List<PlenaryMember>> entry : rosterByName.entrySet()) {
            if (entry.getValue().size() != 1) continue;      // ambiguous roster name -> skip
            List<ElectionCandidateDto> hits = candByName.get(entry.getKey());
            if (hits == null || hits.size() != 1) continue;  // absent or ambiguous in election -> skip
            out.add(new CampaignMatch(entry.getValue().get(0), hits.get(0)));
        }
        return out;
    }

    /** Substitute members (asendusliige) enter mid-term when an elected MP resigns/becomes a
     *  minister; they ran but were not directly elected, so they carry this pseudo mandate type. */
    public static final String SUBSTITUTE_MANDATE = "SUBSTITUTE";

    private int replaceAll(List<ElectionCandidateDto> candidates) {
        List<PlenaryMember> members = memberRepo.findAll();
        Map<String, PlenaryMember> exact = activeWinsNameIndex(members);
        // Fallback index by surname so a compound/abbreviated forename ("Kalev" vs the source's
        // "Grigore-Kalev") can still be reconciled by first-name token overlap.
        Map<String, List<PlenaryMember>> bySurname = new HashMap<>();
        for (PlenaryMember m : members) {
            bySurname.computeIfAbsent(norm(m.getLastName()), k -> new ArrayList<>()).add(m);
        }

        repo.deleteByElectionCode(ELECTION_CODE); // full refresh — immutable source, keeps this idempotent
        Instant now = Instant.now();
        Set<String> claimed = new HashSet<>();    // one row per member; the elected pass wins
        int elected = 0, substitutes = 0;

        // Two passes so a directly-elected mandate always beats a same-surname substitute record.
        for (boolean electedPass : new boolean[]{true, false}) {
            for (ElectionCandidateDto c : candidates) {
                if (c.elected() != electedPass) continue;
                PlenaryMember m = matchMember(c, exact, bySurname);
                if (m == null) continue;
                // A losing candidate matters only if they are currently seated (i.e. a substitute).
                if (!c.elected() && !m.isActive()) continue;
                if (!claimed.add(m.getExternalId())) continue;
                repo.save(ElectionResult.builder()
                        .memberExternalId(m.getExternalId())
                        .electionCode(ELECTION_CODE)
                        .elected(c.elected())
                        .personalVotes(c.votes())
                        .mandateType(c.elected() ? c.mandateType() : SUBSTITUTE_MANDATE)
                        .districtNumber(c.districtNumber())
                        .partyName(c.partyName())
                        .ballotNumber(c.registrationNumber())
                        .importedAt(now)
                        .build());
                if (c.elected()) elected++; else substitutes++;
            }
        }
        log.info("Election import {}: {} elected + {} seated substitutes matched to members",
                ELECTION_CODE, elected, substitutes);
        return elected + substitutes;
    }

    /** Exact name match, else surname + first-name token overlap when unambiguous (one member). */
    static PlenaryMember matchMember(ElectionCandidateDto c,
                                     Map<String, PlenaryMember> exact,
                                     Map<String, List<PlenaryMember>> bySurname) {
        PlenaryMember m = exact.get(nameKey(c.forename(), c.surname()));
        if (m != null) return m;
        List<PlenaryMember> sameSurname = bySurname.get(norm(c.surname()));
        if (sameSurname == null) return null;
        Set<String> candTokens = tokens(c.forename());
        List<PlenaryMember> hits = sameSurname.stream()
                .filter(pm -> !Collections.disjoint(tokens(pm.getFirstName()), candTokens))
                .toList();
        return hits.size() == 1 ? hits.get(0) : null;
    }

    private static Set<String> tokens(String name) {
        Set<String> out = new HashSet<>();
        if (name == null) return out;
        for (String p : name.replace('-', ' ').trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Index members by name, letting ACTIVE members win collisions — a returning name then
     * attaches to the sitting MP rather than an inactive same-name record from an earlier term.
     */
    static Map<String, PlenaryMember> activeWinsNameIndex(List<PlenaryMember> members) {
        List<PlenaryMember> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparing(PlenaryMember::isActive)); // active (true) sorts last → overwrites
        Map<String, PlenaryMember> byName = new HashMap<>();
        for (PlenaryMember m : sorted) {
            byName.put(nameKey(m.getFirstName(), m.getLastName()), m);
        }
        return byName;
    }

    static String nameKey(String forename, String surname) {
        String f = forename == null ? "" : forename.trim().toLowerCase(Locale.ROOT);
        String s = surname == null ? "" : surname.trim().toLowerCase(Locale.ROOT);
        return f + "|" + s;
    }
}
