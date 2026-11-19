package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 联盟通知指定俱乐部进出改变
 */
@Data
public class SUnion_ClubChange extends BaseSendMsg {
    /**
     * 俱乐部Id
     */
    private long clubId;

    /**
     * 联盟ID
     */
    private long unionId;

    /**
     * 竞技点
     */
    private double sportsPoint;

    /**
     * 联盟名称
     */
    private String unionName;

    /**
     * 联盟职务
     */
    private int unionPostType;

    /**
     * 联盟标识
     */
    private int unionSign;

    public static SUnion_ClubChange make(long clubId, long unionId, String unionName, int unionSign) {
        SUnion_ClubChange ret = new SUnion_ClubChange();
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        ret.setUnionName(unionName);
        ret.setUnionSign(unionSign);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
