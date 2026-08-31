package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 游戏ID列表
 *
 * @author Administrator
 */
@Data
public class CBase_RoomInvitationList extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 数量
     */
    private int size = 4;

    /**
     * 房间id
     */
    private long roomID;


}