package com.riigiluup.election;

/** One candidate row parsed from opendata.valimised.ee RESULTS.xml. */
public record ElectionCandidateDto(
        String forename,
        String surname,
        int votes,
        Integer districtNumber,
        String mandateType,       // present only when elected == true
        String partyName,
        String partyCode,
        Integer registrationNumber,
        boolean elected) {
}
