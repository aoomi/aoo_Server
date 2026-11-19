package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import lombok.Data;

/**
 * @author FengZhnag
 * @date 2022/6/10 14:59
 * @description 中至排行榜信息 个人
 */
@Data
public class ClubRankedZhongZhiSelf {
    /**
     * 我的名次
     * 0的话未上榜
     */
    private int id;

    /**
     * 玩家信息
     */
    private Player.ShortPlayer player;

    private double itemsValue;

    public ClubRankedZhongZhiSelf(int id, Player.ShortPlayer player, double value) {
        this.player = player;
        this.itemsValue = value;
    }

    public ClubRankedZhongZhiSelf(Player.ShortPlayer player) {
        this.player = player;
    }
}
