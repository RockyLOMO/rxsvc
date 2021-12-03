//package org.rx.redis;
//
//import com.google.common.hash.BloomFilter;
//import com.google.common.hash.Funnels;
//import lombok.NonNull;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.rx.core.Cache;
//import org.rx.core.CacheExpiration;
//import org.rx.util.function.BiFunc;
//
//import java.io.Serializable;
//import java.util.Set;
//
//import static org.rx.core.App.*;
//
//@Slf4j
//public class RedisLocalCache<TK, TV> extends RedisCache<TK, TV> {
//    private final Cache<TK, TV> local;
//    private final BloomFilter<String> notSerializable = BloomFilter.create(Funnels.unencodedCharsFunnel(), 10000);
//
//    @Override
//    public int size() {
//        return isNull(quietly(super::size), 0) + local.size();
//    }
//
//    public RedisLocalCache(String redisUrl, Cache<TK, TV> local) {
//        super(redisUrl);
//        this.local = local;
//    }
//
//    @Override
//    public Set<TK> keySet() {
//        Set<TK> keys = local.keySet();
//        quietly(() -> keys.addAll(super.keySet()));
//        return keys;
//    }
//
//    @Override
//    public TV put(TK k, @NonNull TV v, CacheExpiration expiration) {
//        Class type = v.getClass();
//        try {
//            if (v instanceof Serializable && !notSerializable.mightContain(type.getName())) {
//                return super.put(k, v, expiration);
//            } else {
//                if (!notSerializable.mightContain(type.getName())) {
//                    log.warn("put fail, {} not serializable", type.getName());
//                    notSerializable.put(type.getName());
//                }
//            }
//        } catch (Exception e) {
//            if (StringUtils.contains(e.getMessage(), "NotSerializableException")) {
//                notSerializable.put(type.getName());
//            }
//            log.warn("put fail", e);
//        }
//        return local.put(k, v);
//    }
//
//    @Override
//    public TV remove(Object k) {
//        TV v = local.remove(k);
//        if (v != null) {
//            return v;
//        }
//        return quietly(() -> super.remove(k));
//    }
//
//    @Override
//    public synchronized void clear() {
//        local.clear();
//        quietly(super::clear);
//    }
//
//    @Override
//    public TV get(Object k) {
//        TV v = local.get(k);
//        if (v != null) {
//            return v;
//        }
//        return super.get(k);
//    }
//
//    @SneakyThrows
//    @Override
//    public TV get(TK k, BiFunc<TK, TV> biFunc, CacheExpiration expiration) {
//        TV v = local.get(k);
//        if (v != null) {
//            return v;
//        }
//        return super.get(k, biFunc, expiration);
//    }
//}
