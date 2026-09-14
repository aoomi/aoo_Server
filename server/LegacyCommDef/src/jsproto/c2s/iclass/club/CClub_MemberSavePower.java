package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_MemberSavePower extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     * 查询的pid
     */
    private long pid;
    /**
     * 加入审核
     */
    private int joinPower;
    /**
     * 踢出成员
     */
    private int kickPower;
    /**
     * 调整玩法
     */
    private int changeCfgPower;
    /**
     * 编辑公告
     */
    private int edictNoticePower;
    /**
     * 邀请成员
     */
    private int invitePower;
    /**
     * 桌子踢人
     */
    private int kickTablePower;
    /**
     * 战绩查看
     */
    private int recordPower;
    /**
     * 举报成员
     */
    private int reportPower;
    /**
     * 比赛权限
     */
    private int matchPower;

    public CClub_MemberSavePower() {
    }

    public CClub_MemberSavePower(long clubId, long pid) {
        this.clubId = clubId;
        this.pid = pid;
    }
}
