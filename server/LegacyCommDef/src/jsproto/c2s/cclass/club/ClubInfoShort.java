package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import lombok.Data;

/**
 * 俱乐部信息
 * 简化版 2021/11/1傅哥要求
 */
@Data
public class ClubInfoShort {
    /**
     *
     */
    Player.ShortPlayer player;
    /**
     * 俱乐部ID
     */
    private long id;
    /**
     * 随机的俱乐部标识ID
     */
    private int clubsign;
    /**
     * 俱乐部名称
     */
    private String name;

    /**
     * 玩家俱乐部圈卡
     */
    private int playerClubCard = -1;
    /**
     * 工会ID
     */
    private long agentsID;
    /**
     * 代理等级
     */
    private int level;


    /**
     * 赛事ID
     */
    private long unionId;
    /**
     * 皮肤类型
     */
    private int skinType;
    /**
     * 显示上级及所属亲友圈
     */
    private int showUplevelId;
    /**
     * 显示本圈标志
     */
    private int showClubSign;
}
