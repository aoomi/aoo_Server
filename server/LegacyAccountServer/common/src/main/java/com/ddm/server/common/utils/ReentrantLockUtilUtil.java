package com.ddm.server.common.utils;

import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁
 * @author zhujianming
 * @date 2021-05-17 17:38
 */
public class ReentrantLockUtilUtil {
    /**
     * 创建不公平锁
     * @return
     */
    public static ReentrantLock makeNonfairLock(){
        return new ReentrantLock();
    }

    /**
     * 公平锁
     * @return
     */
    public static ReentrantLock makeFairLock(){
        return new ReentrantLock(true);
    }

    /**
     * 锁可中断
     *
     * @param lock 锁
     */
    public static void lockInterruptibly(ReentrantLock lock) throws Exception{
        Assert.notNull(lock, "lock must not be null");
        //当抢不到锁时，该线程允许被打断
        lock.lockInterruptibly();
    }

    /**
     * 锁
     *
     * @param lock 锁
     */
    public static void lock(ReentrantLock lock){
        Assert.notNull(lock, "lock must not be null");
        lock.lock();
    }

    /**
     * 试着锁
     *
     * @param lock 锁
     * @return boolean
     */
    public static boolean tryLock(ReentrantLock lock){
        Assert.notNull(lock, "lock must not be null");
        return lock.tryLock();
    }

    /**
     * 试着锁
     *
     * @param lock    锁
     * @param timeout 超时
     * @param unit    单位
     * @return boolean* @throws Exception 异常
     */
    public static boolean tryLock(ReentrantLock lock,long timeout, TimeUnit unit) throws Exception{
        Assert.notNull(lock, "lock must not be null");
        return lock.tryLock(timeout,unit);
    }

    /**
     * 解锁
     *
     * @param lock 锁
     */
    public static void unlock(ReentrantLock lock){
        Assert.notNull(lock, "lock must not be null");
        lock.unlock();
    }


}
