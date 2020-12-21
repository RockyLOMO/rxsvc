package org.rx.core.cache;

import lombok.Getter;
import lombok.Setter;
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
import org.rx.bean.DateTime;
import org.rx.bean.RxConfig;
import org.rx.bean.Tuple;
import org.rx.core.App;
import org.rx.core.Cache;
import org.rx.core.Tasks;
import org.rx.core.ThreadPool;
import org.rx.util.function.BiFunc;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.rx.core.Contract.require;

@Slf4j
public class RedisCache<TK, TV> implements Cache<TK, TV> {
    public static final int ONE_DAY_EXPIRE = 60 * 24;

    public static int todayEffective() {
        return todayEffective(HybridCache.ONE_DAY_EXPIRE);
    }

    public static int todayEffective(int expireMinutes) {
        DateTime now = DateTime.now(), expire = now.addMinutes(expireMinutes);
        DateTime max = DateTime.valueOf(String.format("%s 23:59:59", now.toDateString()), DateTime.Formats.first());
        if (expire.before(max)) {
            return expireMinutes;
        }
        return (int) max.subtract(now).getTotalMinutes();
    }

    public static RedissonClient create(String redisUrl) {
        return create(redisUrl, false);
    }

    public static RedissonClient create(String redisUrl, boolean jdkCodec) {
        require(redisUrl);

        BiTuple<String, Integer, String> resolve = resolve(redisUrl);
        log.info("RedissonClient {} -> {}", redisUrl, resolve);
        Config config = new Config();
        config.setExecutor(Tasks.getExecutor());
        if (jdkCodec) {
            config.setCodec(new SerializationCodec());
        }
        RxConfig rxConfig = App.getConfig();
        int minPoolSize = Math.max(2, rxConfig.getNetMinPoolSize());
        int maxPoolSize = Math.max(minPoolSize, rxConfig.getNetMaxPoolSize());
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
    @Setter
    private int expireMinutes = NON_EXPIRE_MINUTES;
    @Getter
    @Setter
    private boolean isSlidingExpiration;
    @Getter
    private final RedissonClient client;
    private final ConcurrentHashMap<String, TK> keyMap = new ConcurrentHashMap<>();
    protected final Tuple<BiFunc<TV, Serializable>, BiFunc<Serializable, TV>> onNotSerializable;

    @Override
    public long size() {
        return client.getKeys().count();
    }

    public RedisCache(String redisUrl) {
        this(redisUrl, null);
    }

    public RedisCache(String redisUrl, Tuple<BiFunc<TV, Serializable>, BiFunc<Serializable, TV>> onNotSerializable) {
        if (onNotSerializable != null) {
            require(onNotSerializable.left, onNotSerializable.right);
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

    protected String transferKey(TK k) {
        require(k);
        if (k instanceof String) {
            return k.toString();
        }
        String ck = k.getClass().getSimpleName() + k.hashCode();
        keyMap.put(ck, k);
        return ck;
    }

    protected TK transferKey(String k) {
        require(k);
        return keyMap.getOrDefault(k, (TK) k);
    }

    @Override
    public TV put(TK k, TV v) {
        return put(k, v, expireMinutes);
    }

    @SneakyThrows
    @Override
    public TV put(TK k, TV v, int expireMinutes) {
        require(v);
        if (!(v instanceof Serializable) && onNotSerializable != null) {
            Serializable item = onNotSerializable.left.invoke(v);
            RBucket<Serializable> bucket = client.getBucket(transferKey(k));
            return onNotSerializable.right.invoke(expireMinutes < 1 ? bucket.getAndSet(item) : bucket.getAndSet(item, expireMinutes, TimeUnit.MINUTES));
        }

        RBucket<TV> bucket = client.getBucket(transferKey(k));
        return expireMinutes < 1 ? bucket.getAndSet(v) : bucket.getAndSet(v, expireMinutes, TimeUnit.MINUTES);
    }

    @Override
    public TV remove(TK k) {
        return check(client.getBucket(transferKey(k)).getAndDelete());
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
    public TV get(TK k) {
        return check(client.getBucket(transferKey(k)).get());
    }

    @Override
    public TV get(TK k, BiFunc<TK, TV> biFunc) {
        return get(k, biFunc, expireMinutes);
    }

    @SneakyThrows
    @Override
    public TV get(TK k, BiFunc<TK, TV> biFunc, int expireMinutes) {
        require(biFunc);

        RBucket bucket = client.getBucket(transferKey(k));
        TV v = check(bucket.get());
        if (v != null) {
            if (isSlidingExpiration) {
                bucket.expireAsync(expireMinutes, TimeUnit.MINUTES);
            }
            return v;
        }
        put(k, v = biFunc.invoke(k), expireMinutes);
        return v;
    }
}
