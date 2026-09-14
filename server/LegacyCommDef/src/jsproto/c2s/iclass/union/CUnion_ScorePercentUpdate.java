package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 执行收益更新
 *
 * @author zaf
 */
@Data
public class CUnion_ScorePercentUpdate extends CUnion_Base {
    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;

    /**
     * 操作值
     */
    private double value;
    /**
     * 分成类型 0百分比 1固定值
     */

    private int shareType;
}