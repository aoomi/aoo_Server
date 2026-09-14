package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class S1005_PlayerInfo extends BaseSendMsg {
    /**
     * 用户Pid
     */
    public long pid;
    /**
     * 账号Id
     */
    public long accountID;
    /**
     * 名称
     */
    public String name;
    /**
     * 图标(不知原作者用意)
     */
    public int icon;
    /**
     * 性别
     */
    public int sex;
    /**
     * 头像url
     */
    public String headImageUrl;
    /**
     * 创建时间
     */
    public long createTime;
    /**
     * 等级(现：连续登陆天数)
     */
    public int lv;
    /**
     * gm权限(无用)
     */
    public int gmLevel;
    /**
     * vip等级(现：是否试玩用户)
     */
    public int vipLv;
    /**
     * vipExp(现：vip经验(活跃次数(周)))
     */
    public int vipExp;
    /**
     * 累计充值, 统计玩家充值RMB
     */
    public int totalRecharge;
    /**
     * 钻石(兑换奖品积分)
     */
    public int crystal;
    /**
     * 金币（练习场专属）
     */
    public int gold;
    /**
     * 房卡数量
     */
    public int roomCard;
    /**
     * 闪电卷（无用）
     */
    public int fastCard;

    /**
     * 真实名字
     */
    public String realName;
    /**
     * 真实号码(身份证号)
     */
    public String realNumber;
    /**
     * 当前的游戏类型
     */
    public int currentGameType;
    /**
     * 圈卡
     */
    public int clubCard;
    /**
     * 时间(不懂)
     */
    public long time;
    /**
     * 时间（不懂）
     */
    public long startServerTime;
    /**
     * 标识
     */
    public String sign;

    /**
     * 城市Id
     */

    public static S1005_PlayerInfo make(long pid, long accountID, String name, int icon, int sex, String headImageUrl, long createTime, int lv, int gmLevel, int vipLv, int vipExp, int totalRecharge, int crystal, int gold, int roomCard, int fastCard, String realName, String realNumber, int currentGameType, int clubCard, String sign) {
        S1005_PlayerInfo ret = new S1005_PlayerInfo();
        ret.pid = pid;
        ret.accountID = accountID;
        ret.name = name;
        ret.icon = icon;
        ret.sex = sex;
        ret.headImageUrl = headImageUrl;
        ret.createTime = createTime;
        ret.lv = lv;
        ret.gmLevel = gmLevel;
        ret.vipLv = vipLv;
        ret.vipExp = vipExp;
        ret.totalRecharge = totalRecharge;
        ret.crystal = crystal;
        ret.gold = gold;
        ret.roomCard = roomCard;
        ret.fastCard = fastCard;
        ret.realName = realName;
        ret.realNumber = realNumber;
        ret.currentGameType = currentGameType;
        ret.clubCard = clubCard;
        ret.sign = sign;
        return ret;


    }
}
