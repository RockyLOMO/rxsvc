package org.rx.config;

import lombok.extern.slf4j.Slf4j;
import org.rx.core.Cache;
import org.rx.core.Container;
import org.rx.core.Strings;
import org.rx.core.exception.InvalidException;
import org.rx.redis.HybridCache;
import org.rx.redis.RedisCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.rx.core.Cache.LRU_CACHE;

@Configuration
@Slf4j
public class BeanRegister {
    @Bean
    @ConditionalOnProperty(name = "app.redisUrl")
    public <TK, TV> RedisCache<TK, TV> redisCache(RedisConfig redisConfig) {
        if (Strings.isNullOrEmpty(redisConfig.getRedisUrl())) {
            throw new InvalidException("app.redisUrl is null");
        }

        HybridCache<TK, TV> cache = new HybridCache<>(redisConfig.getRedisUrl(), Cache.getInstance(LRU_CACHE), redisConfig.getStoreUrl());
        Container.getInstance().register(LRU_CACHE, cache);
        log.info("register hybrid cache ok");
        return cache;
    }
}
