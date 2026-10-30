package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player;
import jsproto.c2s.cclass.union.UnionDynamicItemZhongZhiRecord;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人积分变化记录
 *
 * @author zaf
 */

@Data
@NoArgsConstructor
public class ClubCompetitionRecord extends BaseSendMsg {
    public Player.ShortPlayer player;
    /**
     * 变换记录
     */
    public List<UnionDynamicItemZhongZhiRecord> unionDynamicItemList;
    public int pageNum;

    /**
     * 成员积分
     */
    private double playerTotalPoint;
    /**
     * 总积分(中至)
     */
    private double zhongZhiTotalPoint;
    /**
     * 个人淘汰分
     */
    private double eliminatePoint;
}