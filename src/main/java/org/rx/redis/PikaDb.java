package org.rx.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.rx.bean.Tuple;
import org.rx.core.Reflects;

import java.lang.reflect.Type;

import static org.rx.core.App.*;

@Slf4j
public class PikaDb<TK, TV> extends RedisCache<TK, TV> {
    private static final String typeKey = "_type_", jsonKey = "_json_";
    private static final FastThreadLocal<Type> tsTypeToken = new FastThreadLocal<>();

    public PikaDb(String url) {
        super(url, Tuple.of(v -> {
            JSONObject json = new JSONObject(2);
            json.put(typeKey, v.getClass().getName());
            json.put(jsonKey, toJsonString(v));
            return json.toJSONString();
        }, v -> {
            if (v == null) {
                return null;
            }
            JSONObject json = JSON.parseObject((String) v);
            Type type = tsTypeToken.get();
            if (type == null) {
                type = Reflects.loadClass(json.getString(typeKey), true);
            }
            return fromJson(json.getString(jsonKey), type);
        }));
    }

    public TV get(TK k,@NonNull Type typeToken) {
        tsTypeToken.set(typeToken);
        try {
            return get(k);
        } finally {
            tsTypeToken.set(null);
        }
    }
}
