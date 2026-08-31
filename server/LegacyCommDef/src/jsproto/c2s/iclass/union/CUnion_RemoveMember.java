package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 赛事移除成员信息
 *
 * @author zaf
 */
@Data
public class CUnion_RemoveMember extends CUnion_Base {
    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;

}