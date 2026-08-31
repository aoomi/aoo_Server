package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.GameTypeUrl;

/**
 * 创建房间
 *
 * @author Administrator
 */
public class SRoom_ContinueRoomInfo extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 房间Key
    private String roomKey;
    // 玩家名称
    private String name;
    // 房间Key
    private int continueType;
    // 老房间id
    private long oldRoomID;
    //游戏连接地址
    private GameTypeUrl gameTypeUrl;

    public static SRoom_ContinueRoomInfo make(long roomID, String roomKey, String name, int continueType, long oldRoomID, GameTypeUrl gameTypeUrl) {
        SRoom_ContinueRoomInfo ret = new SRoom_ContinueRoomInfo();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setContinueType(continueType);
        ret.setName(name);
        ret.setOldRoomID(oldRoomID);
        ret.setGameTypeUrl(gameTypeUrl);
        return ret;
    }

    public static SRoom_ContinueRoomInfo make(long roomID, String roomKey, String name, int continueType, long oldRoomID) {
        SRoom_ContinueRoomInfo ret = new SRoom_ContinueRoomInfo();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setContinueType(continueType);
        ret.setName(name);
        ret.setOldRoomID(oldRoomID);
        return ret;
    }

    public long getOldRoomID() {
        return oldRoomID;
    }

    public void setOldRoomID(long oldRoomID) {
        this.oldRoomID = oldRoomID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getContinueType() {
        return continueType;
    }

    public void setContinueType(int continueType) {
        this.continueType = continueType;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    public GameTypeUrl getGameTypeUrl() {
        return gameTypeUrl;
    }

    public void setGameTypeUrl(GameTypeUrl gameTypeUrl) {
        this.gameTypeUrl = gameTypeUrl;
    }
}