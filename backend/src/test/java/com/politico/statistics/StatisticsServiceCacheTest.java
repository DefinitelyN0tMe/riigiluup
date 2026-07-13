package com.politico.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politico.common.CacheConfig;
import com.politico.ingestion.riigikogu.RiigikoguClient;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsServiceCacheTest {

    @Test
    void participation_result_is_cached_per_uuid() throws Exception {
        RiigikoguClient client = mock(RiigikoguClient.class);
        ObjectMapper json = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        when(client.fetchParticipationStats("m-1")).thenAnswer(inv -> {
            calls.incrementAndGet();
            return json.readTree("{\"totalSittings\":100,\"attended\":80}");
        });

        StatisticsService raw = new StatisticsService(client);
        StatisticsService svc = wrapWithCache(raw);

        ParticipationStats a = svc.participation("m-1");
        ParticipationStats b = svc.participation("m-1");
        ParticipationStats c = svc.participation("m-1");

        assertThat(a.totalSittings()).isEqualTo(100);
        assertThat(a.attended()).isEqualTo(80);
        assertThat(a.participationRate()).isEqualTo(0.8);
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

            Object key = invocation.getArguments().length > 0
                    ? invocation.getArguments()[0]
                    : null;

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
