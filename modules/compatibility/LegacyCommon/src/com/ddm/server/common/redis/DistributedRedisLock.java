package com.ddm.server.common.redis;

import BaseCommon.CommLog;

/**
 * @author xsj
 * @date 2020/9/15 9:31
 * @description redis分布式锁
 */
public class DistributedRedisLock {
    private static final String LOCK_TITLE = "redisLock_";
    private static final String LOCK_SUCCESS = "OK";

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否获取成功
     */
    public static boolean acquire(String lockKey, String requestId) {
        for (int attempt = 0; attempt < 600; attempt++) {
            String result = RedisUtil.set(LOCK_TITLE + lockKey, requestId, "NX", "EX", 30);
            if (LOCK_SUCCESS.equals(result)) {
                return true;
            }
            try {
                CommLog.error("没拿到分布式锁[key:{} value={}]", LOCK_TITLE + lockKey, requestId);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                CommLog.error(e.getMessage(), e);
                return false;
            }
        }
        CommLog.error("获取分布式锁超时[key:{} value={}]", LOCK_TITLE + lockKey, requestId);
        return false;
    }

    /**
     * 尝试获取分布式锁放弃的
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否获取成功
     */
    public static boolean acquireGiveUp(String lockKey, String requestId) {
        String result = RedisUtil.set(LOCK_TITLE + lockKey, requestId, "NX", "EX", 30);
        if (LOCK_SUCCESS.equals(result)) {
            return true;
        } else {
            CommLog.error("分布式锁已经在使用了[key:{} value={}]", LOCK_TITLE + lockKey, requestId);
            return false;
        }

    }

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean release(String lockKey, String requestId) {
        String result = RedisUtil.get(LOCK_TITLE + lockKey);
        if (requestId.equals(result)) {
            RedisUtil.del(LOCK_TITLE + lockKey);
            return true;
        }
        CommLog.error("删除分布式锁失败[key:{} value={}]", LOCK_TITLE + lockKey, requestId);
        return false;
    }
}
