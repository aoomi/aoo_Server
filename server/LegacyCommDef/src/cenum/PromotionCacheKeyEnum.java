package cenum;

/**
 * 推广员缓存key枚举
 */
public enum PromotionCacheKeyEnum {
    /**
     *
     */
    MEMBER_RELATION("RELATIONUID%dANDPUID%d"),
    ;
    private String value;

    PromotionCacheKeyEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
