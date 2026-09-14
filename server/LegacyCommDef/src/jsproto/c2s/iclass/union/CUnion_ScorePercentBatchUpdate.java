package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.union.CUnionScorePercentItem;
import lombok.Data;

import java.util.List;

/**
 * 执行收益更新
 *
 * @author zaf
 */
@Data
public class CUnion_ScorePercentBatchUpdate extends CUnion_Base {
    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;

    /**
     * 0：百分比，1：固定值
     */
    private int type;

    /**
     * configId , scorePercent
     */
    List<CUnionScorePercentItem> unionScorePercentItemList;

}