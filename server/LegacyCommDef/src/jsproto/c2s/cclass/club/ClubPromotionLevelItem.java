package jsproto.c2s.cclass.club;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPromotionLevelItem {
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
     * 更新时间
     */
    private int timestamp;
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
     * 玩家数
     */
    private int number;
    /**
     * 局数
     */
    private int setCount;
    /**
     * 赢家数
     */
    private int winner;
    /**
     * 报名费
     */
    private double entryFee;
    /**
     * 消耗钻石
     */
    private int consume;
    /**
     * 消耗比赛分(理论报名费  房间消耗/房间人数) 输赢比赛分
     */
    private double sportsPointConsume;
    /**
     * 个人比赛分
     */
    private double sportsPoint;
    /**
     * 总比赛分
     */
    private double sumSportsPoint;
    /**
     * 等级 0：普通成员，1：顶级代理 234567.....
     */
    private int level;

    /**
     * 是否创建者
     */
    private int myisminister;
    /**
     * 分成类型
     */
    private int shareType;
    /**
     * 分成百分比
     */
    private double shareValue;
    /**
     * 分成固定值
     */
    private double shareFixedValue;
    /**
     * 活跃度
     */
    private double scorePoint;
    /**
     * 推广员战绩分成
     */
    private double promotionShareValue;
    /**
     * 推广员 只显示自己今天的收益的特殊标志
     */
    private boolean specialFlag;

    /**
     * 贡献值
     * 实际报名费 玩家实际出的报名费 存在大赢家情况下不出或者全出
     */
    private double actualEntryFee;
    /**
     * 理论报名费
     */
    private double theoryEntryFee;
    /**
     * 个人预警值
     */
    private Double personalSportsPointWarning;
    /**
     * 推广员预警值
     */
    private Double sportsPointWarning;

    /**
     * 总积分
     */
    private double totalPoint;
    /**
     * 审核状态
     * 0 不显示
     * 1 未审核
     * 2 已审核
     */
    private int examineStatus;
    /**
     * 成员 总积分
     */
    private double playerTotalPoint;
    /**
     * 生存积分
     */
    private Double alivePoint;
    /**
     * 总积分(中至)
     */
    private double zhongZhiTotalPoint;
    /**
     * 个人淘汰分
     */
    private double eliminatePoint;


    public ClubPromotionLevelItem() {
    }

    /**
     * 常用操作
     *
     * @param pid
     * @param myisminister
     * @param name
     * @param iconUrl
     * @param shareType
     * @param shareValue
     * @param shareFixedValue
     * @param personalSportsPointWarning
     * @param sportsPointWarning
     * @param sportsPoint
     */
    public ClubPromotionLevelItem(long pid, int myisminister, String name, String iconUrl, int shareType, double shareValue, double shareFixedValue, Double personalSportsPointWarning, Double sportsPointWarning, double sportsPoint, int level) {
        this.pid = pid;
        this.myisminister = myisminister;
        this.name = name;
        this.iconUrl = iconUrl;
        this.shareType = shareType;
        this.shareValue = shareValue;
        this.shareFixedValue = shareFixedValue;
        this.personalSportsPointWarning = personalSportsPointWarning;
        this.sportsPointWarning = sportsPointWarning;
        this.sportsPoint = sportsPoint;
        this.level = level;
    }

    public ClubPromotionLevelItem(long pid, int myisminister, String name, String iconUrl, int number, double sportsPoint, double sumSportsPoint, int level, Double personalSportsPointWarning, Double sportsPointWarning, int shareType, double shareValue, double shareFixedValue, int examineStatus, Double alivePoint, double eliminatePoint) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.setCount = 0;
        this.winner = 0;
        this.entryFee = 0.0;
        this.consume = 0;
        this.sportsPointConsume = 0.0;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.level = level;
        this.myisminister = myisminister;
        this.shareType = shareType;
        this.shareFixedValue = shareFixedValue;
        this.shareValue = shareValue;
        this.scorePoint = 0.0;
        this.actualEntryFee = 0.0;
        this.theoryEntryFee = 0.0;
        this.personalSportsPointWarning = personalSportsPointWarning;
        this.sportsPointWarning = sportsPointWarning;
        this.totalPoint = this.formatDouble(this.sportsPointConsume + this.scorePoint - actualEntryFee);
        this.examineStatus = examineStatus;
        this.playerTotalPoint = this.formatDouble(this.sportsPointConsume - actualEntryFee);
        this.zhongZhiTotalPoint = this.playerTotalPoint;
        this.alivePoint = alivePoint;
        this.eliminatePoint = eliminatePoint;
    }

    public ClubPromotionLevelItem(long pid, String name, String iconUrl, int number, double sportsPoint, double sumSportsPoint, int level, Double personalSportsPointWarning, Double sportsPointWarning, int shareType, double shareValue, double shareFixedValue, int examineStatus, Double alivePoint, double eliminatePoint) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.setCount = 0;
        this.winner = 0;
        this.entryFee = 0.0;
        this.consume = 0;
        this.sportsPointConsume = 0.0;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.shareType = shareType;
        this.shareFixedValue = shareFixedValue;
        this.shareValue = shareValue;
        this.level = level;
        this.scorePoint = 0.0;
        this.actualEntryFee = 0.0;
        this.theoryEntryFee = 0.0;
        this.personalSportsPointWarning = personalSportsPointWarning;
        this.sportsPointWarning = sportsPointWarning;
        this.totalPoint = this.formatDouble(this.sportsPointConsume + this.scorePoint - actualEntryFee);
        this.examineStatus = examineStatus;
        this.playerTotalPoint = this.formatDouble(this.sportsPointConsume - actualEntryFee);
        this.zhongZhiTotalPoint = this.playerTotalPoint;
        this.alivePoint = alivePoint;
        this.eliminatePoint = eliminatePoint;
    }


    public ClubPromotionLevelItem(long pid, String name, String iconUrl, int number, int setCount, int winner, double entryFee, int consume, double sportsPointConsume, double sportsPoint, double sumSportsPoint, int level, int myisminister) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.setCount = setCount;
        this.winner = winner;
        this.entryFee = entryFee;
        this.consume = consume;
        this.sportsPointConsume = sportsPointConsume;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.level = level;
        this.myisminister = myisminister;
    }

    public ClubPromotionLevelItem(long pid, String name, String iconUrl, int number, int setCount, int winner, double entryFee, int consume, double sportsPointConsume, double sportsPoint, double sumSportsPoint, int level, int myisminister, int shareType, double shareValue, double shareFixedValue) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.setCount = setCount;
        this.winner = winner;
        this.entryFee = entryFee;
        this.consume = consume;
        this.sportsPointConsume = sportsPointConsume;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.level = level;
        this.myisminister = myisminister;
        this.shareType = shareType;
        this.shareFixedValue = shareFixedValue;
        this.shareValue = shareValue;
    }

    public ClubPromotionLevelItem(long pid, String name, String iconUrl, int number, int setCount, int winner, double entryFee, int consume, double sportsPointConsume, double sportsPoint, double sumSportsPoint, int level, int myisminister, int shareType, double shareValue, double shareFixedValue, double scorePoint, double actualEntryFee, double theoryEntryFee, Double personalSportsPointWarning, Double sportsPointWarning, int examineStatus, Double alivePoint, double eliminatePoint) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.setCount = setCount;
        this.winner = winner;
        this.entryFee = entryFee;
        this.consume = consume;
        this.sportsPointConsume = sportsPointConsume;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.level = level;
        this.myisminister = myisminister;
        this.shareType = shareType;
        this.shareFixedValue = shareFixedValue;
        this.shareValue = shareValue;
        this.scorePoint = scorePoint;
        this.actualEntryFee = actualEntryFee;
        this.theoryEntryFee = theoryEntryFee;
        this.personalSportsPointWarning = personalSportsPointWarning;
        this.sportsPointWarning = sportsPointWarning;
        this.totalPoint = this.formatDouble(this.sportsPointConsume + this.scorePoint - actualEntryFee);
        this.examineStatus = examineStatus;
        this.playerTotalPoint = this.formatDouble(this.sportsPointConsume - actualEntryFee);
        this.zhongZhiTotalPoint = this.playerTotalPoint;
        this.alivePoint = alivePoint;
        this.eliminatePoint = eliminatePoint;

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

    public String isSamePidAndSpecial() {
        return String.valueOf(pid) + String.valueOf(specialFlag);
    }

}
