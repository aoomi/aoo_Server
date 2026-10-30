package jsproto.c2s.cclass.arena;

import cenum.ArenaEnum.RatioEnum;

/**
 * 比赛场玩家的比赛场信息
 *
 * @author Administrator
 */
public class ArenaPlayerRoomInfo {
    private String roomKey;
    private RatioEnum ratioEnum = RatioEnum.InitPoint;

    public ArenaPlayerRoomInfo(String roomKey, RatioEnum ratioEnum) {
        super();
        this.roomKey = roomKey;
        this.ratioEnum = ratioEnum;
    }

    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    public RatioEnum getRatioEnum() {
        return ratioEnum;
    }

    public void setRatioEnum(RatioEnum ratioEnum) {
        this.ratioEnum = ratioEnum;
    }

}
