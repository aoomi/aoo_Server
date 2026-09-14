package jsproto.c2s.cclass.room;

import java.io.Serializable;

/**
 * 环信SDK通信信息
 *
 * @author Administrator
 */
public class RoomHXSDKChatInfo implements Serializable {
    // 房间ID
    private long roomID;
    // 环信会议ID
    private String conferenceID = "";
    // 密码
    private String psd = "";

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public String getConferenceID() {
        return conferenceID;
    }

    public void setConferenceID(String conferenceID) {
        this.conferenceID = conferenceID;
    }

    public String getPsd() {
        return psd;
    }

    public void setPsd(String psd) {
        this.psd = psd;
    }
}
