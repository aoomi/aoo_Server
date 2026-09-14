package com.ddm.server.redis.jedis;

import org.springframework.data.redis.core.*;

/**
 * 连接客户端
 */
public interface RedisClient {

    /**
     * 操作类
     * @return
     */
    public RedisTemplate<String, Object> getRedisTemplate();

    /**
     * Hash数据结构
     * @return
     */
    public HashOperations<String, Object, Object> opsForHash();

    /**
     * ZSe数据结构
     * @return
     */
    public ZSetOperations<String, Object> opsForZSet();

    /**
     * Set数据结构
     * @return
     */
    public SetOperations<String, Object> opsForSet();

    /**
     * List数据结构
     * @return
     */
    public ListOperations<String, Object> opsForList();

    /**
     * Value数据结构
     * @return
     */
    public ValueOperations<String, Object> opsForValue();
}
