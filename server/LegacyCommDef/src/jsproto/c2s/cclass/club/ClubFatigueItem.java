package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 亲友圈疲劳值系统项
 *
 * @author
 */
@Data
public class ClubFatigueItem {
    /**
     * 玩家信息
     */
    private ShortPlayer player;
    /**
     * 玩家疲劳值
     */
    private int fatigueValue;

    public ClubFatigueItem(ShortPlayer player, int fatigueValue) {
        super();
        this.player = player;
        this.fatigueValue = fatigueValue;
    }

    public ClubFatigueItem() {
        super();
    }

}
