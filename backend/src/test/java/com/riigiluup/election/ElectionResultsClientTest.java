package com.riigiluup.election;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ElectionResultsClientTest {

    // Mirrors the real opendata.valimised.ee RESULTS.xml: default namespace v2, party with
    // <name>/<code>, candidates with <elected>, and <mandateType> present ONLY when elected.
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <OutputReport xmlns="https://opendata.valimised.ee/schemas/election-result/rk/v2/">
              <electionCode>RK_2023</electionCode>
              <data>
                <electionResult>
                  <votesAndMandates>
                    <party>
                      <name>Test Party</name>
                      <code>TP</code>
                      <candidates>
                        <candidate>
                          <forename>ILMAR</forename>
                          <surname>RAAG</surname>
                          <registrationNumber>101</registrationNumber>
                          <votes>1131</votes>
                          <elected>false</elected>
                          <districtNumber>1</districtNumber>
                        </candidate>
                        <candidate>
                          <forename>RIINA</forename>
                          <surname>SOLMAN</surname>
                          <registrationNumber>296</registrationNumber>
                          <votes>1773</votes>
                          <elected>true</elected>
                          <districtNumber>2</districtNumber>
                          <mandateType>COMPENSATION</mandateType>
                        </candidate>
                      </candidates>
                    </party>
                  </votesAndMandates>
                </electionResult>
              </data>
            </OutputReport>
            """;

    @Test
    void parses_every_candidate_with_party_and_district() {
        List<ElectionCandidateDto> all = ElectionResultsClient.parse(XML.getBytes(StandardCharsets.UTF_8));

        assertThat(all).hasSize(2);
        assertThat(all).allSatisfy(c -> {
            assertThat(c.partyName()).isEqualTo("Test Party");
            assertThat(c.partyCode()).isEqualTo("TP");
        });
    }

    @Test
    void marks_elected_flag_and_reads_mandate_type_only_for_elected() {
        List<ElectionCandidateDto> elected = ElectionResultsClient
                .parse(XML.getBytes(StandardCharsets.UTF_8)).stream()
                .filter(ElectionCandidateDto::elected)
                .toList();

        assertThat(elected).hasSize(1);
        ElectionCandidateDto c = elected.get(0);
        assertThat(c.surname()).isEqualTo("SOLMAN");
        assertThat(c.votes()).isEqualTo(1773);
        assertThat(c.mandateType()).isEqualTo("COMPENSATION");
        assertThat(c.districtNumber()).isEqualTo(2);
        assertThat(c.registrationNumber()).isEqualTo(296);
    }

    @Test
    void non_elected_candidate_has_no_mandate_type() {
        ElectionCandidateDto raag = ElectionResultsClient
                .parse(XML.getBytes(StandardCharsets.UTF_8)).stream()
                .filter(c -> c.surname().equals("RAAG"))
                .findFirst().orElseThrow();

        assertThat(raag.elected()).isFalse();
        assertThat(raag.mandateType()).isNull();
        assertThat(raag.votes()).isEqualTo(1131);
    }
}
