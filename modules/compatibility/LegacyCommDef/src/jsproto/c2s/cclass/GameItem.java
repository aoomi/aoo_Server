package jsproto.c2s.cclass;

import lombok.Data;

/**
 * 游戏项
 */
@Data
public class GameItem {
    /**
     * 游戏Id
     */
    private int gameId;

    /**
     * 折扣力度 <= -1:无效值
     */
    private int value;

    public GameItem(int gameId, int value) {
        this.gameId = gameId;
        this.value = value;
    }

    @Override
    public String toString() {
        return "GameItem{" +
                "gameId=" + gameId +
                ", value=" + value +
                '}';
    }
}
