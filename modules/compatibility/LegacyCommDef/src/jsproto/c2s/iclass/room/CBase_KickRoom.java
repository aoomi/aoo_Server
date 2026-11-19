package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 踢出房间
 *
 * @author Administrator
 */
public class CBase_KickRoom extends BaseSendMsg {
    // 房间id
    public long roomID;
    // 位置
    public int posIndex;


    public static CBase_KickRoom make(long roomID, int posIndex) {
        CBase_KickRoom ret = new CBase_KickRoom();
        ret.setRoomID(roomID);
        ret.setPosIndex(posIndex);
        return ret;
    }


    public long getRoomID() {
        return roomID;
    }


    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }


    public int getPosIndex() {
        return posIndex;
    }


    public void setPosIndex(int posIndex) {
        this.posIndex = posIndex;
    }


}