package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 亲友圈分组成员信息
 *
 * @author Administrator
 */
@Data
public class ClubGroupingMemberInfo {
    /**
     * 分组ID
     */
    private long groupingID;
    /**
     * 玩家信息
     */
    private ShortPlayer player;
    /**
     * 是否禁止T:禁止
     */
    private boolean isBan;


    public ClubGroupingMemberInfo(long groupingID, ShortPlayer player, boolean isBan) {
        super();
        this.groupingID = groupingID;
        this.player = player;
        this.isBan = isBan;
    }


}
