package org.rx.redis;

import lombok.NonNull;
import org.rx.core.Cache;
import org.rx.core.CachePolicy;
import org.rx.util.function.BiFunc;

import java.util.Set;

import static org.rx.core.Extends.quietly;

public class RedisFallbackCache<TK, TV> extends RedisCache<TK, TV> {
    final Cache<TK, TV> fallback;

    @Override
    public int size() {
        return quietly(super::size, fallback::size);
    }

    public RedisFallbackCache(String redisUrl) {
        this(redisUrl, null);
    }

    public RedisFallbackCache(String redisUrl, Cache<TK, TV> fallback) {
        super(redisUrl);
        if (fallback == null) {
            fallback = Cache.getInstance(Cache.MEMORY_CACHE);
        }
        this.fallback = fallback;
    }

    @Override
    public Set<Entry<TK, TV>> entrySet() {
        return quietly(super::entrySet, fallback::entrySet);
    }

    @Override
    public TV put(TK k, @NonNull TV v, CachePolicy policy) {
        return quietly(() -> super.put(k, v, policy), () -> fallback.put(k, v, policy));
    }

    @Override
    public TV remove(Object k) {
        return quietly(() -> super.remove(k), () -> fallback.remove(k));
    }

    @Override
    public void clear() {
        fallback.clear();
        quietly(super::clear);
    }

    @Override
    public TV get(Object k) {
        return quietly(() -> super.get(k), () -> fallback.get(k));
    }

    @Override
    public TV get(TK k, @NonNull BiFunc<TK, TV> loadingFunc, CachePolicy policy) {
        return quietly(() -> super.get(k, loadingFunc, policy), () -> fallback.get(k, loadingFunc, policy));
    }
}
