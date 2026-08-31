package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 执行竞技点更新
 *
 * @author zaf
 */
@Data
public class CUnion_SportsPointUpdate extends CUnion_Base {
    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;
    /**
     * 操作类型(0:加,1:减)
     */
    private int type;
    /**
     * 操作值(>0)
     */
    private double value;

}