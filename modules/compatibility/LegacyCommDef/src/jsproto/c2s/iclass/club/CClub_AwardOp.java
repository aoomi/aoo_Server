package jsproto.c2s.iclass.club;

import jsproto.c2s.iclass.union.CUnion_Base;
import lombok.Data;

/**
 * 中至颁奖
 *
 * @author zaf
 */
@Data
public class CClub_AwardOp  {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 俱乐部Id
     */
    private long clubId;
    /**
     * 操作的俱乐部
     */
    private long opClubId;

}