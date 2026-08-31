package jsproto.c2s.cclass.union;

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
public class UnionGroupingInfo {
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

    public UnionGroupingInfo(long groupingID, int groupingSize, List<ShortPlayer> playerList) {
        super();
        this.groupingId = groupingID;
        this.groupingSize = groupingSize;
        this.playerList = playerList;
    }

    public UnionGroupingInfo(long groupingID) {
        super();
        this.groupingId = groupingID;
    }


    public UnionGroupingInfo() {
        super();
    }
}
