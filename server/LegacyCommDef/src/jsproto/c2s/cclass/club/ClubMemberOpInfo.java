package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import lombok.Data;

/**
 * @author FengZhnag
 * @date 2022/8/5 10:03
 * @description 中至成员管理
 */
@Data
public class ClubMemberOpInfo {
    /**
     * 玩家信息
     */
    private Player.ShortPlayer player;
    /**
     * 权利信息
     */
    private ClubMemberPowerZhongZhi powerZhongZhi;
    /***
     * 职务 0普通会员 1管理 2创建者 3赛事管理员
     */
    private int isminister;
    /**
     * 最近游戏
     */
    private int lastGame=-1;
    /**
     * 加入时间
     */
    private int joinTime=0;
    /**
     * 比赛券数
     */
    private int gameTicket=0;
}
