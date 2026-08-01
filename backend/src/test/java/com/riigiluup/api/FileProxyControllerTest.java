package com.riigiluup.api;

import com.riigiluup.common.CacheConfig;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileProxyControllerTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void invalid_uuid_is_rejected_without_touching_the_loader() {
        FileProxyController.Loader loader = mock(FileProxyController.Loader.class);
        FileProxyController controller = new FileProxyController(loader);

        for (String junk : new String[]{"not-a-uuid", "../../etc/passwd", "12345", "", "zzzzzzzz-e89b-12d3-a456-426614174000"}) {
            ResponseEntity<byte[]> res = controller.download(junk);
            assertThat(res.getStatusCode().value()).isEqualTo(400);
        }
        verifyNoInteractions(loader);
    }

    @Test
    void valid_uuid_is_accepted_by_the_validator() {
        assertThat(FileProxyController.isValidUuid(VALID_UUID)).isTrue();
        assertThat(FileProxyController.isValidUuid("not-a-uuid")).isFalse();
        assertThat(FileProxyController.isValidUuid(null)).isFalse();
    }

    @Test
    void upstream_busy_maps_to_503_with_retry_after() {
        FileProxyController.Loader loader = mock(FileProxyController.Loader.class);
        when(loader.load(VALID_UUID)).thenThrow(new FileProxyController.UpstreamBusyException());
        FileProxyController controller = new FileProxyController(loader);

        ResponseEntity<byte[]> res = controller.download(VALID_UUID);
        assertThat(res.getStatusCode().value()).isEqualTo(503);
        assertThat(res.getHeaders().getFirst("Retry-After")).isEqualTo("5");
    }

    @Test
    void loader_throws_busy_when_no_permit_available() {
        RiigikoguClient client = mock(RiigikoguClient.class);
        // Zero permits → tryAcquire always fails → busy, upstream never called.
        FileProxyController.Loader loader = new FileProxyController.Loader(client, 0, 20, "");
        assertThatThrownBy(() -> loader.load(VALID_UUID))
                .isInstanceOf(FileProxyController.UpstreamBusyException.class);
        verifyNoInteractions(client);
    }

    @Test
    void loader_fetches_upstream_when_a_permit_is_available() {
        RiigikoguClient client = mock(RiigikoguClient.class);
        byte[] payload = {1, 2, 3};
        when(client.fetchFileBytes(VALID_UUID)).thenReturn(payload);
        FileProxyController.Loader loader = new FileProxyController.Loader(client, 4, 1000, "");
        assertThat(loader.load(VALID_UUID)).isEqualTo(payload);
    }

    @Test
    void empty_upstream_throws_so_it_is_not_cached() {
        RiigikoguClient client = mock(RiigikoguClient.class);
        FileProxyController.Loader loader = new FileProxyController.Loader(client, 4, 1000, "");
        for (byte[] empty : new byte[][]{null, new byte[0]}) {
            when(client.fetchFileBytes(VALID_UUID)).thenReturn(empty);
            assertThatThrownBy(() -> loader.load(VALID_UUID))
                    .isInstanceOf(FileProxyController.EmptyUpstreamException.class);
        }
    }

    @Test
    void empty_upstream_maps_to_404() {
        FileProxyController.Loader loader = mock(FileProxyController.Loader.class);
        when(loader.load(VALID_UUID)).thenThrow(new FileProxyController.EmptyUpstreamException());
        FileProxyController controller = new FileProxyController(loader);
        assertThat(controller.download(VALID_UUID).getStatusCode().value()).isEqualTo(404);
    }

    /**
     * Regression guard: the @Cacheable config must be valid when invoked through the real
     * Spring cache proxy. A sync=true + unless combination compiles fine but blows up at the
     * first call with IllegalStateException — which unit tests on the bare Loader never hit.
     */
    @Test
    void cacheable_load_works_through_the_spring_cache_proxy_and_caches() {
        RiigikoguClient client = mock(RiigikoguClient.class);
        when(client.fetchFileBytes(VALID_UUID)).thenReturn(new byte[]{1, 2, 3});
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getDefaultListableBeanFactory().registerSingleton("riigikoguClient", client);
            ctx.register(CacheConfig.class, LoaderTestConfig.class);
            ctx.refresh();

            FileProxyController.Loader loader = ctx.getBean(FileProxyController.Loader.class);
            assertThat(loader.load(VALID_UUID)).containsExactly(1, 2, 3);
            assertThat(loader.load(VALID_UUID)).containsExactly(1, 2, 3);
            // Cached: the upstream is hit exactly once across two loads.
            verify(client, times(1)).fetchFileBytes(VALID_UUID);
        }
    }

    @Configuration
    static class LoaderTestConfig {
        @Bean
        FileProxyController.Loader loader(RiigikoguClient client) {
            return new FileProxyController.Loader(client, 4, 1000, "");
        }
    }
}
