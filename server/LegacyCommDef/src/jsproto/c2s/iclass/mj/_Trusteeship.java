package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 托管信息
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class _Trusteeship extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 玩家PID
    private long pid;
    // 位置
    private int pos;
    // T:托管,F:取消托管
    private boolean trusteeship;

    public static _Trusteeship make(long roomID, long pid, int pos, boolean trusteeship, String gameNameStr) {
        _Trusteeship ret = new _Trusteeship();
        ret.setRoomID(roomID);
        ret.setPid(pid);
        ret.setPos(pos);
        ret.setTrusteeship(trusteeship);
        ret.setGameNameStr(gameNameStr);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isTrusteeship() {
        return trusteeship;
    }

    public void setTrusteeship(boolean trusteeship) {
        this.trusteeship = trusteeship;
    }

}	
