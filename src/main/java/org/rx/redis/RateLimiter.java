package org.rx.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.rx.config.BeanRegister;
import org.rx.config.MiddlewareConfig;
import org.rx.core.*;
import org.rx.util.Servlets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = BeanRegister.REDIS_PROP_NAME)
@Slf4j
public class RateLimiter {
    interface RateLimiterAdapter {
        boolean tryAcquire();
    }

    private final MiddlewareConfig redisConfig;
    private final RedisCache<?, ?> redisCache;
    private final Map<String, RateLimiterAdapter> rateLimiters = new ConcurrentHashMap<>();

    private int permitsPerSecond() {
        return Math.max(redisConfig.getLimiterPermits(), Constants.CPU_THREADS);
    }

    public boolean tryAcquire() {
        return tryAcquire(Servlets.requestIp(false));
    }

    public boolean tryAcquire(String clientIp) {
        if (!Arrays.isEmpty(redisConfig.getLimiterWhiteList())
                && NQuery.of(redisConfig.getLimiterWhiteList()).any(p -> Strings.startsWith(clientIp, p))) {
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
                return createLimiter(x, permitsPerSecond(), 1)::tryAcquire;
            } catch (Exception e) {
                log.error("getLimiter", e);
                return com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond())::tryAcquire;
            }
        });
    }

    public RRateLimiter createLimiter(String key, long rate, long rateInterval) {
        RRateLimiter limiter = redisCache.getClient().getRateLimiter(key);
        if (limiter.isExists()) {
            RateLimiterConfig config = limiter.getConfig();
            if (config.getRate() == rate && config.getRateInterval() == RateIntervalUnit.SECONDS.toMillis(rateInterval)) {
                return limiter;
            }
        }

        log.info("trySetRate start, {} {}", key, rate);
        int retry = 4;
        // 循环直到重新配置成功
        while (--retry > 0 && !limiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS)) {
            limiter.delete();
            limiter = redisCache.getClient().getRateLimiter(key);
        }
        if (retry == 0) {
            log.warn("trySetRate fail, {} {}", key, rate);
        }
        return limiter;
    }
}
