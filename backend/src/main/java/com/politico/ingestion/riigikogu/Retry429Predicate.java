package com.politico.ingestion.riigikogu;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.function.Predicate;

/**
 * Resilience4j retry predicate — retry if the wrapped call returned an
 * HTTP 429 (rate limited) response entity. Used by {@code resilience4j.retry
 * .instances.riigikogu.retryOnResultPredicate}.
 *
 * <p>Note: the {@link RiigikoguClient} currently throws on non-2xx via
 * RestClient's default status handler, so 429s surface as
 * {@link org.springframework.web.client.HttpClientErrorException.TooManyRequests}
 * and are already covered by {@code retryExceptions}. This predicate is a
 * safety net if a future refactor switches to
 * {@code exchange()}/{@code toEntity()} and starts returning ResponseEntity.</p>
 */
public class Retry429Predicate implements Predicate<Object> {

    @Override
    public boolean test(Object result) {
        if (result instanceof ResponseEntity<?> re) {
            HttpStatusCode status = re.getStatusCode();
            return status.value() == 429;
        }
        return false;
    }
}
