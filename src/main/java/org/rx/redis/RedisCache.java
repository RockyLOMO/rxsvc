package org.rx.redis;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;
import org.rx.bean.BiTuple;
import org.rx.bean.RxConfig;
import org.rx.bean.Tuple;
import org.rx.core.*;
import org.rx.util.function.BiFunc;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisCache<TK, TV> implements Cache<TK, TV> {
    public static RedissonClient create(String redisUrl) {
        return create(redisUrl, false);
    }

    public static RedissonClient create(@NonNull String redisUrl, boolean jdkCodec) {
        BiTuple<String, Integer, String> resolve = resolve(redisUrl);
        log.info("RedissonClient {} -> {}", redisUrl, resolve);
        Config config = new Config();
        config.setExecutor(Tasks.pool());
        if (jdkCodec) {
            config.setCodec(new SerializationCodec());
        }
        int minPoolSize = 2;
        int maxPoolSize = Math.max(minPoolSize, Container.get(RxConfig.class).getNetMaxPoolSize());
        config.useSingleServer().setKeepAlive(true).setTcpNoDelay(true)
                .setConnectionMinimumIdleSize(minPoolSize).setConnectionPoolSize(maxPoolSize)
                .setSubscriptionConnectionMinimumIdleSize(minPoolSize).setSubscriptionConnectionPoolSize(maxPoolSize)
                .setAddress(String.format("redis://%s", resolve.left)).setDatabase(resolve.middle).setPassword(resolve.right);
        return Redisson.create(config);
    }

    private static BiTuple<String, Integer, String> resolve(String redisUrl) {
        String pwd = null;
        int database = 0, i;
        if ((i = redisUrl.lastIndexOf("/")) != -1) {
            database = Integer.parseInt(redisUrl.substring(i + 1));
            redisUrl = redisUrl.substring(0, i);
        }
        if ((i = redisUrl.lastIndexOf("@")) != -1) {
            pwd = redisUrl.substring(0, i);
            redisUrl = redisUrl.substring(i + 1);
        }
        return BiTuple.of(redisUrl, database, pwd);
    }

    protected static RedisClient createRedisClient(String redisUrl) {
        BiTuple<String, Integer, String> resolve = resolve(redisUrl);
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress(resolve.left).setDatabase(resolve.middle).setPassword(resolve.right);
        return RedisClient.create(config);
    }

    @Getter
    private final RedissonClient client;
    private final ConcurrentHashMap<String, TK> keyMap = new ConcurrentHashMap<>();
    protected final Tuple<BiFunc<TV, Serializable>, BiFunc<Serializable, TV>> onNotSerializable;

    @Override
    public int size() {
        return (int) client.getKeys().count();
    }

    public RedisCache(String redisUrl) {
        this(redisUrl, null);
    }

    public RedisCache(String redisUrl, Tuple<BiFunc<TV, Serializable>, BiFunc<Serializable, TV>> onNotSerializable) {
        if (onNotSerializable != null) {
            Objects.requireNonNull(onNotSerializable.left);
            Objects.requireNonNull(onNotSerializable.right);
        }

        client = create(redisUrl);
        this.onNotSerializable = onNotSerializable;
    }

    @Override
    public Set<TK> keySet() {
        Set<TK> keys = new HashSet<>();
        for (String key : client.getKeys().getKeys()) {
            keys.add(transferKey(key));
        }
        return keys;
    }

    @Override
    public Collection<TV> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<TK, TV>> entrySet() {
        throw new UnsupportedOperationException();
    }

    protected String transferKey(@NonNull TK k) {
        if (k instanceof String) {
            return k.toString();
        }
        String ck = k.getClass().getSimpleName() + k.hashCode();
        keyMap.put(ck, k);
        return ck;
    }

    protected TK transferKey(@NonNull String k) {
        return keyMap.getOrDefault(k, (TK) k);
    }

    @Override
    public TV put(TK k, TV v) {
        return put(k, v, CacheExpiration.NON_EXPIRE);
    }

    @SneakyThrows
    @Override
    public TV put(TK k, @NonNull TV v, CacheExpiration expiration) {
        int expireSeconds = expiration.getSlidingExpiration();
        if (!(v instanceof Serializable) && onNotSerializable != null) {
            Serializable item = onNotSerializable.left.invoke(v);
            RBucket<Serializable> bucket = client.getBucket(transferKey(k));
            return onNotSerializable.right.invoke(expireSeconds < 1 ? bucket.getAndSet(item) : bucket.getAndSet(item, expireSeconds, TimeUnit.SECONDS));
        }

        RBucket<TV> bucket = client.getBucket(transferKey(k));
        return expireSeconds < 1 ? bucket.getAndSet(v) : bucket.getAndSet(v, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public TV remove(Object k) {
        return check(client.getBucket(transferKey((TK) k)).getAndDelete());
    }

    @SneakyThrows
    private TV check(Object item) {
        if (onNotSerializable != null) {
            return onNotSerializable.right.invoke((Serializable) item);
        }
        return (TV) item;
    }

    @Override
    public void clear() {
        client.getKeys().flushdb();
    }

    @Override
    public TV get(Object k) {
        return check(client.getBucket(transferKey((TK) k)).get());
    }

    @Override
    public TV get(TK k, BiFunc<TK, TV> biFunc) {
        return get(k, biFunc, CacheExpiration.NON_EXPIRE);
    }

    @SneakyThrows
    @Override
    public TV get(TK k, @NonNull BiFunc<TK, TV> biFunc, CacheExpiration expiration) {
        int expireSeconds = expiration.getSlidingExpiration();
        RBucket bucket = client.getBucket(transferKey(k));
        TV v = check(bucket.get());
        if (v != null) {
            if (expiration.getSlidingExpiration() > 0) {
                bucket.expireAsync(expireSeconds, TimeUnit.SECONDS);
            }
            return v;
        }
        put(k, v = biFunc.invoke(k), expiration);
        return v;
    }
}
