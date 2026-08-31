package jsproto.c2s.cclass;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class RoomCfgCount {
    /**
     * 游戏中房间数
     */
    private final AtomicInteger roomCount = new AtomicInteger();
    /**
     * 游戏中玩家数
     */
    private final AtomicInteger playerCount = new AtomicInteger();

    public RoomCfgCount() {
    }


    public void addRoomCount() {
        roomCount.incrementAndGet();
    }

    public void addPlayerCount(int playerCount) {
        this.playerCount.addAndGet(playerCount);
    }


}
