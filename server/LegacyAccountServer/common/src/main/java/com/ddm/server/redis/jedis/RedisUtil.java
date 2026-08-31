package com.ddm.server.redis.jedis;

import org.springframework.data.redis.core.*;
/**
 * redis 工具类
 */
public class RedisUtil {
    private static RedisClient redisClient;

    public static void setRedisClient(RedisClient jedisClient) {
        redisClient = jedisClient;
    }


    /**
     * 操作类
     * @return
     */
    
    public static RedisTemplate<String, Object> getRedisTemplate() {
        return redisClient.getRedisTemplate();
    }


    /**
     * Hash数据结构
     * @return
     */
    
    public static HashOperations<String, Object, Object> opsForHash() {
        return redisClient.opsForHash();
    }

    /**
     * ZSe数据结构
     * @return
     */
    public static ZSetOperations<String, Object> opsForZSet() {
        return redisClient.opsForZSet();
    }

    /**
     * Set数据结构
     * @return
     */
    public static SetOperations<String, Object> opsForSet() {
        return redisClient.opsForSet();
    }

    /**
     * List数据结构
     * @return
     */
    public static ListOperations<String, Object> opsForList() {
        return redisClient.opsForList();
    }

    /**
     * Value数据结构
     * @return
     */
    public static ValueOperations<String, Object> opsForValue() {
        return redisClient.opsForValue();
    }


}
