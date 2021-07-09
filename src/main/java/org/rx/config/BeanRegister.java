package org.rx.config;

import lombok.extern.slf4j.Slf4j;
import org.rx.core.Cache;
import org.rx.core.Container;
import org.rx.core.Strings;
import org.rx.core.exception.InvalidException;
import org.rx.redis.RedisCache;
import org.rx.redis.RedisLocalCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.rx.core.Cache.DISTRIBUTED_CACHE;
import static org.rx.core.Cache.LOCAL_CACHE;

@Configuration
@Slf4j
public class BeanRegister {
    public static final String REDIS_PROP_NAME = "app.redisUrl";

    @Bean
    @ConditionalOnProperty(name = REDIS_PROP_NAME)
    public <TK, TV> RedisCache<TK, TV> redisCache(RedisConfig redisConfig) {
        if (Strings.isNullOrEmpty(redisConfig.getRedisUrl())) {
            throw new InvalidException("app.redisUrl is null");
        }

        RedisLocalCache<TK, TV> cache = new RedisLocalCache<>(redisConfig.getRedisUrl(), Cache.getInstance(LOCAL_CACHE));
        Container.getInstance().register(DISTRIBUTED_CACHE, cache);
        log.info("register HybridCache ok");
        return cache;
    }
}
