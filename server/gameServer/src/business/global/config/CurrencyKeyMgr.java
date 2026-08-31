package business.global.config;

import BaseThread.BaseMutexObject;
import business.global.room.sharekey.ShareCurrencyKeyMgr;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;

import java.util.BitSet;
import java.util.Objects;

/**
 * 通用key管理器
 * 公会key ,亲友圈key,联赛key
 *
 * @author
 */
public class CurrencyKeyMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static CurrencyKeyMgr instance = new CurrencyKeyMgr();
    }

    // 私有化构造方法
    private CurrencyKeyMgr() {
    }

    private final BitSet set = new BitSet(899999);

    // 获取单例
    public static CurrencyKeyMgr getInstance() {
        return CurrencyKeyMgr.SingletonHolder.instance;
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    public void init() {
        // 初始 100000-999999,key存在。
        set.set(100000, 999999, true);
    }


    /**
     * 清空key
     *
     * @param key
     */
    public void clearKey(int key) {
        if (Config.isShare()) {
            ShareCurrencyKeyMgr.getInstance().clearKey(String.valueOf(key));
        } else {
            try {
                lock();
                if (Objects.isNull(set)) {
                    return;
                }
                if (key <= 0) {
                    return;
                }
                set.clear(key);
            } finally {
                unlock();
            }
        }
    }

    /**
     * 领取key
     *
     * @return
     */
    public int getNewKey() {
        if (Config.isShare()) {
            return Integer.parseInt(ShareCurrencyKeyMgr.getInstance().getNoUseKey());
        } else {
            try {
                lock();
                if (Objects.isNull(set)) {
                    return 0;
                }
                if (set.cardinality() <= 0) {
                    return 0;
                }
                int index = set.nextSetBit(CommMath.randomInt(set.cardinality() - 1));
                if (index < 0) {
                    return 0;
                }
                set.clear(index);
                return index;
            } finally {
                unlock();
            }
        }
    }

    /**
     * 归还key
     *
     * @param key
     */
    public void giveBackKey(int key) {
        if (Config.isShare()) {
            ShareCurrencyKeyMgr.getInstance().giveBackKey(String.valueOf(key));
        } else {
            try {
                lock();
                if (Objects.isNull(set)) {
                    return;
                }
                if (key <= 0) {
                    return;
                }
                if (!this.set.get(key)) {
                    this.set.set(key, true);
                }
            } finally {
                unlock();
            }
        }
    }

}
