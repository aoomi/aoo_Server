package jsproto.c2s.cclass.pk.pocker80;

public enum Pocker80ValueType {
		POCKER_VALUE_TYPE_SINGLE(0),			//单张
		POCKER_VALUE_TYPE_SUB(1),				//对子
		POCKER_VALUE_TYPE_THREE(2),				//三张一样(三条)
		POCKER_VALUE_TYPE_BOMB(3),				//四张一样 （炸弹）
		POCKER_VALUE_TYPE_FLUSH(4),				//同花
		POCKER_VALUE_TYPE_STRAIGHT_FLUSH(5),	//同花顺
		POCKER_VALUE_TYPE_SHUN_ZI(6),			//顺子

		;
		private int value;
		private Pocker80ValueType(int value) {this.value = value;}
		public int value() {return this.value;}
		public static Pocker80ValueType valueOf(int value) {
			for (Pocker80ValueType flow : Pocker80ValueType.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return Pocker80ValueType.POCKER_VALUE_TYPE_SUB;
		}

		public static Pocker80ValueType getOpType(String value) {
			String gameTypyName = value.toUpperCase();
			for (Pocker80ValueType flow : Pocker80ValueType.values()) {
				if (flow.toString().equals(gameTypyName)) {
					return flow;
				}
			}
			return Pocker80ValueType.POCKER_VALUE_TYPE_SUB;
		}
	}