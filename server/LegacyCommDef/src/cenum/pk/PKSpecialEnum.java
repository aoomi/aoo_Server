package cenum.pk;

/**
 * 麻将特殊枚举
 *
 * @author Administrator
 */
public enum PKSpecialEnum {
    NOT(0),
    /**
     * 13张牌
     */
    SIZE_13(13),
    /**
     * 12张牌
     */
    SIZE_12(12),
    /**
     * 15张牌
     */
    SIZE_15(15),
    /**
     * 17张牌
     */
    SIZE_17(17),
    /**
     * 20张牌
     */
    SIZE_20(20),
    /**
     * 26张牌
     */
    SIZE_26(26),
    /**
     * 27张牌
     */
    SIZE_27(27),
    /**
     * 33张牌
     */
    SIZE_33(33),
    /**
     * 34张牌
     */
    SIZE_34(34),
    /**
     * 34张牌
     */
    SIZE_35(35),
    /**
     * 36张牌
     */
    SIZE_36(36),
    /**
     * 39张牌
     */
    SIZE_39(39),
    /**
     * 54张牌
     */
    SIZE_54(54),
    /**
     * 66张牌
     */
    SIZE_66(66),
    /**
     * 67张牌
     */
    SIZE_67(67),
    /**
     * 68张牌
     */
    SIZE_68(68),
    /**
     * 69张牌
     */
    SIZE_69(69),
    /**
     * 70张牌
     */
    SIZE_70(70),
    /**
     * 70张牌
     */
    SIZE_71(71),

    /**
     * 78张牌
     */
    SIZE_78(78),
    /**
     * 104张牌
     */
    SIZE_104(104),
    /**
     * 130张牌
     */
    SIZE_130(130),
    /**
     * 2最大时2的牌值
     */
    MAX_CARD_2_VALUE(0x0F),
    /**
     * 鬼牌的颜色值
     */
    TRUMP_COLOR(0x04),

    /**
     * A最大是A的牌值
     */
    MAX_CARD_A_VALUE(0x0E),

    /**
     * A最小是A的牌值
     */
    MIN_CARD_A_VALUE(0x01),

    /**
     * K最大是K的牌值
     */
    MAX_CARD_K_VALUE(0x0D),
    /**
     * 最大的王
     */
    MAX_KING(18),
    /**
     * 最小的王
     */
    MIN_KING(17),
    /**
     * 2张牌
     */
    SIZE_2(2),
    //三打哈
    /**
     * 25张牌
     */
    SIZE_25(25),
    /**
     * 21张牌
     */
    SIZE_21(21),
    /**
     * 19张牌
     */
    SIZE_19(19),
    /**
     * 28张
     */
    SIZE_28(28),
    /**
     * 40
     */
    SIZE_40(40),
    /**
     * 5
     */
    SIZE_5(5),
    /**
     * 8
     */
    SIZE_8(8),
    ;

    private int value;

    private PKSpecialEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
