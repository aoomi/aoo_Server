package jsproto.c2s.cclass.pk.pocker256;

public enum Pocker256ColorType {
        POCKER_COLOR_TYPE_DIAMOND(0), 		//方块
        POCKER_COLOR_TYPE_CLUB(1), 		//梅花
        POCKER_COLOR_TYPE_SPADE(2), 		//红桃
        POCKER_COLOR_TYPE_HEART(3), 		//黑桃
        POCKER_COLOR_TYPE_TRUMP(4), 		//大小王
        POCKER_COLOR_TYPE_LaiZi(5), 		//癞子
        ;
        private int value;
        private Pocker256ColorType(int value) {this.value = value;}
        public int value() {return this.value;}
        public static Pocker256ColorType valueOf(int value) {
            for (Pocker256ColorType flow : Pocker256ColorType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Pocker256ColorType.POCKER_COLOR_TYPE_DIAMOND;
        }

        public static Pocker256ColorType getOpType(String value) {
            String gameTypyName = value.toUpperCase();
            for (Pocker256ColorType flow : Pocker256ColorType.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Pocker256ColorType.POCKER_COLOR_TYPE_DIAMOND;
        }
    }