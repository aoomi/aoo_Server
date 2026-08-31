package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部玩家信息状态改变
 *
 * @author zaf
 */
public class SClub_AlivePointChange extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long unionId;
    public int type;//0 关闭  1 开启
    public double taoTaiValue; //淘汰值
    public double personalSportsPointWarning; //个人预警值

    public static SClub_AlivePointChange make(long clubId, long unionId, int type, double taoTaiValue, double personalSportsPointWarning) {
        SClub_AlivePointChange ret = new SClub_AlivePointChange();
        ret.clubId = clubId;
        ret.unionId = unionId;
        ret.type = type;
        ret.taoTaiValue = taoTaiValue;
        ret.personalSportsPointWarning = personalSportsPointWarning;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}