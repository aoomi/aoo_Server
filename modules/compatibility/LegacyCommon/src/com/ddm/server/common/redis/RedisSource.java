package com.ddm.server.common.redis;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

public class RedisSource {

    public String get(String key) {
        return RedisUtil.get(key);
    }

    public <T> T getObject(String key, Class<T> clazz) {
        if (StringUtils.isNotEmpty(key)) {
            String result = get(key);
            if (StringUtils.isNotEmpty(result)) {
                return new Gson().fromJson(result, clazz);
            }
        }
        return null;
    }

    public String put(String key, String value) {
        return RedisUtil.set(key, value);
    }

    public boolean putAndSuccess(String key, String value) {
        return RedisUtil.setnx(key, value);
    }

    public String putWithTime(String key, int seconds, String value) {
        return RedisUtil.setex(key, seconds, value);
    }

    public boolean setExpireTime(String key, int seconds) {
        return RedisUtil.expire(key, seconds);
    }

    public boolean delExpireTime(String key) {
        return RedisUtil.persist(key);
    }

    public long getRemainTime(String key) {
        return RedisUtil.ttl(key);
    }

    public boolean exists(String key) {
        return RedisUtil.exists(key);
    }

    public boolean remove(String key) {
        return RedisUtil.del(key);
    }

    public String getType(String key) {
        return RedisUtil.type(key);
    }

    public RedisMap getMap(String key) {
        return new RedisMap(key);
    }

    public static RedisMap getMaps(String key) {
        return new RedisMap(key);
    }

    public RedisList getList(String key) {
        return new RedisList(key);
    }

    public RedisSet getSet(String key) {
        return new RedisSet(key);
    }

    public final static RedisSet getSetV(String key) {
        return new RedisSet(key);
    }

    public static RedisSetTuple getRedisSetTuple(String key) {
        return new RedisSetTuple(key);
    }

    public RedisDeque getDeque(String key) {
        return new RedisDeque(key);
    }

    public void clear() {
        RedisUtil.flushAll();
    }

    public Long incrLong(String key) {
        return RedisUtil.incrLong(key);
    }
}
