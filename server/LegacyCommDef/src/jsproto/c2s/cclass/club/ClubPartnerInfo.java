package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 亲友圈合伙人信息
 *
 * @author Administrator
 */
@Data
public class ClubPartnerInfo {
    /**
     * 玩家信息
     */
    private ShortPlayer player;
    /**
     * 玩家数
     */
    private int number;
    /**
     * 任命：1，卸任：0
     */
    private int partner;


    public ClubPartnerInfo(ShortPlayer player, int number, int partner) {
        super();
        this.player = player;
        this.number = number;
        this.partner = partner;
    }
}
