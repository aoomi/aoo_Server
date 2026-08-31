package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 赛事成员审核操作
 *
 * @author zaf
 */
@Data
public class CUnion_MemberExamineOperate extends CUnion_Base {
    /**
     * 操作对象亲友圈Id
     */
    private long opClubId;

    /**
     * 操作对象玩家Pid
     */
    private long opPid;

    /**
     * 0:加入审核,1:退出审核
     */
    private int type;

    /**
     * 0:同意,1:拒绝
     */
    private int operate;

}