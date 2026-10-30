package com.ddm.server.redis.jedis;


import org.springframework.data.redis.connection.DataType;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RedisSource {

	public static long increment(String key) {
		Long value = RedisUtil.opsForValue().increment(key);
		return Objects.isNull(value) ? 0:value.longValue();
	}

	public static Long increment(String key,long delta) {
		return RedisUtil.opsForValue().increment(key,delta);
	}


	public static String get(String key) {
		Object o =  RedisUtil.opsForValue().get(key);
		return Objects.nonNull(o) ? o.toString() : "";
	}

	public static String put(String key, String value) {
		RedisUtil.opsForValue().set(key,value );
		return value;
	}

	public static void setex(String key, String value, long time, TimeUnit timeUnit) {
		RedisUtil.opsForValue().set(key,value , time, timeUnit);
	}

	public static boolean exists(String key) {
		return RedisUtil.getRedisTemplate().hasKey(key).booleanValue();
	}
	
	public static boolean remove(String key) {
		return RedisUtil.getRedisTemplate().delete(key);
	}
	
	public DataType getType(String key) {
		return RedisUtil.getRedisTemplate().type(key);
	}
	
	public static RedisMap getMap(String key) {
		return new RedisMap(key);
	}

	public static RedisSet getSet(String key) {
		return new RedisSet(key);
	}

	public static RedisSetTuple getSetTuple(String key) {
		return new RedisSetTuple(key);
	}

}
