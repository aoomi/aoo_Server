package cenum;

/**
 * 推广员二级显示列表
 *
 * @author Administrator
 */
public enum ClubPromotionSecondShowEnum {

    /**
     * 我的分成
     */
    WoDeFenCheng(0),
    /**
     * 推广员预警值
     */
    PromotionWarn(1),
    /**
     * 个人预警值
     */
    PersonalWarn(2),


    ;

    private int value;

    ClubPromotionSecondShowEnum(int value) {
        this.value = value;
    }

    // 获取值
    public int value() {
        return this.value;
    }

    public static ClubPromotionSecondShowEnum valueOf(int value) {
        for (ClubPromotionSecondShowEnum flow : ClubPromotionSecondShowEnum.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ClubPromotionSecondShowEnum.WoDeFenCheng;
    }
}
