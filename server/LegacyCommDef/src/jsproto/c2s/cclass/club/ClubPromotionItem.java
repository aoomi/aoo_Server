package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPromotionItem {
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
     * 活跃计算
     */
    private double calcActiveValue;
    /**
     * 当前活跃
     */
    private double curActiveValue;

    /**
     * 推广员状态(0不是推广员,1任命,2卸任)
     */
    private int promotion;
    /**
     * 代理分成百分比值
     */

    private double shareValue;

    /**
     * 代理分成固定值
     */
    private double shareFixedValue;
    /**
     * 代理分成类型
     */
    private int shareType;
    /**
     * 推广员 操作的人员 身上的百分比  不能超过这个值
     */
    private double doShareValue;
    /**
     * 最小的值
     */
    private double minShareValue;

    /**
     * 推广员 操作的人员 身上的固定值  不能超过这个值
     */
    private double doShareFixedValue;
    /**
     * 最小的固定值
     */
    private double minShareFixedValue;

    /**
     * 上级是否创建者
     */
    private boolean isUpLevelCreate;

    /**
     * 下级类型
     */
    private int lowerLevelShareType;

    /**
     * 上级
     */
    private long upLevelId;

    /**
     * 上级类型
     */
    private int upLevelShareType;

    public ClubPromotionItem(long pid, String name, String iconUrl, double calcActiveValue, double curActiveValue, int promotion) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.calcActiveValue = calcActiveValue;
        this.curActiveValue = curActiveValue;
        this.promotion = promotion;
    }

    public ClubPromotionItem(long pid, String name, double shareValue, double shareFixedValue, int shareType) {
        this.pid = pid;
        this.name = name;
        this.shareValue = shareValue;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
    }

    public ClubPromotionItem(long pid, String name, String iconUrl, int number, double calcActiveValuef, double curActiveValue, int promotion) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.number = number;
        this.calcActiveValue = calcActiveValuef;
        this.curActiveValue = curActiveValue;
        this.promotion = promotion;
    }

    public ClubPromotionItem(long pid, double shareValue, double shareFixedValue, int shareType, double doShareValue) {
        this.pid = pid;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
        this.shareValue = shareValue;
        this.doShareValue = doShareValue;
    }

    public ClubPromotionItem(long pid, double shareValue, double shareFixedValue, int shareType, double doShareValue, double minShareValue, double doShareFixedValue, double minShareFixedValue, boolean isUpLevelCreate, int lowerLevelShareType, long upLevelId, int upLevelShareType) {
        this.pid = pid;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
        this.shareValue = shareValue;

        this.doShareValue = doShareValue;
        this.minShareValue = minShareValue;

        this.doShareFixedValue = doShareFixedValue;
        this.minShareFixedValue = minShareFixedValue;
        this.isUpLevelCreate = isUpLevelCreate;
        this.lowerLevelShareType = lowerLevelShareType;
        this.upLevelId = upLevelId;
        this.upLevelShareType = upLevelShareType;
    }
}
