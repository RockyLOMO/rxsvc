package org.rx.config;

import lombok.extern.slf4j.Slf4j;
import org.rx.core.Cache;
import org.rx.core.IOC;
import org.rx.core.Strings;
import org.rx.exception.InvalidException;
import org.rx.redis.RedisCache;
import org.rx.redis.RedisFallbackCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BeanRegister {
    public static final String REDIS_PROP_NAME = "app.redisUrl";

    @Bean
    @ConditionalOnProperty(name = REDIS_PROP_NAME)
    public <TK, TV> RedisCache<TK, TV> redisCache(MiddlewareConfig redisConfig) {
        if (Strings.isEmpty(redisConfig.getRedisUrl())) {
            throw new InvalidException("app.redisUrl is null");
        }

        RedisCache<TK, TV> cache = redisConfig.isRedisLocalFallback()
                ? new RedisFallbackCache<>(redisConfig.getRedisUrl())
                : new RedisCache<>(redisConfig.getRedisUrl());
        IOC.register(Cache.class, cache);
        log.info("register RedisCache ok");
        return cache;
    }
}
