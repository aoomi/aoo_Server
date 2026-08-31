package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 房间邀请操作
 *
 * @author Administrator
 */
@Data
public class CBase_RoomInvitationOperation extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 玩家pid
     */
    private long pid;
    /**
     * 房间id
     */
    private long roomID;
}