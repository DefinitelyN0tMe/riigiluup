package com.riigiluup.ingestion.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.person.MpPartyMembership;
import com.riigiluup.person.MpPartyMembershipRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One-shot importer that cross-references our PlenaryMember rows with Wikidata
 * (CC0 license — free to use for any purpose).
 *
 * <p>Fetches every person who has ever held the position "member of the Riigikogu"
 * (Wikidata Q21100241) via SPARQL, plus per-language Wikipedia article URLs. Matches to
 * existing MPs by (fullName, dateOfBirth); a name-only fallback is used only when the name is
 * unambiguous on both sides. Matches update the four Wikidata columns on plenary_member.
 *
 * <p>The three SPARQL calls run outside any transaction; each write pass commits in its own
 * short {@link TransactionTemplate} so no DB connection is held across HTTP (same pattern as
 * the Riigikogu importers).
 *
 * <p>Idempotent: safe to re-run. Not scheduled — triggered by
 * {@code POST /api/v1/admin/import/wikidata}.
 */
@Slf4j
@Service
public class WikidataImporter {

    private static final String JOB_NAME = "wikidata.mp-crossref";
    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    // --- Input hardening (Wikidata is CC0 but world-editable — treat every value as untrusted).
    //   Wikidata entity ids are the letter Q followed by digits; anything else is a garbage QID.
    private static final Pattern QID_PATTERN = Pattern.compile("^Q[0-9]{1,15}$");
    //   party_label column is VARCHAR(256); education/positions are TEXT but we still cap them so a
    //   vandalised label can't balloon the row. Bio fields get more room than a single party label.
    private static final int PARTY_LABEL_MAX = 256;
    private static final int BIO_MAX = 2000;
    //   Default allow-list of the Wikidata QIDs of Estonian parliamentary parties (+ direct
    //   predecessors). Soft list: rows with a QID outside it are still stored, but logged for
    //   review — see enrichParties. Overridable via riigiluup.wikidata.allowed-party-qids.
    static final String DEFAULT_ALLOWED_PARTY_QIDS =
            "Q738947,"      // Estonian Reform Party
            + "Q163347,"    // Isamaa
            + "Q1428217,"   // Pro Patria Union (Isamaaliit — predecessor of Isamaa)
            + "Q928652,"    // Estonian Centre Party
            + "Q794028,"    // Conservative People's Party of Estonia (EKRE)
            + "Q913551,"    // Social Democratic Party (Estonia)
            + "Q56249403,"  // Estonia 200
            + "Q113677848," // Parempoolsed
            + "Q3896796";   // People's Party of Republicans and Conservatives (predecessor)
    // Every person ever elected as Riigikogu MP.
    //   Q21100241 = "member of the Riigikogu" (position held).
    //   Q217799 = Riigikogu (institution) — used earlier by mistake; that returns only
    //   the handful of people whose entire position ONLY is "the parliament", not MPs.
    private static final String SPARQL = """
            SELECT ?person ?personLabel ?dateOfBirth ?enwiki ?etwiki ?ruwiki WHERE {
              ?person wdt:P39 wd:Q21100241.
              OPTIONAL { ?person wdt:P569 ?dateOfBirth. }
              OPTIONAL {
                ?enwiki schema:about ?person;
                        schema:isPartOf <https://en.wikipedia.org/>.
              }
              OPTIONAL {
                ?etwiki schema:about ?person;
                        schema:isPartOf <https://et.wikipedia.org/>.
              }
              OPTIONAL {
                ?ruwiki schema:about ?person;
                        schema:isPartOf <https://ru.wikipedia.org/>.
              }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "et,en,ru". }
            }
            """;

    // Follow-up query for the matched MPs only (VALUES list), so it stays bounded: education
    // institutions (P69) and other offices held (P39, excluding the MP position itself),
    // as "; "-joined Estonian labels. No label service (would clash with GROUP BY).
    private static final String BIO_SPARQL_TEMPLATE = """
            SELECT ?person
              (GROUP_CONCAT(DISTINCT ?eduL; separator="; ") AS ?education)
              (GROUP_CONCAT(DISTINCT ?posL; separator="; ") AS ?positions)
            WHERE {
              VALUES ?person { %s }
              OPTIONAL { ?person wdt:P69 ?edu. ?edu rdfs:label ?eduL. FILTER(lang(?eduL) = "et") }
              OPTIONAL { ?person wdt:P39 ?pos. FILTER(?pos != wd:Q21100241) ?pos rdfs:label ?posL. FILTER(lang(?posL) = "et") }
            }
            GROUP BY ?person
            """;

    // Party membership (P102) with start/end qualifiers — statement-node form (not wdt:) so the
    // dates come through. One row per (person, membership statement). et label, en fallback.
    private static final String PARTY_SPARQL_TEMPLATE = """
            SELECT ?person ?party ?partyLabelEt ?partyLabelEn ?start ?end WHERE {
              VALUES ?person { %s }
              ?person p:P102 ?stmt.
              ?stmt ps:P102 ?party.
              OPTIONAL { ?stmt pq:P580 ?start. }
              OPTIONAL { ?stmt pq:P582 ?end. }
              OPTIONAL { ?party rdfs:label ?partyLabelEt. FILTER(lang(?partyLabelEt) = "et") }
              OPTIONAL { ?party rdfs:label ?partyLabelEn. FILTER(lang(?partyLabelEn) = "en") }
            }
            """;

    private final PlenaryMemberRepository memberRepo;
    private final MpPartyMembershipRepository partyMembershipRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;
    /** Known Estonian party QIDs; a P102 QID outside it is stored but flagged (see enrichParties). */
    private final Set<String> allowedPartyQids;
    private final RestClient rest = RestClient.builder()
            // Wikidata's UA policy: identify the client + contact so they can reach out.
            .defaultHeader(HttpHeaders.USER_AGENT, "riigiluup/0.1 (riigiluup@gmail.com)")
            .defaultHeader(HttpHeaders.ACCEPT, "application/sparql-results+json")
            .build();

    public WikidataImporter(PlenaryMemberRepository memberRepo,
                            MpPartyMembershipRepository partyMembershipRepo,
                            ImportRunLogRepository runLogRepo,
                            PlatformTransactionManager txManager,
                            @Value("${riigiluup.wikidata.allowed-party-qids:" + DEFAULT_ALLOWED_PARTY_QIDS + "}")
                            List<String> allowedPartyQids) {
        this.memberRepo = memberRepo;
        this.partyMembershipRepo = partyMembershipRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
        this.allowedPartyQids = allowedPartyQids.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Cross-reference pass result. The member entities are detached once the tx commits. */
    private record CrossrefResult(int seen, int matched, Map<String, PlenaryMember> matchedByQid) {
    }

    public ImportRunLog runOnce() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName("wikidata").jobName(JOB_NAME)
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, matched = 0;
        try {
            JsonNode result = fetchSparql(SPARQL); // network, outside any tx
            if (result == null) throw new IllegalStateException("empty Wikidata response");
            JsonNode bindings = result.path("results").path("bindings");

            CrossrefResult crossref = tx.execute(status -> applyCrossref(bindings));
            seen = crossref.seen();
            matched = crossref.matched();

            enrichBio(crossref.matchedByQid());
            enrichParties(crossref.matchedByQid());
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("wikidata cross-ref failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(matched);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private JsonNode fetchSparql(String query) {
        return rest.get()
                .uri(SPARQL_ENDPOINT + "?query={q}&format=json", query)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Match the fetched rows to plenary_member and stamp QIDs + Wikipedia URLs (one tx). */
    private CrossrefResult applyCrossref(JsonNode bindings) {
        int seen = 0, matched = 0;

        // Index MPs by (fullNameLower, dateOfBirth) — highest-specificity match — and by name.
        List<PlenaryMember> mps = memberRepo.findAll();
        Map<String, PlenaryMember> byNameDob = new HashMap<>();
        Map<String, PlenaryMember> byNameOnly = new HashMap<>();
        Map<String, Integer> ourNameFreq = new HashMap<>();
        for (PlenaryMember m : mps) {
            String nameKey = m.getFullName().toLowerCase();
            byNameOnly.putIfAbsent(nameKey, m);
            ourNameFreq.merge(nameKey, 1, Integer::sum);
            if (m.getDateOfBirth() != null) {
                byNameDob.put(nameKey + "|" + m.getDateOfBirth(), m);
            }
        }

        // The SPARQL set spans every Riigikogu member since 1919, so a name-only match risks
        // stamping a historical namesake onto a current MP. Count Wikidata labels so a name-only
        // match is only trusted when it's unambiguous on both sides.
        Map<String, Integer> wdNameFreq = new HashMap<>();
        for (JsonNode row : bindings) {
            String label = row.path("personLabel").path("value").asText(null);
            if (label != null) wdNameFreq.merge(label.toLowerCase(), 1, Integer::sum);
        }

        Set<UUID> assignedByDob = new HashSet<>(); // matched by the strong name+DOB key — never override
        Map<String, PlenaryMember> matchedByQid = new HashMap<>(); // for the follow-up bio enrichment
        for (JsonNode row : bindings) {
            seen++;
            String qUri = row.path("person").path("value").asText(null);
            String label = row.path("personLabel").path("value").asText(null);
            if (qUri == null || label == null) continue;
            String qid = qUri.substring(qUri.lastIndexOf('/') + 1);
            if (!isValidQid(qid)) {
                log.warn("wikidata: skipping row with malformed person QID '{}' (label {})", qid, label);
                continue;
            }

            LocalDate dob = null;
            String dobRaw = row.path("dateOfBirth").path("value").asText(null);
            if (dobRaw != null) {
                try {
                    // ISO instants like "1962-03-14T00:00:00Z" — take the date part.
                    dob = LocalDate.parse(dobRaw.substring(0, 10));
                } catch (DateTimeParseException ignored) { /* skip bad dates */ }
            }

            String nameKey = label.toLowerCase();
            PlenaryMember mp = null;
            boolean viaDob = false;
            if (dob != null) {
                mp = byNameDob.get(nameKey + "|" + dob);
                if (mp != null) viaDob = true;
            }
            if (mp == null
                    && wdNameFreq.getOrDefault(nameKey, 0) == 1
                    && ourNameFreq.getOrDefault(nameKey, 0) == 1) {
                PlenaryMember cand = byNameOnly.get(nameKey);
                if (cand != null && !assignedByDob.contains(cand.getId())) mp = cand;
            }
            if (mp == null) continue;

            // Don't clobber a QID already set (manual correction, or a stronger match) with a
            // different one — re-runs and namesakes must not silently rewrite identities.
            if (mp.getWikidataQid() != null && !mp.getWikidataQid().equals(qid)) {
                log.warn("wikidata QID conflict for {}: keeping existing {} (ignoring {})",
                        mp.getFullName(), mp.getWikidataQid(), qid);
                continue;
            }

            mp.setWikidataQid(qid);
            // Only overwrite URL fields with non-null values so a temporarily missing sitelink
            // in Wikidata doesn't wipe a previously-good link. URLs are validated first so a
            // javascript:/http:/off-domain value can never reach the DB (later rendered as href).
            setUrlIfValid(row, "enwiki", mp::setWikipediaUrlEn);
            setUrlIfValid(row, "etwiki", mp::setWikipediaUrlEt);
            setUrlIfValid(row, "ruwiki", mp::setWikipediaUrlRu);
            mp.setUpdatedAt(Instant.now());
            if (viaDob) assignedByDob.add(mp.getId());
            matchedByQid.put(qid, mp);
            matched++;
        }
        log.info("wikidata cross-ref: {}/{} MPs matched from {} Wikidata rows",
                matched, mps.size(), seen);
        return new CrossrefResult(seen, matched, matchedByQid);
    }

    /** Second pass over just the matched QIDs: attach education + prior offices from Wikidata. */
    private void enrichBio(Map<String, PlenaryMember> matchedByQid) {
        if (matchedByQid.isEmpty()) return;
        String values = matchedByQid.keySet().stream()
                .map(q -> "wd:" + q)
                .collect(Collectors.joining(" "));
        String query = String.format(BIO_SPARQL_TEMPLATE, values);
        try {
            JsonNode result = fetchSparql(query); // network, outside any tx
            if (result == null) return;
            int enriched = tx.execute(status ->
                    applyBio(result.path("results").path("bindings"), matchedByQid));
            log.info("wikidata bio: enriched {} MPs with education/positions", enriched);
        } catch (Exception e) {
            // Bio is a nice-to-have; never let it fail the whole cross-reference.
            log.warn("wikidata bio enrichment failed (education/positions skipped): {}", e.getMessage());
        }
    }

    private int applyBio(JsonNode bindings, Map<String, PlenaryMember> matchedByQid) {
        int enriched = 0;
        for (JsonNode row : bindings) {
            String qUri = row.path("person").path("value").asText(null);
            if (qUri == null) continue;
            String qid = qUri.substring(qUri.lastIndexOf('/') + 1);
            PlenaryMember mp = matchedByQid.get(qid);
            if (mp == null) continue;
            String edu = clamp("education", qid, row.path("education").path("value").asText(null), BIO_MAX);
            String pos = clamp("positions", qid, row.path("positions").path("value").asText(null), BIO_MAX);
            if (edu != null && !edu.isBlank()) mp.setEducation(edu);
            if (pos != null && !pos.isBlank()) mp.setPositions(pos);
            memberRepo.save(mp); // re-attach: mp was detached when the cross-ref tx committed
            enriched++;
        }
        return enriched;
    }

    /** Third pass over matched QIDs: party membership periods (P102) into mp_party_membership. */
    private void enrichParties(Map<String, PlenaryMember> matchedByQid) {
        if (matchedByQid.isEmpty()) return;
        String values = matchedByQid.keySet().stream()
                .map(q -> "wd:" + q)
                .collect(Collectors.joining(" "));
        try {
            JsonNode result = fetchSparql(String.format(PARTY_SPARQL_TEMPLATE, values)); // network, outside any tx
            if (result == null) return;
            List<PartyRow> rows = dedupeForUniqueKey(
                    parsePartyRows(result.path("results").path("bindings")));
            int stored = tx.execute(status -> replaceParties(rows, matchedByQid));
            log.info("wikidata parties: stored {} P102 memberships", stored);
        } catch (Exception e) {
            // Party history is a nice-to-have; never let it fail the whole cross-reference.
            log.warn("wikidata party enrichment failed (P102 skipped): {}", e.getMessage());
        }
    }

    private int replaceParties(List<PartyRow> rows, Map<String, PlenaryMember> matchedByQid) {
        // Full refresh of the Wikidata source; äriregister rows (phase 2) are left untouched.
        partyMembershipRepo.deleteBySource("wikidata");
        Instant now = Instant.now();
        int stored = 0;
        Set<String> insertedKeys = new HashSet<>();
        for (PartyRow r : rows) {
            PlenaryMember mp = matchedByQid.get(r.personQid());
            if (mp == null) continue;
            // Guard the ACTUAL DB unique key (member_external_id, party_qid, start_date): two
            // Wikidata person entities can resolve to the same MP, so deduping on personQid
            // upstream isn't enough. A violation would roll back this whole refresh. Since V29
            // the constraint is NULLS NOT DISTINCT, so null start dates collide too and every
            // row is guarded.
            if (!insertedKeys.add(
                    mp.getExternalId() + "|" + r.partyQid() + "|" + r.startDate())) {
                continue;
            }
            // Soft allow-list: an unknown party QID is NOT dropped (that would silently lose a
            // legitimate small/new party), but it is flagged so a curator can review whether the
            // P102 statement is a genuine affiliation or a vandalised one before it is trusted.
            if (!allowedPartyQids.isEmpty() && !allowedPartyQids.contains(r.partyQid())) {
                log.warn("wikidata: P102 party {} ({}) for member {} is not in the known Estonian "
                                + "party allow-list — stored, flag for review",
                        r.partyQid(), r.label(), mp.getExternalId());
            }
            partyMembershipRepo.save(MpPartyMembership.builder()
                    .memberExternalId(mp.getExternalId())
                    .partyQid(r.partyQid())
                    .partyLabel(r.label())
                    .startDate(r.startDate())
                    .endDate(r.endDate())
                    .source("wikidata")
                    .importedAt(now)
                    .build());
            stored++;
        }
        return stored;
    }

    /** One P102 statement parsed from Wikidata, before the person is resolved to an MP. */
    record PartyRow(String personQid, String partyQid, String label,
                    LocalDate startDate, LocalDate endDate) {
    }

    /**
     * Collapse P102 statements that map to the same unique key. Wikidata sometimes carries two
     * claims for one person+party with the same start date (a re-stated or duplicated membership),
     * which would violate {@code ux_mp_party_membership (member_external_id, party_qid, start_date,
     * source)} and roll back the whole P102 refresh. Keep the first occurrence, but prefer one
     * that carries an end date (more complete). Since V29 the constraint is
     * {@code NULLS NOT DISTINCT}, so null-start rows collapse the same way.
     */
    static List<PartyRow> dedupeForUniqueKey(List<PartyRow> rows) {
        Map<String, PartyRow> byKey = new LinkedHashMap<>();
        for (PartyRow r : rows) {
            String key = r.personQid() + "|" + r.partyQid() + "|" + r.startDate();
            PartyRow existing = byKey.get(key);
            if (existing == null || (existing.endDate() == null && r.endDate() != null)) {
                byKey.put(key, r);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    static List<PartyRow> parsePartyRows(JsonNode bindings) {
        List<PartyRow> rows = new ArrayList<>();
        for (JsonNode row : bindings) {
            String personUri = row.path("person").path("value").asText(null);
            String partyUri = row.path("party").path("value").asText(null);
            if (personUri == null || partyUri == null) continue;
            String personQid = personUri.substring(personUri.lastIndexOf('/') + 1);
            String partyQid = partyUri.substring(partyUri.lastIndexOf('/') + 1);
            // Reject malformed QIDs: party_qid is a VARCHAR(32) key and is trusted downstream, so a
            // garbage value (too long / non-Q) must never be written — drop the whole statement.
            if (!isValidQid(personQid) || !isValidQid(partyQid)) {
                log.warn("wikidata: skipping P102 statement with malformed QID (person '{}', party '{}')",
                        personQid, partyQid);
                continue;
            }
            String label = clamp("partyLabel", partyQid, firstNonBlank(
                    row.path("partyLabelEt").path("value").asText(null),
                    row.path("partyLabelEn").path("value").asText(null),
                    partyQid), PARTY_LABEL_MAX);
            rows.add(new PartyRow(
                    personQid,
                    partyQid, label,
                    parseWdDate(row.path("start").path("value").asText(null)),
                    parseWdDate(row.path("end").path("value").asText(null))));
        }
        return rows;
    }

    private static LocalDate parseWdDate(String raw) {
        if (raw == null || raw.length() < 10) return null;
        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /** Accept a URL only if it is https and points at a *.wikipedia.org host. */
    private static void setUrlIfValid(JsonNode row, String key, Consumer<String> setter) {
        String v = row.path(key).path("value").asText(null);
        if (v == null || v.isBlank()) return;
        if (!isWikipediaUrl(v)) {
            log.warn("wikidata: ignoring non-wikipedia {} URL '{}'", key, v);
            return;
        }
        setter.accept(v.trim());
    }

    /**
     * Wikidata entity id shape: {@code Q} followed by 1..15 digits. The upper bound keeps the
     * value well inside the {@code party_qid VARCHAR(32)} column even for a crafted statement.
     */
    static boolean isValidQid(String qid) {
        return qid != null && QID_PATTERN.matcher(qid).matches();
    }

    /**
     * True only for {@code https://...wikipedia.org/...} URLs. Blocks {@code javascript:}, plain
     * {@code http}, and arbitrary domains — anything that would later be rendered as an href.
     */
    static boolean isWikipediaUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI u = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(u.getScheme())) return false;
            String host = u.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("wikipedia.org") || host.endsWith(".wikipedia.org");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Clamp an over-long free-text value from Wikidata to {@code max} chars, logging when it does. */
    static String clamp(String field, String qid, String value, int max) {
        if (value == null || value.length() <= max) return value;
        log.warn("wikidata: truncating oversized {} for {} ({} -> {} chars)",
                field, qid, value.length(), max);
        return value.substring(0, max);
    }
}
