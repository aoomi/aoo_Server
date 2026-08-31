package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.GameTypeUrl;

/**
 * 进入房间
 *
 * @author Administrator
 */
public class SRoom_EnterRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 房间号
    private String roomKey;
    // 游戏类型
    private int gameType;
    //游戏连接地址
    private GameTypeUrl gameTypeUrl;

    public static SRoom_EnterRoom make(long roomID, String roomKey, int gameType, GameTypeUrl gameTypeUrl) {
        SRoom_EnterRoom ret = new SRoom_EnterRoom();
        ret.setRoomID(roomID);
        ret.setGameType(gameType);
        ret.setGameTypeUrl(gameTypeUrl);
        ret.setRoomKey(roomKey);
        return ret;
    }

    public static SRoom_EnterRoom make(long roomID, int gameType, GameTypeUrl gameTypeUrl) {
        SRoom_EnterRoom ret = new SRoom_EnterRoom();
        ret.setRoomID(roomID);
        ret.setGameType(gameType);
        ret.setGameTypeUrl(gameTypeUrl);
        return ret;
    }

    public static SRoom_EnterRoom make(long roomID, int gameType) {
        SRoom_EnterRoom ret = new SRoom_EnterRoom();
        ret.setRoomID(roomID);
        ret.setGameType(gameType);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public GameTypeUrl getGameTypeUrl() {
        return gameTypeUrl;
    }

    public void setGameTypeUrl(GameTypeUrl gameTypeUrl) {
        this.gameTypeUrl = gameTypeUrl;
    }

    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }
}