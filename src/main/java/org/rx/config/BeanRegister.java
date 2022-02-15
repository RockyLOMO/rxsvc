package org.rx.config;

import lombok.extern.slf4j.Slf4j;
import org.rx.core.Cache;
import org.rx.core.Container;
import org.rx.core.Strings;
import org.rx.exception.InvalidException;
import org.rx.redis.RedisCache;
//import org.rx.redis.RedisLocalCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.rx.core.Cache.*;

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

//        RedisLocalCache<TK, TV> cache = new RedisLocalCache<>(redisConfig.getRedisUrl(), Cache.getInstance(MEMORY_CACHE));
        RedisCache<TK, TV> cache = new RedisCache<>(redisConfig.getRedisUrl());
        Container.register(Cache.class, cache);
        log.info("register RedisCache ok");
        return cache;
    }
}
