package com.riigiluup.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.common.CacheConfig;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsServiceCacheTest {

    private static final LocalDate FROM = LocalDate.of(2023, 4, 10);
    private static final LocalDate TO = LocalDate.of(2026, 7, 13);

    @Test
    void participation_result_is_cached_per_uuid_and_date_range() throws Exception {
        RiigikoguClient client = mock(RiigikoguClient.class);
        ObjectMapper json = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        when(client.fetchParticipationStats(eq("m-1"), any(), any())).thenAnswer(inv -> {
            calls.incrementAndGet();
            return json.readTree("{\"sittings\":400,\"participated\":383,\"absent\":17}");
        });

        ParticipationCacheStore cacheStore = mock(ParticipationCacheStore.class);
        StatisticsService raw = new StatisticsService(client, cacheStore);
        StatisticsService svc = wrapWithCache(raw);

        ParticipationStats a = svc.participation("m-1", FROM, TO);
        ParticipationStats b = svc.participation("m-1", FROM, TO);
        ParticipationStats c = svc.participation("m-1", FROM, TO);

        assertThat(a.totalSittings()).isEqualTo(400);
        assertThat(a.attended()).isEqualTo(383);
        assertThat(a.participationRate()).isEqualTo(383.0 / 400.0);
        assertThat(b).isSameAs(a);
        assertThat(c).isSameAs(a);
        assertThat(calls.get()).isEqualTo(1);
    }

    private StatisticsService wrapWithCache(StatisticsService target) {
        CacheManager cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.CACHE_PARTICIPATION, CacheConfig.CACHE_VOTING);

        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvisor(new StaticMethodMatcherPointcutAdvisor(
                new CachingMethodInterceptor(cacheManager)) {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return method.isAnnotationPresent(Cacheable.class);
            }
        });
        factory.setProxyTargetClass(true);

        return (StatisticsService) factory.getProxy();
    }

    private static class CachingMethodInterceptor implements MethodInterceptor {
        private final CacheManager cacheManager;

        CachingMethodInterceptor(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Cacheable cacheable = invocation.getMethod().getAnnotation(Cacheable.class);
            if (cacheable == null) {
                return invocation.proceed();
            }

            String cacheName = cacheable.value()[0];
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return invocation.proceed();
            }

            String key = Arrays.stream(invocation.getArguments())
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ":" + b)
                    .orElse("");

            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) {
                return cached.get();
            }

            Object result = invocation.proceed();
            cache.put(key, result);
            return result;
        }
    }
}
