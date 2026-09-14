package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import lombok.Data;

/**
 * 查询结果
 */
@Data
public class ClubPromotionPidInfo {
    /**
     * 合伙人信息
     */
    private Player.ShortPlayer player;
    /**
     * 类型
     */
    private int type;

    public ClubPromotionPidInfo(Player.ShortPlayer player, int type) {
        this.player = player;
        this.type = type;
    }
}
