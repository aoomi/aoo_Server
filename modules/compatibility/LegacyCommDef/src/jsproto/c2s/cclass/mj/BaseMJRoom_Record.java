package jsproto.c2s.cclass.mj;

import jsproto.c2s.cclass.Player.ShortPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间记录
 *
 * @author Clark
 */

// 房间玩家信息
public class BaseMJRoom_Record {
    private long roomID;
    private int endSec;
    private int setCnt;
    private List<ShortPlayer> players = new ArrayList<>(); // 玩家信息

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getEndSec() {
        return endSec;
    }

    public void setEndSec(int endSec) {
        this.endSec = endSec;
    }

    public int getSetCnt() {
        return setCnt;
    }

    public void setSetCnt(int setCnt) {
        this.setCnt = setCnt;
    }

    public List<ShortPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<ShortPlayer> players) {
        this.players = players;
    }

}
