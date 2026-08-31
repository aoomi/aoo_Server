package jsproto.c2s.cclass.club;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 亲友圈推广员项
 * 简单信息
 */
@Data
public class ClubPromotionLevelItemMin {
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
     * 推广员 只显示自己今天的收益的特殊标志
     */
    private boolean specialFlag;


    /**
     *个人预警值
     */
    private Double personalSportsPointWarning;
    /**
     *  推广员预警值
     */
    private  Double sportsPointWarning;


    /**
     * 审核状态
     * 0 不显示
     * 1 未审核
     * 2 已审核
     */
    private int examineStatus;

    /**
     * 生存积分
     */
    private Double alivePoint;

    /**
     * 个人淘汰分
     */
    private double eliminatePoint;

    /**
     * 是否为推广员管理 0不是 1是
     */
    private int isPromotionManage;

    public ClubPromotionLevelItemMin() {
    }


    public ClubPromotionLevelItemMin(long pid, String name, String iconUrl, int number,   double sportsPoint, double sumSportsPoint, int level, int myisminister,
                                     int shareType, double shareValue, double shareFixedValue, Double personalSportsPointWarning, Double sportsPointWarning,
                                     int examineStatus, Double alivePoint, double eliminatePoint, int  isPromotionManage) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.sportsPoint = sportsPoint;
        this.sumSportsPoint = sumSportsPoint;
        this.level = level;
        this.myisminister = myisminister;
        this.shareType = shareType;
        this.shareFixedValue = shareFixedValue;
        this.shareValue = shareValue;
        this.personalSportsPointWarning=personalSportsPointWarning;
        this.sportsPointWarning=sportsPointWarning;
        this.examineStatus=examineStatus;
        this.alivePoint=alivePoint;
        this.eliminatePoint=eliminatePoint;
        this.isPromotionManage=isPromotionManage;

    }




    public static String getItemsName() {
        return "sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as promotionShareValue,sum(roomSportsPointConsume) as actualEntryFee";
    }

    public static String getItemsNameMaxId() {
        return "max(id) as maxId,sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as promotionShareValue,sum(roomSportsPointConsume) as actualEntryFee";
    }

    public double formatDouble(double value){
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public String isSamePidAndSpecial(){
            return String.valueOf(pid)+String.valueOf(specialFlag);
    }

}
