package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 返回查询俱乐部key信息
 *
 * @author zaf
 */
@Data
public class SUnion_FindClubSignInfo extends BaseSendMsg {
    /**
     * 俱乐部名称
     */
    private String clubName;
    /**
     * 俱乐部圈主名称
     */
    private String createName;

    public static SUnion_FindClubSignInfo make(String clubName, String createName) {
        SUnion_FindClubSignInfo ret = new SUnion_FindClubSignInfo();
        ret.setClubName(clubName);
        ret.setCreateName(createName);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
