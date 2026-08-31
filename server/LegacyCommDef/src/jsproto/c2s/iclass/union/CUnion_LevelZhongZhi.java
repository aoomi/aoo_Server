package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止亲友圈成员游戏
 *
 * @author zaf
 */
@Data
public class CUnion_LevelZhongZhi extends CUnion_Base {
    /**
     * 设置的值
     */
    private int value;

    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;
    /**
     * 操作玩家
     */
    private long pid;

}