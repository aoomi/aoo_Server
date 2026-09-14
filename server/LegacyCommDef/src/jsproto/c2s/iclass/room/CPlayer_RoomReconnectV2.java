package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

public class CPlayer_RoomReconnectV2 extends BaseSendMsg {
    private long roomId;
    private long lastServerSeq;
    private String reconnectToken;

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
    public long getLastServerSeq() { return lastServerSeq; }
    public void setLastServerSeq(long lastServerSeq) { this.lastServerSeq = lastServerSeq; }
    public String getReconnectToken() { return reconnectToken; }
    public void setReconnectToken(String reconnectToken) { this.reconnectToken = reconnectToken; }
}
