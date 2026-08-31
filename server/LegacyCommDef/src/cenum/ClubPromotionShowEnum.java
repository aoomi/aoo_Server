package cenum;

/**
 * 推广员显示列表
 *
 * @author Administrator
 */
public enum ClubPromotionShowEnum {

    /**
     * 局数
     */
    Game_Count(0),
    /**
     * 报名费
     */
    Rank_Cost(1),
    /**
     * 贡献值
     */
    Contribution_Value(2),
    /**
     * 分成
     */
    Share_Value(3),
    /**
     * 活跃度
     */
    Activity_Value(4),
    /**
     * 消耗钻石
     */
    Diamond_Cost(5),
    /**
     * 输赢比赛分
     */
    SportsPoint_WinOrLose(6),
    /**
     * 个人比赛分
     */
    SportsPoint_Personal(7),
    /**
     * 总比赛分
     */
    SportsPoint_Total(8),
    /**
     * 推广员预警值
     */
    SportsPointWarn_Promotion(9),
    /**
     * 个人预警值
     */
    SportsPointWarn_Personal(10),
    /**
     * 总积分
     */
    Total_Point(11),
    /**
     * 成员总积分
     */
    PlayerTotal_Point(12),
    /**
     * 生存积分
     */
    Alive_Point(13),
    /**
     * 总积分
     */
    ZhongZhiTotal_Point(14),
    ;

    private int value;

    ClubPromotionShowEnum(int value) {
        this.value = value;
    }

    // 获取值
    public int value() {
        return this.value;
    }

    public static ClubPromotionShowEnum valueOf(int value) {
        for (ClubPromotionShowEnum flow : ClubPromotionShowEnum.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ClubPromotionShowEnum.Game_Count;
    }
}
