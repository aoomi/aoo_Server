package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.GameTypeUrl;

/**
 * 创建房间
 *
 * @author Administrator
 */
public class SRoom_CreateRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 房间Key
    private String roomKey;
    // 创建类型
    private int createType;
    // 游戏类型
    private int gameType;
    //游戏连接地址
    private GameTypeUrl gameTypeUrl;

    public static SRoom_CreateRoom make(long roomID, String roomKey, int createType, int gameType, GameTypeUrl gameTypeUrl) {
        SRoom_CreateRoom ret = new SRoom_CreateRoom();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setCreateType(createType);
        ret.setGameType(gameType);
        ret.setGameTypeUrl(gameTypeUrl);
        return ret;
    }

    public static SRoom_CreateRoom make(long roomID, String roomKey, int createType, int gameType) {
        SRoom_CreateRoom ret = new SRoom_CreateRoom();
        ret.setRoomID(roomID);
        ret.setRoomKey(roomKey);
        ret.setCreateType(createType);
        ret.setGameType(gameType);
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

    public GameTypeUrl getGameTypeUrl() {
        return gameTypeUrl;
    }

    public void setGameTypeUrl(GameTypeUrl gameTypeUrl) {
        this.gameTypeUrl = gameTypeUrl;
    }
}