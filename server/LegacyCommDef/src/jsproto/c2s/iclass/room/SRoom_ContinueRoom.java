package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.GameTypeUrl;

/**
 * 创建房间
 *
 * @author Administrator
 */
public class SRoom_ContinueRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 房间Key
    private String roomKey;
    // 创建类型
    private int createType;
    // 游戏类型
    private int gameType;
    private int continueType;
    //游戏连接地址
    private GameTypeUrl gameTypeUrl;

    public static SRoom_ContinueRoom make(long roomID, String roomKey, int createType, int gameType, int continueType, GameTypeUrl gameTypeUrl) {
        SRoom_ContinueRoom ret = new SRoom_ContinueRoom();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setCreateType(createType);
        ret.setGameType(gameType);
        ret.setContinueType(continueType);
        ret.setGameTypeUrl(gameTypeUrl);
        return ret;
    }

    public static SRoom_ContinueRoom make(long roomID, String roomKey, int createType, int gameType, int continueType) {
        SRoom_ContinueRoom ret = new SRoom_ContinueRoom();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setCreateType(createType);
        ret.setGameType(gameType);
        ret.setContinueType(continueType);
        return ret;
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

    public int getCreateType() {
        return createType;
    }

    public void setCreateType(int createType) {
        this.createType = createType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameType() {
        return gameType;
    }

    public int getContinueType() {
        return continueType;
    }

    public void setContinueType(int continueType) {
        this.continueType = continueType;
    }

    public GameTypeUrl getGameTypeUrl() {
        return gameTypeUrl;
    }

    public void setGameTypeUrl(GameTypeUrl gameTypeUrl) {
        this.gameTypeUrl = gameTypeUrl;
    }
}