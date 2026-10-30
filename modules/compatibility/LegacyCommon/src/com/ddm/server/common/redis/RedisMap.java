package com.ddm.server.common.redis;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisMap implements Map<String, String> {

    private String sourceKey;

    RedisMap(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    @Override
    public void clear() {
        RedisUtil.del(sourceKey);
    }

    @Override
    public boolean containsKey(Object key) {
        return RedisUtil.hexists(sourceKey, key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        List<String> values = RedisUtil.hvals(sourceKey);
        return values.contains(value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return RedisUtil.hgetAll(sourceKey).entrySet();
    }

    public Map<String, String> entryMapString() {
        return RedisUtil.hgetAll(sourceKey);
    }

    public Map entryMap() {
        return RedisUtil.hgetAllObject(sourceKey);
    }

    public List<Map.Entry<String, String>> listMap(Integer cursor, Integer size) {
        return RedisUtil.hscan(sourceKey, cursor, size);
    }

    @Override
    public String get(Object key) {
        return RedisUtil.hget(sourceKey, key.toString());
    }

    public <T> T getObject(Object key) {
        return RedisUtil.hgetObject(sourceKey, key.toString());
    }

    public <T> T getObject(Object key, Class<T> clazz) {
        if (key != null) {
            String result = get(String.valueOf(key));
            if (StringUtils.isNotEmpty(result)) {
                Gson gson = new Gson();
                return gson.fromJson(result, clazz);
            }
        }
        return null;
    }

    public <T> T getBean(Object key, Class<T> clazz) {
        return getObject(key, clazz);
    }

    @Override
    public boolean isEmpty() {
        return RedisUtil.hlen(sourceKey) > 0;
    }

    public boolean isEmptyObject() {
        return RedisUtil.hlenObject(sourceKey) > 0;
    }

    @Override
    public Set<String> keySet() {
        return RedisUtil.hkeys(sourceKey);
    }

    @Override
    public String put(String key, String value) {
        if (value != null) {
            RedisUtil.hset(sourceKey, key, value);
        } else {
            RedisUtil.hdel(sourceKey, key);
        }
        return null;
    }

    public String putIf(String key, String value) {
        return put(key, value);
    }

    public String putObject(String key, Object value) {
        if (value != null) {
            RedisUtil.hsetObject(sourceKey, key, value);
        } else {
            RedisUtil.hdelObject(sourceKey, key);
        }
        return null;
    }

    public String putJson(String key, Object value) {
        if (value != null) {
            Gson gson = new Gson();
            put(key, gson.toJson(value));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (map.size() == 0) {
            return;
        }
        RedisUtil.hmset(sourceKey, (Map<String, String>) map);
    }

    @Override
    public String remove(Object key) {
        RedisUtil.hdel(sourceKey, key.toString());
        return null;
    }

    public String removeObject(Object key) {
        RedisUtil.hdelObject(sourceKey, key.toString());
        return null;
    }

    @Override
    public int size() {
        return (int) RedisUtil.hlen(sourceKey);
    }

    public int sizeObject() {
        return (int) RedisUtil.hlenObject(sourceKey);
    }

    @Override
    public Collection<String> values() {
        return RedisUtil.hvals(sourceKey);
    }

    @Override
    public String toString() {
        return RedisUtil.hgetAll(sourceKey).toString();
    }

    public Map<String, String> toMap() {
        Map<String, String> map = Maps.newHashMap();
        for (Entry<String, String> entry : this.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }
}
