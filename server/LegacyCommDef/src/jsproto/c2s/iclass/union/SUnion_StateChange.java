package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SUnion_StateChange extends BaseSendMsg {

    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 赛事状态 0:启动,1:禁止,2:不足
     */
    private int stateType;

    /**
     * 本赛事结束时间
     */
    private int endRoundTime;

    public static SUnion_StateChange make(long unionId, int stateType) {
        SUnion_StateChange ret = new SUnion_StateChange();
        ret.setUnionId(unionId);
        ret.setStateType(stateType);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_StateChange make(long unionId, int stateType, int endRoundTime) {
        SUnion_StateChange ret = new SUnion_StateChange();
        ret.setUnionId(unionId);
        ret.setStateType(stateType);
        ret.setEndRoundTime(endRoundTime);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
