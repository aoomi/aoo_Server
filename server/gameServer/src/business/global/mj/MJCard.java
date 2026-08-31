package business.global.mj;

import java.util.Hashtable;

/**
 * 龙岩麻将 每一局麻将底牌信息
 *
 * @author Clark
 * <p>
 * <p>
 * 抓牌人是逆时针出手
 * 牌是顺时针被抓
 * <p>
 * <p>
 * 龙岩麻将牌编号说明
 * 1101-1901 1万-9万  1101-1104 4张1万
 * 2101-2901 1条-9条
 * 3101-3901 1筒-9筒
 * <p>
 * ==========4000==========
 * <p>
 * 4101 东
 * 4201 西
 * 4301 南
 * 4401 北
 * 4501 中
 * 4601 发
 * 4701 白
 * ==========5000=========
 * 5101 梅
 * 5201 兰
 * 5301 竹
 * 5401 菊
 * 5501 春
 * 5601 夏
 * 5701 秋
 * 5801 冬
 */


public class MJCard {

    public static Hashtable<Integer, String> TypeDict = new Hashtable<>();

    static {
        for (int i = 1; i <= 9; i++) {
            TypeDict.put(10 + i, i + "万");
            TypeDict.put(20 + i, i + "条");
            TypeDict.put(30 + i, i + "筒");
        }
        //风牌
        TypeDict.put(41, " 东");
        TypeDict.put(42, " 南");
        TypeDict.put(43, " 西");
        TypeDict.put(44, " 北");
        //箭牌
        TypeDict.put(45, " 中");
        TypeDict.put(46, " 发");
        TypeDict.put(47, " 白");
        //花牌
        TypeDict.put(51, " 梅");
        TypeDict.put(52, " 兰");
        TypeDict.put(53, " 竹");
        TypeDict.put(54, " 菊");
        TypeDict.put(55, " 春");
        TypeDict.put(56, " 夏");
        TypeDict.put(57, " 秋");
        TypeDict.put(58, " 冬");


    }

    public int cardID; // 1101
    public int type; // 11
    public int ownnerPos = -1;//玩家位置
    public boolean isLock = false;//0未锁，1锁牌

    public MJCard(int cardID) {
        this.cardID = cardID;
        this.type = cardID / 100;
    }

    @Override
    public String toString() {
        return "[cardID=" + cardID + ", type=" + type + ", ownnerPos="
                + ownnerPos + "]\n";
    }

    public Integer getCardID() {
        return cardID;
    }

    public void setCardID(int cardID) {
        this.cardID = cardID;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOwnnerPos() {
        return ownnerPos;
    }

    public void setOwnnerPos(int ownnerPos) {
        this.ownnerPos = ownnerPos;
    }

    public void setLock(boolean lock) {
        isLock = lock;
    }

    public boolean isLock() {
        return isLock;
    }

    public int getID(int value) {
        int dValue = type / 10;
        if (value == dValue) {
            return type * 10;
        } else {
            return type;
        }
    }

    /**
     * 获取颜色值0方块 1梅花 2红桃 3黑桃
     *
     * @return
     */
    public int getColor() {
        return (type - 10) / 20;
    }

    /**
     * 获取牌值
     *
     * @return
     */
    public int getValue() {
        return (type - 10) % 20;
    }

}

