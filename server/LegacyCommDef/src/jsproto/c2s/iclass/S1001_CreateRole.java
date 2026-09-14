package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class S1001_CreateRole extends BaseSendMsg {

    public long pid;
    public long accountID;
    public String name;
    public int icon;
    public int sex;
    public String headImageUrl;
    public long createTime;
    public int lv;
    public int gmLevel;
    public int vipLv;
    public int vipExp;
    public int totalRecharge;
    public int crystal;
    public int gold;
    public int roomCard;
    public int fastCard;


    public static S1001_CreateRole make(long pid, long accountID, String name, int icon, int sex, String headImageUrl, long createTime, int lv, int gmLevel, int vipLv, int vipExp, int totalRecharge, int crystal, int gold, int roomCard, int fastCard) {
        S1001_CreateRole ret = new S1001_CreateRole();
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

        return ret;


    }
}