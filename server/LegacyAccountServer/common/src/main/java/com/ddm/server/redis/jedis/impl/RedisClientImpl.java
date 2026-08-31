package com.ddm.server.redis.jedis.impl;

import com.ddm.server.redis.jedis.RedisClient;
import org.springframework.data.redis.core.*;


public class RedisClientImpl implements RedisClient {
    /**
     * 操作
     */
    private RedisTemplate<String,Object> redisTemplate;

    public void setJedisPool(RedisTemplate<String,Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 操作类
     * @return
     */
    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * Hash数据结构
     * @return
     */
    @Override
    public HashOperations<String, Object, Object> opsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * ZSe数据结构
     * @return
     */
    @Override
    public ZSetOperations<String, Object> opsForZSet() {
        return redisTemplate.opsForZSet();
    }

    /**
     * Set数据结构
     * @return
     */
    @Override
    public SetOperations<String, Object> opsForSet() {
        return redisTemplate.opsForSet();
    }

    /**
     * List数据结构
     * @return
     */
    @Override
    public ListOperations<String, Object> opsForList() {
        return redisTemplate.opsForList();
    }

    /**
     * Value数据结构
     * @return
     */
    @Override
    public ValueOperations<String, Object> opsForValue() {
        return redisTemplate.opsForValue();
    }



}
