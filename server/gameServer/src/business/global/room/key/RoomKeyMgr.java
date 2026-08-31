package business.global.room.key;

import BaseThread.BaseMutexObject;
import business.global.room.sharekey.ShareRoomKeyMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import org.apache.commons.lang3.StringUtils;

import java.util.BitSet;

/**
 * 房间key管理器
 */
public class RoomKeyMgr {
    private static RoomKeyMgr instance = new RoomKeyMgr();
    private final BitSet set = new BitSet(899999);
    private final BaseMutexObject _lock = new BaseMutexObject();

    public static RoomKeyMgr getInstance() {
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
    public final void init() {
        // 初始 100000-999999,key存在。
        set.set(100000, 999999, true);
    }

    /**
     * 领取key
     *
     * @return
     */
    public String getNewKey() {
        if (Config.isShare()) {
            return ShareRoomKeyMgr.getInstance().getNoUseKey();
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
                    // 出现标问题
                    CommLogD.error("RoomKeyMgr getNewKey index < 0");
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
            ShareRoomKeyMgr.getInstance().giveBackKey(key);
        } else {
            try {
                lock();
                // key == 空 或者 不是数字
                if (StringUtils.isEmpty(key) || !StringUtils.isNumeric(key)) {
                    return;
                }
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

    /**
     * 是否使用
     *
     * @param key 房间key
     * @return T:没使用，F，使用中
     */
    public boolean isNotExistUse(String key) {
        if (Config.isShare()) {
            return ShareRoomKeyMgr.getInstance().isNotExistUse(key);
        } else {
            try {
                lock();
                // 检查是否存在
                if (null == this.set) {
                    return false;
                }
                int index = Integer.parseInt(key);
                if (index < 0) {
                    return false;
                }
                if (this.set.get(index)) {
                    this.set.set(index, false);
                    return true;
                } else {
                    return false;
                }
            } finally {
                unlock();
            }
        }
    }


    /**
     * 是否使用
     *
     * @param key 房间key
     * @return T:使用中，F，没使用
     */
    public boolean isExistUse(String key) {
        return !this.isNotExistUse(key);
    }

}
