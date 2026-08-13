package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.legislation.SponsorKind;
import org.springframework.stereotype.Component;

@Component
public class SponsorClassifier {

    public SponsorKind classify(DraftDetailDto.Initiator initiator) {
        if (initiator == null) return SponsorKind.OTHER;
        String type = initiator.type() == null ? "" : initiator.type().toLowerCase();
        String name = initiator.name() == null ? "" : initiator.name().toLowerCase();
        return switch (type) {
            // The draft API tags an individual MP initiator as "user" (not "plenarymember"); both
            // are people who should link to their profile. Missing "user" silently dropped every
            // MP bill-initiation into OTHER, emptying the profile's "bills initiated" and topic radar.
            case "user", "plenarymember" -> SponsorKind.PLENARY_MEMBER;
            case "classifier" -> SponsorKind.ORGAN;
            case "usergroup" -> nameToUsergroupKind(name);
            default -> SponsorKind.OTHER;
        };
    }

    private static SponsorKind nameToUsergroupKind(String lowerName) {
        if (lowerName.contains("fraktsioon")) return SponsorKind.FACTION;
        if (lowerName.contains("komisjon")) return SponsorKind.COMMITTEE;
        return SponsorKind.OTHER;
    }
}
