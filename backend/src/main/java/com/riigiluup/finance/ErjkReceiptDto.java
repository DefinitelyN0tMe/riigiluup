package com.riigiluup.finance;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One aggregated income row from the ERJK receipts query. */
public record ErjkReceiptDto(
        @JsonProperty("amount") String amount,
        @JsonProperty("period") String period,
        @JsonProperty("party_id") String partyId,
        @JsonProperty("party_name") String partyName,
        @JsonProperty("category_id") String categoryId,
        @JsonProperty("category_name") String categoryName) {
}
