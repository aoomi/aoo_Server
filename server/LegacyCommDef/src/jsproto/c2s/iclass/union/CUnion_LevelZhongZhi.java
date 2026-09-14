package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止俱乐部成员游戏
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
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;
    /**
     * 操作成员
     */
    private long pid;

}