package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionRoomCfgCount {
    /**
     * 游戏中房间数
     */
    private int roomCount;
    /**
     * 游戏中玩家数
     */
    private int playerCount;
    /**
     * 排序0:未勾选,1:勾选
     */
    private int sort;

    public UnionRoomCfgCount() {
    }

    public UnionRoomCfgCount(int roomCount, int playerCount) {
        this.roomCount = roomCount;
        this.playerCount = playerCount;
    }
}
