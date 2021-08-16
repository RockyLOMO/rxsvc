//package org.rx.redis;
//
//import com.google.common.hash.BloomFilter;
//import com.google.common.hash.Funnels;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.Node;
//import org.redisson.api.NodeType;
//import org.rx.core.*;
//import org.rx.core.exception.InvalidException;
//import org.rx.util.function.BiFunc;
//
//import java.util.Map;
//import java.util.Set;
//
//@Slf4j
//public final class HybridCache<TK, TV> extends RedisLocalCache<TK, TV> {
//    private volatile int usedMemoryPercent;
//    @Setter
//    private int useStoreThreshold = 65;
//    private PikaDb<TK, TV> store;
//    private BloomFilter<String> storeFilter;
//
//    public PikaDb<TK, TV> getPikaDb() {
//        if (store == null) {
//            throw new InvalidException("PikaDb not init");
//        }
//        return store;
//    }
//
//    @Override
//    public long size() {
//        return super.size() + storeFilter.approximateElementCount();
//    }
//
//    public HybridCache(String redisUrl, Cache<TK, TV> local, String storeUrl) {
//        super(redisUrl, local);
////        setExpireMinutes(CONFIG.getCacheExpireMinutes());
//        setSlidingExpiration(true);
//
//        if (!Strings.isEmpty(storeUrl)) {
//            Tasks.schedule(this::refreshUsedMemory, 1000 * 16);
//            store = new PikaDb<>(storeUrl);
//            store.setSlidingExpiration(false);
//            storeFilter = BloomFilter.create(Funnels.unencodedCharsFunnel(), 1000000, 0.001);
//        }
//    }
//
//    private void refreshUsedMemory() {
//        Node master = NQuery.of(getClient().getNodesGroup().getNodes(NodeType.MASTER)).first();
//        Map<String, String> data = master.info(Node.InfoSection.MEMORY);
//        long usedMemory = Long.parseLong(data.get("used_memory"));
//        long maxmemory = Long.parseLong(data.get("maxmemory"));
//        usedMemoryPercent = (int) ((double) usedMemory / maxmemory * 100);
//        log.info("redis usedMemoryPercent={}", usedMemoryPercent);
//    }
//
//    @Override
//    public Set<TK> keySet() {
//        Set<TK> keys = super.keySet();
//        if (store != null) {
//            keys.addAll(store.keySet());
//        }
//        return keys;
//    }
//
//    @Override
//    public TV put(TK key, TV value, int expireMinutes) {
//        if (useStore(expireMinutes)) {
//            storeFilter.put(store.transferKey(key));
//            return store.put(key, value, expireMinutes);
//        }
//        return super.put(key, value, expireMinutes);
//    }
//
//    private boolean useStore(int expireMinutes) {
//        if (store == null) {
//            return false;
//        }
//        return expireMinutes == PERSISTENT_EXPIRE || usedMemoryPercent > useStoreThreshold;
//    }
//
//    @Override
//    public TV remove(TK k) {
//        if (store != null && storeFilter.mightContain(store.transferKey(k))) {
//            return store.remove(k);
//        }
//        return super.remove(k);
//    }
//
//    @Override
//    public void clear() {
//        super.clear();
//        if (store != null) {
//            store.clear();
//        }
//    }
//
//    @Override
//    public TV get(TK k) {
//        if (store != null && storeFilter.mightContain(store.transferKey(k))) {
//            return store.get(k);
//        }
//        return super.get(k);
//    }
//
//    @Override
//    public TV get(TK k, BiFunc<TK, TV> biFunc, int expireMinutes) {
//        if (useStore(expireMinutes) && storeFilter.mightContain(store.transferKey(k))) {
//            return store.get(k, biFunc, expireMinutes);
//        }
//        return super.get(k, biFunc, expireMinutes);
//    }
//}
