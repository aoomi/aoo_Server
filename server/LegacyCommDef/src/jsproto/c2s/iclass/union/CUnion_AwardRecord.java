package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_AwardRecord extends CUnion_Base {

    /**
     颁奖次数
     */
    private int awardNum;

    /**
     0 某次
     1 汇总
     */
    private int type;
    /**
     * 操作的亲友圈id
     */
    private int opClubId;

    /**
     *      * 0 全部 1未分配 2管理员3普通成员
     */
    private int statusType;
}