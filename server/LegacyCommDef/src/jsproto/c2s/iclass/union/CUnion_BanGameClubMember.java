package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止亲友圈成员游戏
 *
 * @author zaf
 */
@Data
public class CUnion_BanGameClubMember extends CUnion_Base {
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
    private int value;

}