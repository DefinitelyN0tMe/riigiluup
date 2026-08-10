package com.riigiluup.election;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads the pre-2023 Riigikogu electoral history for current MPs from a bundled dataset compiled
 * by Martin Mölder (Johan Skytte Institute, University of Tartu), covering RK candidates 1992-2019
 * with birth dates. Used with his permission and attributed on display.
 *
 * <p>Matching is by <b>date of birth + surname-token overlap</b>: a candidacy row attaches to a
 * member when their birth dates are equal AND at least one surname token is shared. Surname-change
 * variants (marriage etc.) are pre-baked into each row's {@code surname_tokens} column from
 * Mölder's name-variant map, so a member whose surname differs from the historical one still
 * matches (e.g. Simson↔Must), while the birth date rules out namesakes. Only current MPs carry a
 * birth date in our roster, so this realises the "electoral history" idea for the sitting Riigikogu.
 *
 * <p>The bundled resource is immutable, so this is an idempotent full replace of the historical
 * rows (kept separate from the open-data footprint by the {@code historical} flag).
 */
@Service
public class HistoricalElectionImporter {

    private static final Logger log = LoggerFactory.getLogger(HistoricalElectionImporter.class);
    private static final String RESOURCE = "election/rk_history_1992_2019.csv";
    private static final String SOURCE_NAME = "molder-historical";
    private static final String JOB_NAME = "elections.historical-import";

    private final PlenaryMemberRepository memberRepo;
    private final ElectionResultRepository repo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public HistoricalElectionImporter(PlenaryMemberRepository memberRepo,
                                      ElectionResultRepository repo,
                                      ImportRunLogRepository runLogRepo,
                                      PlatformTransactionManager txManager) {
        this.memberRepo = memberRepo;
        this.repo = repo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** One parsed candidacy row from the bundled CSV. */
    private record Candidacy(LocalDate birth, Set<String> tokens, String name, int year,
                             String districtName, String party, int votes, boolean elected) {}

    public int importHistorical() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(SOURCE_NAME)
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int matched = 0;
        try {
            List<Candidacy> rows = parseBundle();
            seen = rows.size();
            if (rows.isEmpty()) {
                throw new IllegalStateException("historical election bundle is empty; existing rows kept");
            }
            matched = tx.execute(status -> replaceAll(rows));
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            throw new RuntimeException("historical election import failed", e);
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(matched);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return matched;
    }

    private int replaceAll(List<Candidacy> rows) {
        // Index members by birth date; only members with a DOB can be matched (current MPs).
        Map<LocalDate, List<PlenaryMember>> byBirth = new HashMap<>();
        for (PlenaryMember m : memberRepo.findAll()) {
            if (m.getDateOfBirth() == null) continue;
            byBirth.computeIfAbsent(m.getDateOfBirth(), k -> new ArrayList<>()).add(m);
        }

        // One row per (member, year); if the dataset repeats a member in a year, prefer the
        // elected candidacy, then the higher vote count, so a duplicate ballot row can never
        // downgrade an "elected" year to "not elected".
        Map<String, ElectionResult> best = new HashMap<>();
        Instant now = Instant.now();
        for (Candidacy c : rows) {
            List<PlenaryMember> members = byBirth.get(c.birth());
            if (members == null) continue;
            for (PlenaryMember m : members) {
                // Surname token overlap AND a forename token/initial match. The birth date already
                // makes a wrong pairing unlikely; the forename check guards the residual case of a
                // different person who happens to share a birth date and a surname token.
                if (!overlaps(memberTokens(m), c.tokens())) continue;
                if (!forenameMatches(m.getFirstName(), c.name())) continue;
                String code = "RK_" + c.year();
                String key = m.getExternalId() + "|" + code;
                ElectionResult existing = best.get(key);
                if (existing != null && !isBetter(c, existing)) continue;
                best.put(key, ElectionResult.builder()
                        .memberExternalId(m.getExternalId())
                        .electionCode(code)
                        .elected(c.elected())
                        .personalVotes(c.votes())
                        .mandateType(null)          // historical rows record votes/elected, not mandate class
                        .districtNumber(null)
                        .districtName(c.districtName())
                        .partyName(c.party())
                        .ballotNumber(null)
                        .historical(true)
                        .importedAt(now)
                        .build());
            }
        }

        // Never wipe the layer when nothing matched (empty/DOB-less roster): keep the existing rows
        // and fail the run, mirroring the open-data importers' "empty source" guard.
        if (best.isEmpty()) {
            throw new IllegalStateException(
                    "historical import matched zero MPs (roster empty or without birth dates); existing rows kept");
        }
        repo.deleteAllHistorical();
        repo.saveAll(best.values());
        log.info("Historical election import: {} candidacies -> {} rows for current MPs",
                rows.size(), best.size());
        return best.size();
    }

    /** Prefer an elected candidacy, then a higher vote count, when a (member, year) repeats. */
    private static boolean isBetter(Candidacy c, ElectionResult existing) {
        if (c.elected() != existing.isElected()) return c.elected();
        return c.votes() > existing.getPersonalVotes();
    }

    /**
     * A forename match between the roster forename and the candidate's display name: either a
     * shared forename token (handles compound forenames like "Grigore-Kalev" vs "Kalev") or a
     * shared first initial. Surname changes never touch the forename, so this stays permissive
     * for real matches while ruling out a same-birth-date, same-surname stranger.
     */
    private static boolean forenameMatches(String memberFirstName, String candidateName) {
        Set<String> memberFore = tokenize(memberFirstName);
        Set<String> candFore = forenameTokens(candidateName);
        if (candFore.isEmpty() || memberFore.isEmpty()) return true; // no forename info -> don't block
        if (!java.util.Collections.disjoint(memberFore, candFore)) return true;
        char mi = firstChar(memberFore);
        for (String t : candFore) if (!t.isEmpty() && t.charAt(0) == mi) return true;
        return false;
    }

    /** Forename tokens = every token of a full name except the last (the surname). */
    private static Set<String> forenameTokens(String fullName) {
        if (fullName == null) return Set.of();
        String[] parts = fullName.trim().toLowerCase(Locale.ROOT).split("[-\\s]+");
        Set<String> out = new HashSet<>();
        for (int i = 0; i < parts.length - 1; i++) if (!parts[i].isEmpty()) out.add(parts[i]);
        return out;
    }

    private static char firstChar(Set<String> tokens) {
        for (String t : tokens) if (!t.isEmpty()) return t.charAt(0);
        return '\0';
    }

    private static Set<String> memberTokens(PlenaryMember m) {
        return tokenize(m.getLastName());
    }

    private static boolean overlaps(Set<String> a, Set<String> b) {
        for (String t : a) if (b.contains(t)) return true;
        return false;
    }

    private static Set<String> tokenize(String surname) {
        Set<String> out = new HashSet<>();
        if (surname == null) return out;
        for (String p : surname.trim().toLowerCase(Locale.ROOT).split("[-\\s]+")) {
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    private List<Candidacy> parseBundle() {
        List<Candidacy> out = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(RESOURCE);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine(); // birth,surname_tokens,name,year,district_name,party,votes,elected
            if (header == null) return out;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = splitCsv(line);
                if (f.length < 8) continue;
                try {
                    Set<String> tokens = new HashSet<>(Arrays.asList(f[1].split("\\|")));
                    out.add(new Candidacy(
                            LocalDate.parse(f[0]),
                            tokens,
                            f[2],
                            Integer.parseInt(f[3].trim()),
                            f[4],
                            f[5],
                            parseIntSafe(f[6]),
                            "1".equals(f[7].trim())));
                } catch (RuntimeException ex) {
                    log.debug("skipping malformed historical row: {}", line);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
        return out;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** Minimal RFC-4180 field splitter: handles double-quoted fields containing commas and "". */
    static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                out.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
