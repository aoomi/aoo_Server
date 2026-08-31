package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_AwardRecordDetail extends CUnion_Base {

    /**
    颁奖次数
     */
    private int awardNum;

    /**
     0 有效耗卡
     1 大赢家
     */
    private int type;
    /**
     * 操作的亲友圈Id
     */
    private long opClubId;

}