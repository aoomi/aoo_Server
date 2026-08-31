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
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 操作的亲友圈
     */
    private long opClubId;

}