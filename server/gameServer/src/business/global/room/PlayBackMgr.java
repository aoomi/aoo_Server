package business.global.room;

import BaseThread.BaseMutexObject;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommFile;
import jsproto.c2s.cclass.playback.PlayBackDateTimeInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.BitSet;

/**
 * 回放码管理器
 *
 * @author clark
 */
@Data
public class PlayBackMgr {
    private static PlayBackMgr instance = new PlayBackMgr();

    public static PlayBackMgr getInstance() {
        return instance;
    }

    private PlayBackMgr() {
        // 初始 100000-999999,key存在。
        this.getSet().set(100000, 999999, true);
    }

    private final BitSet set = new BitSet(899999);

    /**
     * 取余值
     */
    private final static int MOD = 90;

    /**
     * 回放最小值
     */
    public final static int VALUE = 1000000;

    public void init() {
        initBitSet();
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

//    // 领取key
//    public int getNewKey() {
//        try {
//            lock();
//            // 检查key值是否用完。
//            if (set == null || set.cardinality() <= 0) {
//                CommLogD.error("PlayBackMgr set == null || set.cardinality() <= 0");
//                return 0;
//            }
//            // 随机获取key值
//            int index = set.nextSetBit(CommMath.randomInt(set.cardinality() - 1));
//            if (index < 0) {
//                // 出现标问题
//                CommLogD.error("PlayBackMgr getNewKey index < 0");
//                return 0;
//            }
//            // 在字典中清除获取的key值
//            set.clear(index);
//            return index;
//        } finally {
//            unlock();
//        }
//
//    }

//    public void clear() {
//        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
//
//        try {
//            lock();
////            // 初始 100000-999999,key存在。
////            this.getSet().set(100000, 999999, true);
//            //清理掉缓存的当前回放码
//            if(Config.isShare()){
//                SharePlayBackKeyMgr.getInstance().remove();
//            }
//        } finally {
//            unlock();
//        }
//    }

    /**
     * 第一级目录
     * 每日文件夹的可以
     *
     * @return
     */
    private final String dayKey() {
        DateTime nowTime = new DateTime();
        String nowToString = nowTime.toString("yyyyMMdd");
        int week = nowTime.getDayOfWeek();
        // 第一级目录java yyyyMMdd 周一
        // 2020021102
        return String.format("%s0%d", nowToString, week);
    }

    /**
     * 文件存储路径
     *
     * @param playBackDateTimeInfo
     * @return
     */
    public String path(PlayBackDateTimeInfo playBackDateTimeInfo) {
        //  ../bin/2020021102/62/2445112
        return String.format("%s/%s/%d/%d", Config.PlayBackFilePath(), playBackDateTimeInfo.getDayKey(), playBackDateTimeInfo.getCode() % MOD, playBackDateTimeInfo.getCode());
    }

    /**
     * 通过回放码获取文件
     *
     * @param code
     * @return
     */
    public String getFile(int code) {
        if (code < 1) {
            return null;
        }
        // 回放码：2445112
        DateTime nowTime = new DateTime();
        int week = nowTime.getDayOfWeek();
        // 回放码字符串
        String codeStr = String.valueOf(code);
        // 周几
        int codeWeek = Integer.parseInt(codeStr.substring(0, 1));
        if (codeWeek > 7) {
            return null;
        }
        // 放回值
        int codeMod = Integer.parseInt(codeStr.substring(1));
        // 根据周数获取日期时间
        String toString = (week >= codeWeek ? nowTime : nowTime.minusWeeks(1)).withDayOfWeek(codeWeek).toString("yyyyMMdd");
        // 第一级目录
        // 2020021102
        String key = String.format("%s0%d", toString, codeWeek);
        // 第二级目录
        // 62
        int mod = codeMod % MOD;
        //  ../bin/2020021102/62/2445112.json
        return String.format("%s/%s/%d/%d", Config.PlayBackFilePath(), key, mod, codeMod);
    }


    /**
     * 初始化BitSet
     */
    private final void initBitSet() {
        CommFile.FileNameList(String.format("%s/%s/", Config.PlayBackFilePath(), dayKey())).stream().forEach(k -> {
            if (StringUtils.isNumeric(k)) {
                set.clear(Integer.parseInt(k));
            }
        });
    }


}
