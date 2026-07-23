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
