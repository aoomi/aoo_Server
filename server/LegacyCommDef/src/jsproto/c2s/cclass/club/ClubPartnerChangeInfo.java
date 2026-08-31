package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 亲友圈合伙人变更信息
 *
 * @author Administrator
 */
@Data
public class ClubPartnerChangeInfo {
    /**
     * 合伙人信息
     */
    private ShortPlayer player;
    /**
     * 标记
     */
    private boolean sign;

    public ClubPartnerChangeInfo(ShortPlayer player, boolean sign) {
        super();
        this.player = player;
        this.sign = sign;
    }
}
