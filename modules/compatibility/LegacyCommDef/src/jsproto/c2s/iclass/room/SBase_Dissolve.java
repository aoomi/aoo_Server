package jsproto.c2s.iclass.room;

import cenum.room.DissolveNoticeTypeEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 房间解散通知
 *
 * @author Administrator
 */
@Data
public class SBase_Dissolve extends BaseSendMsg {
    /**
     * 房间Id
     */
    private long roomID;
    /**
     * 是否房主解散
     */
    private boolean ownnerForce;
    /**
     * 解散类型
     */
    private int dissolveNoticeType = DissolveNoticeTypeEnum.DEFAULT.ordinal();
    /**
     * 解散信息
     */
    private String msg;

    public static SBase_Dissolve make(long roomID, boolean ownnerForce) {
        SBase_Dissolve ret = new SBase_Dissolve();
        ret.setRoomID(roomID);
        ret.setOwnnerForce(ownnerForce);
        return ret;
    }


    public static SBase_Dissolve make(long roomID, int dissolveNoticeType, boolean ownnerForce) {
        SBase_Dissolve ret = new SBase_Dissolve();
        ret.setRoomID(roomID);
        ret.setOwnnerForce(ownnerForce);
        ret.setDissolveNoticeType(dissolveNoticeType);
        return ret;
    }

    public static SBase_Dissolve make(long roomID, int dissolveNoticeType) {
        SBase_Dissolve ret = new SBase_Dissolve();
        ret.setRoomID(roomID);
        ret.setOwnnerForce(false);
        ret.setDissolveNoticeType(dissolveNoticeType);
        return ret;
    }

    public static SBase_Dissolve make(long roomID, int dissolveNoticeType, String msg) {
        SBase_Dissolve ret = new SBase_Dissolve();
        ret.setRoomID(roomID);
        ret.setOwnnerForce(false);
        ret.setDissolveNoticeType(dissolveNoticeType);
        ret.setMsg(msg);
        return ret;
    }
}