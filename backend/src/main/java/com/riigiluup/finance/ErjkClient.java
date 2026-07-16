package com.riigiluup.finance;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Fetches party income data from the ERJK (Political Parties Financing Surveillance
 * Committee) open-data API — https://erjk.ee/api, CC BY-SA 3.0. The receipts query
 * returns per-party, per-income-type, per-year totals for all parties in one call.
 */
@Component
public class ErjkClient {

    private static final String RECEIPTS_URL =
            "https://erjk.ee/api/quarterly-reports/queries/receipts?party_id=all&category_id=all&period=show_all";

    private final RestClient rest;

    public ErjkClient() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(30_000);
        this.rest = RestClient.builder().requestFactory(f).build();
    }

    public List<ErjkReceiptDto> fetchAllReceipts() {
        ErjkReceiptDto[] arr = rest.get().uri(RECEIPTS_URL).retrieve().body(ErjkReceiptDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }
}
