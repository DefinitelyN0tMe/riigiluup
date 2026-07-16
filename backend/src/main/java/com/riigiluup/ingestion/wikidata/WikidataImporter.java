package com.riigiluup.ingestion.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One-shot importer that cross-references our PlenaryMember rows with Wikidata
 * (CC0 license — free to use for any purpose).
 *
 * <p>Fetches every person who has ever held the position "member of the Riigikogu"
 * (Wikidata Q21100241) via SPARQL, plus per-language Wikipedia article URLs. Matches to
 * existing MPs by (fullName, dateOfBirth); a name-only fallback is used only when the name is
 * unambiguous on both sides. Matches update the four Wikidata columns on plenary_member.
 *
 * <p>Idempotent: safe to re-run. Not scheduled — triggered by
 * {@code POST /api/v1/admin/import/wikidata}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikidataImporter {

    private static final String JOB_NAME = "wikidata.mp-crossref";
    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
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

    private final PlenaryMemberRepository memberRepo;
    private final ImportRunLogRepository runLogRepo;
    private final RestClient rest = RestClient.builder()
            // Wikidata's UA policy: identify the client + contact so they can reach out.
            .defaultHeader(HttpHeaders.USER_AGENT, "riigiluup/0.1 (riigiluup@gmail.com)")
            .defaultHeader(HttpHeaders.ACCEPT, "application/sparql-results+json")
            .build();

    @Transactional
    public ImportRunLog runOnce() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName("wikidata").jobName(JOB_NAME)
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, matched = 0;
        try {
            JsonNode result = rest.get()
                    .uri(SPARQL_ENDPOINT + "?query={q}&format=json", SPARQL)
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null) throw new IllegalStateException("empty Wikidata response");
            JsonNode bindings = result.path("results").path("bindings");

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
                // in Wikidata doesn't wipe a previously-good link.
                setIfPresent(row, "enwiki", mp::setWikipediaUrlEn);
                setIfPresent(row, "etwiki", mp::setWikipediaUrlEt);
                setIfPresent(row, "ruwiki", mp::setWikipediaUrlRu);
                mp.setUpdatedAt(Instant.now());
                if (viaDob) assignedByDob.add(mp.getId());
                matchedByQid.put(qid, mp);
                matched++;
            }
            log.info("wikidata cross-ref: {}/{} MPs matched from {} Wikidata rows",
                    matched, mps.size(), seen);

            enrichBio(matchedByQid);
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

    /** Second pass over just the matched QIDs: attach education + prior offices from Wikidata. */
    private void enrichBio(Map<String, PlenaryMember> matchedByQid) {
        if (matchedByQid.isEmpty()) return;
        String values = matchedByQid.keySet().stream()
                .map(q -> "wd:" + q)
                .collect(java.util.stream.Collectors.joining(" "));
        String query = String.format(BIO_SPARQL_TEMPLATE, values);
        try {
            JsonNode result = rest.get()
                    .uri(SPARQL_ENDPOINT + "?query={q}&format=json", query)
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null) return;
            int enriched = 0;
            for (JsonNode row : result.path("results").path("bindings")) {
                String qUri = row.path("person").path("value").asText(null);
                if (qUri == null) continue;
                String qid = qUri.substring(qUri.lastIndexOf('/') + 1);
                PlenaryMember mp = matchedByQid.get(qid);
                if (mp == null) continue;
                String edu = row.path("education").path("value").asText(null);
                String pos = row.path("positions").path("value").asText(null);
                if (edu != null && !edu.isBlank()) mp.setEducation(edu);
                if (pos != null && !pos.isBlank()) mp.setPositions(pos);
                enriched++;
            }
            log.info("wikidata bio: enriched {} MPs with education/positions", enriched);
        } catch (Exception e) {
            // Bio is a nice-to-have; never let it fail the whole cross-reference.
            log.warn("wikidata bio enrichment failed (education/positions skipped): {}", e.getMessage());
        }
    }

    private static void setIfPresent(JsonNode row, String key, Consumer<String> setter) {
        String v = row.path(key).path("value").asText(null);
        if (v != null && !v.isBlank()) setter.accept(v);
    }
}
