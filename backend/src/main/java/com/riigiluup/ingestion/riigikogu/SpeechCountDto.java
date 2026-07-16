package com.riigiluup.ingestion.riigikogu;

/** One row from GET /api/steno/speeches — per-member plenary speech/question tallies. */
public record SpeechCountDto(
        String uuid,
        int speeches,
        int questions) {
}
