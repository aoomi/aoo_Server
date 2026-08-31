package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_RoomIdOperation extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;

    /**
     * 赛事Id
     */
    private long unionId;

    /**
     * 战绩结束时间
     */
    private int endTime;
    /**
     * 房间Id
     */
    private long roomID;

    /**
     * 操作类型 1:勾选
     */
    private int type;
}
