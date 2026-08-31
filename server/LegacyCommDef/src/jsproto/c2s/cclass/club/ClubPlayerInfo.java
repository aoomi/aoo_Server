package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import lombok.Data;

import java.io.Serializable;

/**
 * 俱乐部玩家信息
 *
 * @author
 */
@Data
public class ClubPlayerInfo implements Serializable {
    /**
     * * 序数id
     * 客户端排序用
     */
    private long id;
    /**
     * 玩家信息
     */
    private ShortPlayer shortPlayer;

    /**
     * 上级玩家信息
     */
    private Player.ShortPlayer upShortPlayer;

    /**
     * 是否是管理,不是为null,是为1，2为创建者
     */
    private int minister;
    /**
     * 状态
     */
    private int status;
    /**
     * 时间
     */
    private int time;
    /**
     * 玩家圈卡
     */
    private int playerClubCard;
    /**
     * 是否禁止游戏
     */
    private boolean isBanGame;
    /**
     * 是否合伙人
     */
    private int promotion;

    /**
     * 竞技点
     */
    private double sportsPoint;
    /**
     * 是否为推广员管理
     */
    private int isPromotionManage;
    /**
     * 是否在线
     */
    private boolean onlineFlag;
    /**
     * 最近一次登录时间 日期时间
     */
    private String lastLogin;

    public ClubPlayerInfo(ShortPlayer shortPlayer, ShortPlayer upShortPlayer, int status, int isminister, int time, int playerClubCard,
                          boolean isBanGame, int promotion, double sportsPoint, int promotionManage) {
        this.shortPlayer = shortPlayer;
        this.upShortPlayer = upShortPlayer;
        this.status = status;
        this.minister = isminister;
        this.time = time;
        this.playerClubCard = playerClubCard;
        this.isBanGame = isBanGame;
        this.promotion = promotion;
        this.sportsPoint = sportsPoint;
        this.isPromotionManage = promotionManage;
    }

    public ClubPlayerInfo(ShortPlayer shortPlayer, ShortPlayer upShortPlayer, int status, int isminister, int time, int playerClubCard,
                          boolean isBanGame, int promotion, double sportsPoint, int promotionManage, boolean onlineFlag, String lastLogin) {
        this.shortPlayer = shortPlayer;
        this.upShortPlayer = upShortPlayer;
        this.status = status;
        this.minister = isminister;
        this.time = time;
        this.playerClubCard = playerClubCard;
        this.isBanGame = isBanGame;
        this.promotion = promotion;
        this.sportsPoint = sportsPoint;
        this.isPromotionManage = promotionManage;
        this.onlineFlag = onlineFlag;
        this.lastLogin = lastLogin;
    }

    public int getWaiPiZhun() {
        return status == Club_Player_Status.PLAYER_WEIPIZHUN.value() ? 1 : 0;
    }

    /**
     * 加入赛事管理者的
     * 如果是赛事管理者  则他的排序 和管理员一级
     *
     * @return
     */
    public int getUnionMgrSorted() {
        return minister == Club_define.Club_MINISTER.Club_MINISTER_UNIONMGR.value() ? Club_define.Club_MINISTER.Club_MINISTER_MGR.value() : minister;
    }
}
