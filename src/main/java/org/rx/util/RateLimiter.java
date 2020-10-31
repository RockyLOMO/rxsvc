package org.rx.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.rx.core.NQuery;
import org.rx.core.Strings;
import org.rx.core.ThreadPool;
import org.rx.core.cache.HybridCache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class RateLimiter {
    interface RateLimiterAdapter {
        boolean tryAcquire();
    }

    @Getter
    private static final RateLimiter instance = new RateLimiter();
    private static final List<String> whiteList = new CopyOnWriteArrayList<>();

    static {
        whiteList.add("127.0.0.");
        whiteList.add("172.");
    }

    private final Map<String, RateLimiterAdapter> rateLimiters = new ConcurrentHashMap<>();

    private int permitsPerSecond() {
        return Math.max(4, ThreadPool.CPU_THREADS);
    }

    public boolean tryAcquire() {
        return tryAcquire(Servlets.requestIp(false));
    }

    public boolean tryAcquire(String clientIp) {
        if (NQuery.of(whiteList).any(p -> Strings.startsWith(clientIp, p))) {
            return true;
        }

        return getLimiter(clientIp).tryAcquire();
    }

    private RateLimiterAdapter getLimiter(String clientIp) {
        if (clientIp == null) {
            clientIp = "ALL";
        }

        String k = "RateLimiter:" + clientIp;
        return rateLimiters.computeIfAbsent(k, x -> {
            try {
                RRateLimiter limiter = HybridCache.getInstance().getClient().getRateLimiter(x);
                if (!limiter.trySetRate(RateType.OVERALL, permitsPerSecond(), 1, RateIntervalUnit.SECONDS)) {
                    log.error("trySetRate fail, key={}", k);
                }
                return limiter::tryAcquire;
            } catch (Exception e) {
                log.error("getLimiter", e);
                com.google.common.util.concurrent.RateLimiter local = com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond());
                return local::tryAcquire;
            }
        });
    }
}
