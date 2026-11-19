package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * @author FengZhnag
 * @date 2022/8/5 10:03
 * @description 中至成员权利
 */
@Data
public class ClubMemberPowerZhongZhi {
    private long id;
    private long memberId;
    private long clubID;
    // 玩家游戏ID 长的
    private long playerID;
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
    /**
     * 更新时间
     */
    private int updateTime;

    public ClubMemberPowerZhongZhi() {
    }

    public ClubMemberPowerZhongZhi(long id, long memberId, long clubID, long playerID, int joinPower, int kickPower, int changeCfgPower, int edictNoticePower, int invitePower, int kickTablePower, int recordPower, int reportPower, int matchPower, int updateTime) {
        this.id = id;
        this.memberId = memberId;
        this.clubID = clubID;
        this.playerID = playerID;
        this.joinPower = joinPower;
        this.kickPower = kickPower;
        this.changeCfgPower = changeCfgPower;
        this.edictNoticePower = edictNoticePower;
        this.invitePower = invitePower;
        this.kickTablePower = kickTablePower;
        this.recordPower = recordPower;
        this.reportPower = reportPower;
        this.matchPower = matchPower;
        this.updateTime = updateTime;
    }

    public static String getItemsName() {
        return "id,memberId,clubID,playerID,joinPower,kickPower,changeCfgPower,edictNoticePower,invitePower,kickTablePower,recordPower,reportPower,matchPower,updateTime";
    }
}
