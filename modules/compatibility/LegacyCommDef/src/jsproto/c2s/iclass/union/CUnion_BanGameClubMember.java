package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止俱乐部成员游戏
 *
 * @author zaf
 */
@Data
public class CUnion_BanGameClubMember extends CUnion_Base {
    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;
    /**
     * 操作类型(0:加,1:减)
     */
    private int type;
    /**
     * 操作值(>0)
     */
    private int value;

}