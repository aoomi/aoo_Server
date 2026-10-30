package business.global.room.key;

import BaseThread.BaseMutexObject;
import business.global.room.sharekey.ShareGoldKeyMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.GameType;

import java.util.BitSet;

/**
 * 房间key管理器
 */
public class GoldKeyMgr {
    private static GoldKeyMgr instance = new GoldKeyMgr();
    private final BaseMutexObject _lock = new BaseMutexObject();
    /**
     * 初始key空间
     */
    private final BitSet set = new BitSet(899999);

    public static GoldKeyMgr getInstance() {
        return instance;
    }

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * 初始化房间key
     */
    public void init() {
        // 初始 100000-999999,key存在。
        set.set(100000, 999999, true);
    }

    /**
     * 领取key
     *
     * @param type 游戏类型
     * @return
     */
    public String getNewKey(GameType type) {
        if (Config.isShare()) {
            return ShareGoldKeyMgr.getInstance().getNoUseKey();
        } else {
            try {
                lock();
                // 检查key值是否用完。
                if (set == null || set.cardinality() <= 0) {
                    CommLogD.error("set == null || set.cardinality() <= 0");
                    return "";
                }
                // 随机获取key值
                int index = set.nextSetBit(CommMath.randomInt(set.cardinality() - 1));
                if (index < 0) {
                    return "";
                }
                // 在字典中清除获取的key值
                set.clear(index);
                String ret = Integer.toString(index);
                return ret;
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
    public void giveBackKey(String key) {
        if (Config.isShare()) {
            ShareGoldKeyMgr.getInstance().giveBackKey(key);
        } else {
            try {
                lock();
                // 检查是否存在
                if (null == this.set) {
                    return;
                }
                int index = Integer.parseInt(key);
                if (index < 0) {
                    return;
                }
                // 将key值归还。
                this.set.set(index, true);
            } finally {
                unlock();
            }
        }
    }
}
