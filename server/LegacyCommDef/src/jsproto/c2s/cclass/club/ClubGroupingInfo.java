package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分组信息
 *
 * @author Administrator
 */
@Data
public class ClubGroupingInfo {
    /**
     * 分组ID
     */
    private long groupingId;
    /**
     * 当前组人数
     */
    private int groupingSize;
    /**
     * 玩家信息。
     */
    private List<ShortPlayer> playerList = new ArrayList<>();

    public ClubGroupingInfo(long groupingId, int groupingSize, List<ShortPlayer> playerList) {
        super();
        this.groupingId = groupingId;
        this.groupingSize = groupingSize;
        this.playerList = playerList;
    }

    public ClubGroupingInfo(long groupingId) {
        super();
        this.groupingId = groupingId;
    }


    public ClubGroupingInfo() {
        super();
    }
}
