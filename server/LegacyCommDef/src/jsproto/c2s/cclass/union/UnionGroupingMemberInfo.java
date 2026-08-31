package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 亲友圈分组成员信息
 *
 * @author Administrator
 */
@Data
public class UnionGroupingMemberInfo {
    /**
     * 分组ID
     */
    private long groupingId;
    /**
     * 玩家信息
     */
    private ShortPlayer player;


    public UnionGroupingMemberInfo(long groupingID, ShortPlayer player) {
        super();
        this.groupingId = groupingID;
        this.player = player;
    }


}
