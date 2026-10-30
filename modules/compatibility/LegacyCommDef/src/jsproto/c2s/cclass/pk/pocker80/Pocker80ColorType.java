package jsproto.c2s.cclass.pk.pocker80;

public enum Pocker80ColorType {
		POCKER_COLOR_TYPE_DIAMOND(0), 		//方块
		POCKER_COLOR_TYPE_CLUB(1), 		//梅花
		POCKER_COLOR_TYPE_SPADE(2), 		//红桃
		POCKER_COLOR_TYPE_HEART(3), 		//黑桃
		POCKER_COLOR_TYPE_TRUMP(4), 		//大小王
		;
		private int value;
		private Pocker80ColorType(int value) {this.value = value;}
		public int value() {return this.value;}
		public static Pocker80ColorType valueOf(int value) {
			for (Pocker80ColorType flow : Pocker80ColorType.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return Pocker80ColorType.POCKER_COLOR_TYPE_DIAMOND;
		}

		public static Pocker80ColorType getOpType(String value) {
			String gameTypyName = value.toUpperCase();
			for (Pocker80ColorType flow : Pocker80ColorType.values()) {
				if (flow.toString().equals(gameTypyName)) {
					return flow;
				}
			}
			return Pocker80ColorType.POCKER_COLOR_TYPE_DIAMOND;
		}
	}



