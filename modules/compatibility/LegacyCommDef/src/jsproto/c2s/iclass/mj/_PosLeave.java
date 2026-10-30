package jsproto.c2s.iclass.mj;

import jsproto.c2s.iclass.room.SBase_PosLeave;

/**
 * 位置离开通知
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class _PosLeave extends SBase_PosLeave {

    public static _PosLeave make(SBase_PosLeave posLeave, String gameNameStr) {
        _PosLeave ret = new _PosLeave();
        ret.setRoomID(posLeave.getRoomID());
        ret.setPos(posLeave.getPos());
        ret.setBeKick(posLeave.isBeKick());
        ret.setOwnerID(posLeave.getOwnerID());
        ret.setKickOutTYpe(posLeave.getKickOutTYpe());
        ret.setMsg(posLeave.getMsg());
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}	
