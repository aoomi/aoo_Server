package jsproto.c2s.cclass.zjh;

/*
 * 炸金花宏定义
 * @author zaf
 * **/
public class ZJH_define {

	public static enum ZJH_OpType {
		GenZhu(1),//跟注
		JiaZhu(2),//加注
		BiPai(3),//比牌
		KanPai(4),//看牌
		QiPai(5),//弃牌
		QuanYa(6),//全压
		;
		private int value;
		private ZJH_OpType(int value) {this.value = value;}
		public int value() {return this.value;}
		public static ZJH_OpType valueOf(int value) {
			for (ZJH_OpType flow : ZJH_OpType.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZJH_OpType.QiPai;
		}

		public static ZJH_OpType getOpType(String value) {
			String gameTypyName = value.toUpperCase();
			for (ZJH_OpType flow : ZJH_OpType.values()) {
				if (flow.toString().equals(gameTypyName)) {
					return flow;
				}
			}
			return ZJH_OpType.QiPai;
		}
	};

	public static enum ZJH_StatusType {
		Normal(0x1),//默认状态
		GiveUp(0x2),//弃牌
		ComporeLose(0x4),//比牌输
		ComporeWin(0x8),//比牌赢
		KaiPai(0x10),//看牌
		WinLoseGiveUp(0x11),//(status&WinLoseGiveUp) 结果在 再去 |上新值能保证 弃牌 输赢 维一
		QuanYa(0x20),//全压
		OpenCard(0x40),//看牌
		;
		private int value;
		private ZJH_StatusType(int value) {this.value = value;}
		public int value() {return this.value;}
		public static ZJH_StatusType valueOf(int value) {
			for (ZJH_StatusType flow : ZJH_StatusType.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZJH_StatusType.Normal;
		}

		public static ZJH_StatusType getOpType(String value) {
			String gameTypyName = value.toUpperCase();
			for (ZJH_StatusType flow : ZJH_StatusType.values()) {
				if (flow.toString().equals(gameTypyName)) {
					return flow;
				}
			}
			return ZJH_StatusType.Normal;
		}
	};

}
