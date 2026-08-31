package business.global.club;

import lombok.Data;

/**
 *
 */
@Data
public class ClubPromotionLevetShareChangeBatchItem {
    /**
     * 亲友圈成员-被操作者
     */
    private ClubMember toClubMember;
    /**
     * 不能超过值
     */
    private double maxValue;
    /**
     * 不能小于值
     */
    private double minValue;
    /**
     * 上级成员Pid
     */
    private long upLevelPid;

    /**
     * 当前操作的类型类型
     */
    private int doType;

    /**
     * 操作的分成类型
     */
    private int toType;

    public ClubPromotionLevetShareChangeBatchItem(ClubMember toClubMember, double maxValue, double minValue, long upLevelPid, int doType, int toType) {
        this.toClubMember = toClubMember;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.upLevelPid = upLevelPid;
        this.doType = doType;
        this.toType = toType;
    }
}
