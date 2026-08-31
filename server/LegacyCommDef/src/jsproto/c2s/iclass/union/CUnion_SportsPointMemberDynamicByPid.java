package jsproto.c2s.iclass.union;

import lombok.Data;

@Data
public class CUnion_SportsPointMemberDynamicByPid extends CUnion_Base {

    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 获取时间 0今天,1昨天,2最近三天,3近30天
     */
    private int getType;
    /**
     * 被操作Pid
     */
    private long opPid;

    /**
     * 被操作亲友圈Id
     */
    private long opClubId;

    /**
     * 选择的分类
     * 0 全部
     * 1 异常操作
     * 2 对局输赢
     * 3 报名费
     */
    private int chooseType;
    /**
     * 被操作Pid
     */
    private int pid;

    public CUnion_SportsPointMemberDynamicByPid(long unionId, long clubId, int pageNum, int getType, long opPid, long opClubId) {
        super(unionId, clubId);
        this.pageNum = pageNum;
        this.getType = getType;
        this.opPid = opPid;
        this.opClubId = opClubId;
    }
}
