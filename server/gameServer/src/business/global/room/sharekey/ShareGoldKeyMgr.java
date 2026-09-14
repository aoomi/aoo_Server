package business.global.room.sharekey;

import BaseCommon.CommLog;
import BaseThread.BaseMutexManager;
import com.ddm.server.common.redis.RedisSet;
import core.ioc.ContainerMgr;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xsj
 * @date 2020/8/11 15:48
 * @description 共享练习场房间号管理类
 */
public class ShareGoldKeyMgr {
    //使用过的key
    private static final String SHARE_GOLD_NO_KEY_USE = "shareGoldNoKey_use";
    //未使用的key
    private static final String SHARE_GOLD_NO_KEY_NO = "shareGoldNoKey_no";
    private static ShareGoldKeyMgr instance = new ShareGoldKeyMgr();
    private final BaseMutexManager lock = new BaseMutexManager();

    public static ShareGoldKeyMgr getInstance() {
        return instance;
    }

    public final void init() {
        initKey();
    }

    /**
     * 初始化添加KEy
     */
    private void initKey() {
        long start = System.currentTimeMillis();
        RedisSet redisSetNo = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_NO);
        RedisSet redisSetUse = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_USE);
        if (redisSetUse.size() == 0) {
            Set<String> newSet = new HashSet<>(899999);
            for (long i = 100000; i < 1000000; i++) {
                String key = String.valueOf(i);
                newSet.add(key);
            }
            redisSetNo.addAll(newSet);
            CommLog.info("初始化练习场房间key耗时:{}", System.currentTimeMillis() - start);
        }
    }

    /**
     * 随机获取一个未使用的key并且设置到使用队列里面
     *
     * @return
     */
    public String getNoUseKey() {
        lock.lock();
        RedisSet redisSetNo = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_NO);
        String key = redisSetNo.getRandom();
        RedisSet redisSetUse = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_USE);
        redisSetUse.add(key);
        lock.unlock();
        return key;
    }

    /**
     * 随机获取一个未使用的key并且设置到使用队列里面
     *
     * @return
     */
    public void giveBackKey(String key) {
        lock.lock();
        RedisSet redisSetNo = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_NO);
        redisSetNo.add(key);
        RedisSet redisSetUse = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_USE);
        redisSetUse.remove(key);
        lock.unlock();
    }

    /**
     * 是否使用
     *
     * @param key 房间key
     * @return T:没使用，F，使用中
     */
    public boolean isNotExistUse(String key) {
        RedisSet redisSetNo = ContainerMgr.get().getRedis().getSet(SHARE_GOLD_NO_KEY_NO);
        return redisSetNo.contains(key);
    }

}
