package com.riigiluup.election;

import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private final ElectionResultsClient client;
    private final PlenaryMemberRepository memberRepo;
    private final ElectionResultRepository repo;

    public ElectionResultsImporter(ElectionResultsClient client,
                                   PlenaryMemberRepository memberRepo,
                                   ElectionResultRepository repo) {
        this.client = client;
        this.memberRepo = memberRepo;
        this.repo = repo;
    }

    @Transactional
    public int importRk2023() {
        List<ElectionCandidateDto> candidates = client.fetchRk2023Results();

        // Active members win name collisions, so a returning name attaches to the sitting MP
        // rather than an inactive same-name record from an earlier term.
        List<PlenaryMember> members = new ArrayList<>(memberRepo.findAll());
        members.sort(Comparator.comparing(PlenaryMember::isActive)); // active (true) sorts last → overwrites
        Map<String, PlenaryMember> byName = new HashMap<>();
        for (PlenaryMember m : members) {
            byName.put(nameKey(m.getFirstName(), m.getLastName()), m);
        }

        repo.deleteByElectionCode(ELECTION_CODE); // full refresh — immutable source, keeps this idempotent
        Instant now = Instant.now();
        int matched = 0, electedTotal = 0, unmatched = 0;
        for (ElectionCandidateDto c : candidates) {
            if (!c.elected()) continue;
            electedTotal++;
            PlenaryMember m = byName.get(nameKey(c.forename(), c.surname()));
            if (m == null) { unmatched++; continue; }
            repo.save(ElectionResult.builder()
                    .memberExternalId(m.getExternalId())
                    .electionCode(ELECTION_CODE)
                    .personalVotes(c.votes())
                    .mandateType(c.mandateType())
                    .districtNumber(c.districtNumber())
                    .partyName(c.partyName())
                    .ballotNumber(c.registrationNumber())
                    .importedAt(now)
                    .build());
            matched++;
        }
        log.info("Election import {}: {} elected candidates, {} matched to seated MPs, {} unmatched (elected but not currently in the member table)",
                ELECTION_CODE, electedTotal, matched, unmatched);
        return matched;
    }

    private static String nameKey(String forename, String surname) {
        String f = forename == null ? "" : forename.trim().toLowerCase(Locale.ROOT);
        String s = surname == null ? "" : surname.trim().toLowerCase(Locale.ROOT);
        return f + "|" + s;
    }
}
