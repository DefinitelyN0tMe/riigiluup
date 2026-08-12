package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Shape of {@code /api/plenary-members/{uuid}}. Committees, factions and
 * electoral district live INSIDE each {@link Membership} (one per parliamentary
 * term the MP has served). Callers pick the current term via
 * {@code membershipRoleItems[i].endDate == null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlenaryMemberDetailDto(
        String uuid,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String gender,
        String dateOfBirth,
        String biography,
        Integer parliamentSeniority,
        Photo photo,
        List<Membership> memberships,
        List<PressEntry> press
) {
    /**
     * A press-activity entry: {@code description} is the free-text line (title + publication(s) +
     * date), {@code url} points to the external article (may be null), {@code date} is ISO yyyy-MM-dd.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PressEntry(Integer membershipNumber, String description, String url, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Photo(String uuid, String fileName, String fileExtension, Links _links) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Links(Href download, Href self) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Href(String href) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Membership(
            Integer membershipNumber,
            List<GroupRef> factions,
            List<GroupRef> committees,
            List<DistrictEntry> electoralDistrict,
            List<MembershipRoleItem> membershipRoleItems
    ) {}

    /** Committee / faction reference nested under a Membership. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupRef(
            String uuid,
            String name,
            CodeValue type,
            Boolean active,
            MembershipSpan membership
    ) {}

    /** Validity window of a specific group affiliation within a term. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MembershipSpan(
            String uuid,
            Integer membershipNumber,
            String startDate,
            String endDate,
            CodeValue role,
            CodeValue jobTitle
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DistrictEntry(Integer membership, CodeValue electoralDistrict) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MembershipRoleItem(
            String startDate,
            String endDate,
            CodeValue role
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodeValue(String code, String value) {}
}
