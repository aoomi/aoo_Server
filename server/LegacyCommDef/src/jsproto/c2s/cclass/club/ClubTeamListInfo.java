package jsproto.c2s.cclass.club;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubTeamListInfo {
    /**
     * 序数id
     * 客户端排序用
     */
    private long id;
    /**
     * 最大id
     */
    private long maxId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 玩家头像
     */
    private String iconUrl;
    /**
     * 活跃积分
     */
    private double scorePoint;

    /**
     * 玩家数
     */
    private int number;
    /**
     * 总积分变化
     */
    private double sportsPointConsume;

    /**
     * 消耗钻石
     */
    private int consume;


    /**
     * 等级 0：普通成员，1：顶级代理 234567.....
     */
    private int level;

    /**
     * 生存积分
     */
    private Double alivePoint;
    /**
     * 个人淘汰分
     */
    private double eliminatePoint = 0d;
    /**
     * 职位
     * 0 普通成员
     * 1 推广员管理员
     * 2 推广员
     * 3 圈主
     */
    private int position = 0;

    public ClubTeamListInfo() {
    }

    public ClubTeamListInfo(long pid, String name, String iconUrl, int number, int level, double eliminatePoint, int position) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.consume = 0;
        this.sportsPointConsume = 0.0;
        this.level = level;
        this.scorePoint = 0.0;
        this.eliminatePoint = eliminatePoint;
        this.position = position;

    }

    public ClubTeamListInfo(long pid, String name, String iconUrl, int number, int consume, double sportsPointConsume, int level, double scorePoint, double eliminatePoint, int position) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.consume = consume;
        this.sportsPointConsume = sportsPointConsume;
        this.level = level;
        this.scorePoint = scorePoint;
        this.eliminatePoint = eliminatePoint;
        this.position = position;


    }


    public static String getItemsName() {
        return "sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as promotionShareValue,sum(roomSportsPointConsume) as actualEntryFee";
    }

    public static String getItemsNameMaxId() {
        return "max(id) as maxId,sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as promotionShareValue,sum(roomSportsPointConsume) as actualEntryFee";
    }

    public double formatDouble(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }


}
