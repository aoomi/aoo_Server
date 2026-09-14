package cenum.mj;

/**
 * 麻将特殊枚举
 *
 * @author Administrator
 */
public enum MJSpecialEnum {
    NOT(0),
    // 风牌
    FENG(40),
    // 红中
    ZHONG(45),
    // 发财
    FA(46),
    // 白板
    BAIBAN(47),
    // 空金
    NOT_JIN(60),
    // 19张牌
    SIZE_19(19),
    // 空牌
    NOT_CARD(70),
    // 空花
    NOT_HUA(50),
    // 听牌数
    TING(34),
    // 16张牌
    SIZE_16(16),
    // 13张牌
    SIZE_13(13),
    // 总共牌144
    TOTAL_CARD(144),
    // 骰子
    DICE(2),
    // 神开启
    GOD_CARD(1),
    // 牌ID
    CARD_ID(1000),
    // 一条龙长度 9
    LONG_SIZE(9),
    ;
    private int value;

    private MJSpecialEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
