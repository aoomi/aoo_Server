package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

import java.io.Serializable;

/**
 * 圈主名下普通玩家 中至
 * 俱乐部玩家信息
 *
 * @author
 */
@Data
public class ClubNormalPlayerInfo implements Serializable {
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
     * 上级名字
     */
    private String upPlayerName = "";

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
     * 调入 0
     * 调出 1
     */
    private int type = -1;

    /**
     * 生存积分
     */
    private double alivePoint = 0d;
    /**
     * 当前积分
     */
    private double sportsPoint;
    /**
     * 个人淘汰分
     */
    private double eliminatePoint = 0d;

    public ClubNormalPlayerInfo(ShortPlayer shortPlayer, int status, int isminister, int time) {
        this.shortPlayer = shortPlayer;
        this.status = status;
        this.minister = isminister;
        this.time = time;

    }

    public ClubNormalPlayerInfo(ShortPlayer shortPlayer, int status, int isminister, int time, String upPlayerName, double eliminatePoint, double sportsPoint) {
        this.shortPlayer = shortPlayer;
        this.status = status;
        this.minister = isminister;
        this.time = time;
        this.upPlayerName = upPlayerName;
        this.eliminatePoint = eliminatePoint;

        this.sportsPoint = sportsPoint;
    }

    public ClubNormalPlayerInfo(ShortPlayer shortPlayer, int status, int isminister, int time, String upPlayerName, int type) {
        this.shortPlayer = shortPlayer;
        this.status = status;
        this.minister = isminister;
        this.time = time;
        this.upPlayerName = upPlayerName;
        this.type = type;

    }

    public ClubNormalPlayerInfo(ShortPlayer shortPlayer, String upPlayerName, double eliminatePoint, double alivePoint, double sportsPoint) {
        this.shortPlayer = shortPlayer;
        this.upPlayerName = upPlayerName;
        this.eliminatePoint = eliminatePoint;
        this.alivePoint = alivePoint;
        this.sportsPoint = sportsPoint;


    }

}
