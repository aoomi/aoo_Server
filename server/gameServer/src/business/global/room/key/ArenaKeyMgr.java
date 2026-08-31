package business.global.room.key;

import BaseThread.BaseMutexObject;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.TypeUtils;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.config.server.GameTypeMgr;
import jsproto.c2s.cclass.GameType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;

/**
 * 竞技场key管理器
 *
 * @author clark
 */
public class ArenaKeyMgr {
    private static ArenaKeyMgr instance = new ArenaKeyMgr();

    public static ArenaKeyMgr getInstance() {
        return instance;
    }

    /**
     * 初始key空间
     */
    private final BitSet set = new BitSet(899999);
    private Hashtable<String, Long> goldUsedKeys = new Hashtable<String, Long>();
    private Hashtable<Long, List<String>> gameScorekeys = new Hashtable<Long, List<String>>();
    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    public ArenaKeyMgr() {
        this.init();
    }

    public void init() {
        // 初始 100000-999999,key存在。
        set.set(100000, 999999, true);
    }

    // 领取key
    public String getNewKey(GameType type, int goldBaseScore) {
        try {
            lock();
            if (set == null) {
                return "";
            }
            if (set.cardinality() <= 0) {
                return "";
            }
            int index = set
                    .nextSetBit(CommMath.randomInt(set.cardinality() - 1));
            if (index < 0) {
                return "";
            }
            set.clear(index);
            String idexStr = index + "";
            long goldL = getGameTypeGold(type, goldBaseScore);
            goldUsedKeys.put(idexStr, goldL);
            addGameScore(goldL, idexStr);
            return idexStr;
        } finally {
            unlock();
        }
    }

    // 归还key
    public void giveBackKey(String playBackCode) {
        lock();

        if (set == null) {
            return;
        }
        if (StringUtils.isEmpty(playBackCode)) {
            return;
        }

        removeGameScore(playBackCode);
        unlock();
    }

    public long getGameTypeGold(GameType type, int goldBaseScore) {
        int gameType = type.getId() + 1;
        String gold = gameType + "00" + goldBaseScore;
        return TypeUtils.StringTypeLong(gold);
    }

    public void addGameScore(Long gameScorekey, String roomkey) {
        if (this.gameScorekeys.containsKey(gameScorekey)) {
            List<String> roomkeys = this.gameScorekeys.get(gameScorekey);
            roomkeys.add(roomkey);
            this.gameScorekeys.put(gameScorekey, roomkeys);

        } else {
            List<String> roomKeys = new ArrayList<String>();
            roomKeys.add(roomkey);
            this.gameScorekeys.put(gameScorekey, roomKeys);
        }
    }

    public void removeGameScore(String roomkey) {
        int playCode = TypeUtils.StringTypeInt(roomkey);
        // 检查 练习场房间号是否存在
        if (goldUsedKeys.containsKey(roomkey)) {
            // 通过练习场房间号,找到对应的游戏类型和底分编码
            Long gameScorekey = this.goldUsedKeys.get(roomkey);
            // 检查 游戏类型和底分的编码是否存在
            if (this.gameScorekeys.containsKey(gameScorekey)) {
                // 获取
                List<String> roomkeys = this.gameScorekeys.get(gameScorekey);
                roomkeys.remove(roomkey);
                if (roomkeys.size() <= 0) {
                    this.gameScorekeys.remove(gameScorekey);
                } else {
                    this.gameScorekeys.put(gameScorekey, roomkeys);
                }
            }
            if (!this.set.get(playCode)) {
                this.set.set(playCode, true);
            }
            this.goldUsedKeys.remove(roomkey);
        }

    }

    /**
     * 获取 房间编号
     *
     * @param gameScorekey
     * @return
     */
    public List<String> getKey(Long gameScorekey) {
        if (this.gameScorekeys.containsKey(gameScorekey)) {
            return gameScorekeys.get(gameScorekey);
        }
        return null;
    }

    public List<String> getAllRoomKeyByGameType() {
        ArrayList<String> keylst = new ArrayList<String>();
        // 获取游戏列表
        for (RefPractice refPractices : RefDataMgr.getAll(RefPractice.class)
                .values()) {
            if (null == refPractices) {
                continue;
            }
            // 获取房间数量。
            GameType gameType = GameTypeMgr.getInstance().gameType(refPractices.gameType);
            List<String> getKey = getKey(getGameTypeGold(
                    gameType,
                    refPractices.baseMark));
            if (null == getKey) {
                continue;
            }

            if (isGameType(refPractices.gameType, refPractices.baseMark)) {
                keylst.addAll(getKey);
            }
        }
        return keylst;
    }

    public boolean isGameType(String gameTypeStr, int baseMark) {
        String gameTypeName = gameTypeStr.toUpperCase();
        return "SSS".equalsIgnoreCase(gameTypeName) && baseMark > 20 ? false : true;
    }

}
