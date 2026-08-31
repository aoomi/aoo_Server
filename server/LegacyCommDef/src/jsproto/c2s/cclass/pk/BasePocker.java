package jsproto.c2s.cclass.pk;

/*
 * 纸牌
 * */
public class BasePocker {
    public static enum PockerColorType {
        POCKER_COLOR_TYPE_DIAMOND(0),        //方块
        POCKER_COLOR_TYPE_CLUB(1),        //梅花
        POCKER_COLOR_TYPE_SPADE(2),        //红桃
        POCKER_COLOR_TYPE_HEART(3),        //黑桃
        POCKER_COLOR_TYPE_TRUMP(4),        //大小王
        ;
        private int value;

        private PockerColorType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static PockerColorType valueOf(int value) {
            for (PockerColorType flow : PockerColorType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return PockerColorType.POCKER_COLOR_TYPE_DIAMOND;
        }

        public static PockerColorType getOpType(String value) {
            String gameTypyName = value.toUpperCase();
            for (PockerColorType flow : PockerColorType.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return PockerColorType.POCKER_COLOR_TYPE_DIAMOND;
        }
    }

    public static enum PockerValueType {
        POCKER_VALUE_TYPE_SINGLE(0),            //单张
        POCKER_VALUE_TYPE_SUB(1),                //对子
        POCKER_VALUE_TYPE_THREE(2),                //三张一样(三条)
        POCKER_VALUE_TYPE_BOMB(3),                //四张一样 （炸弹）
        POCKER_VALUE_TYPE_FLUSH(4),                //同花
        POCKER_VALUE_TYPE_STRAIGHT_FLUSH(5),    //同花顺
        POCKER_VALUE_TYPE_SHUN_ZI(6),            //顺子

        ;
        private int value;

        private PockerValueType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static PockerValueType valueOf(int value) {
            for (PockerValueType flow : PockerValueType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return PockerValueType.POCKER_VALUE_TYPE_SUB;
        }

        public static PockerValueType getOpType(String value) {
            String gameTypyName = value.toUpperCase();
            for (PockerValueType flow : PockerValueType.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return PockerValueType.POCKER_VALUE_TYPE_SUB;
        }
    }

    public static enum PockerListType {
        POCKERLISTTYPE_AFIRST,   //a在前
        POCKERLISTTYPE_AEND,   //a在后
        POCKERLISTTYPE_TWOEND,   //2在后
    }

    //扑克牌(一副) a 最小
    public static Integer[] PockerList_AFirst = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, //方块A~K
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, //梅花A~K
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, //红桃A~K
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, //黑桃A~K
    };

    //扑克牌(一副) a 最大
    public static Integer[] PockerList_AEnd = {
            0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,//方块2~A
            0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E,//梅花2~A
            0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E,//红桃2~A
            0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E,//黑桃2~A
    };

    //扑克牌(一副) 2 最大
    public static Integer[] PockerList_TWOEnd = {
            0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, //方块3~2
            0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, //梅花3~2
            0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, //红桃3~2
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, //黑桃3~2
    };

    //扑克牌(一副)
    public static Integer[] Trump_PockerList = {
            0x41, 0x42 //大小王
    };

    //牌值掩码
    public static final int LOGIC_MASK_VALUE = 0x0F;

    //花色掩码
    public static final int LOGIC_MASK_COLOR = 0xF0;

    //牌数
    public static final int MAX_NORMAL_POCKER = 52;

    //单色牌数
    public static final int ONE_COLOR_POCKER_COUNT = 13;

    //王牌数
    public static final int MAX_TRUMP_POCKER = 2;

    //顺子最少牌数
    public static final int MIN_FLUSH_COUNT = 5;

    // 花色掩码 % 80
    public static final int LOGIC_MASK_COLOR_MOD = 80;

    //获取牌值
    public static int getCardValue(int card) {
        int value = card & LOGIC_MASK_VALUE;
        return value;
    }

    public static int getCardValueEx(int card) {
        int value = card & LOGIC_MASK_VALUE;
        if (PockerColorType.POCKER_COLOR_TYPE_TRUMP.equals(PockerColorType.valueOf(getCardColor(card)))) {
            value += 0x10;
        }
        return value;
    }

    /**
     * 大小王为1组牌17
     *
     * @param card
     * @return
     */
    public static int getCardValueExx(int card) {
        int cardValueEx = getCardValueEx(card);
        return cardValueEx == 18 || cardValueEx == 17 ? 18 : cardValueEx;
    }

    //获取掩码
    public static int getCardColor(int card) {
        int color = card & LOGIC_MASK_COLOR;
        while (color >= LOGIC_MASK_COLOR_MOD) {
            color -= LOGIC_MASK_COLOR_MOD;
        }
        return color / 0x10;
    }
}
