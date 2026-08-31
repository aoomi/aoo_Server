package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 执行职务类型更新
 *
 * @author zaf
 */
@Data
public class CUnion_PostTypeUpdate extends CUnion_Base {
    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;

    /**
     * 操作值
     */
    private int value;

}