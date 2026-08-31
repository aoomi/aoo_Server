package jsproto.c2s.cclass.zypk;

/*
 * 自由扑克宏定义
 * @author huaxing
 * **/
public class ZYPK_define {
	//通用按钮类型
	public static enum ZYPK_AnNiu{
		Not(-1),//空操作
		BuPai(0),//补牌
		BuMingPai(1),//补明牌
		OutCard(2), //出牌
		GenZhu(3),//跟注
		YaZhu(4),//压注
		KanPai(5),//看牌
		QiPai(6),//弃牌
		JiaBei(7),//加倍
		MingPai(8),//明牌
		LiPai(9),//理牌
		BiPai(10),//比牌
		;

		private int value;
		private ZYPK_AnNiu(int value) {this.value = value;}
		public int value() {return this.value;}

		public static ZYPK_AnNiu valueOf(int value) {
			for (ZYPK_AnNiu flow : ZYPK_AnNiu.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_AnNiu.Not;
		}
	};
	
	//庄家设置
	public static enum ZYPK_ZhuangSet{
		Not_Zhuang(0),
		Zhuang(1),
		;
		private int value;
		private ZYPK_ZhuangSet(int value) {this.value = value;}
		public int value() {return this.value;}

		public static ZYPK_ZhuangSet valueOf(int value) {
			for (ZYPK_ZhuangSet flow : ZYPK_ZhuangSet.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_ZhuangSet.Not_Zhuang;
		}
	};

	//选庄时间
	public static enum ZYPK_XuanZhuang{
		Not(0),
		FaPai_Q(1),
		FaPai_H(2),
		;
		private int value;
		private ZYPK_XuanZhuang(int value) {this.value = value;}
		public int value() {return this.value;}

		public static ZYPK_XuanZhuang valueOf(int value) {
			for (ZYPK_XuanZhuang flow : ZYPK_XuanZhuang.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_XuanZhuang.Not;
		}
	};

	//控牌设置
	public static enum ZYPK_KongPai{
		FangZhu(0),		//房主
		ZhuangJia(1),	//庄家
		;
		private int value;
		private ZYPK_KongPai(int value) {this.value = value;}
		public int value() {return this.value;}

		public static ZYPK_KongPai valueOf(int value) {
			for (ZYPK_KongPai flow : ZYPK_KongPai.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_KongPai.FangZhu;
		}
	};
	
	
	//操作模式
	public static enum ZYPK_MoShi{
		Luan(0),
		Tong(1),
		;
		private int value;
		private ZYPK_MoShi(int value) {this.value = value;}
		public int value() {return this.value;}

		public static ZYPK_MoShi valueOf(int value) {
			for (ZYPK_MoShi flow : ZYPK_MoShi.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_MoShi.Luan;
		}
	};


	//创建房间后庄家设置
	public static enum ZYPK_Zhuang{
		/**轮庄*/
		Luan(0),
		/**随机*/
		SuiJi(1),
		/**抢*/
		Qiang(2),
		/**固定*/
		GuDing(3),
		;
		private int value;
		private ZYPK_Zhuang(int value) {this.value = value;}
		public int value() {return this.value;}
		public static ZYPK_Zhuang valueOf(int value) {
			for (ZYPK_Zhuang flow : ZYPK_Zhuang.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_Zhuang.Luan;
		}
	};

	//抢庄状态
	public static enum ZYPK_QiangZhuang{
		/**开始_抢*/
		Strat_Qiang(0),
		/**抢*/
		Qiang(1),
		;
		private int value;
		private ZYPK_QiangZhuang(int value) {this.value = value;}
		public int value() {return this.value;}
		public static ZYPK_QiangZhuang valueOf(int value) {
			for (ZYPK_QiangZhuang flow : ZYPK_QiangZhuang.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return ZYPK_QiangZhuang.Strat_Qiang;
		}
	};

	//控牌动作
	public static enum Op_KongPai{
		/**发牌*/
		FaPai(0),
		/**洗牌*/
		XiPai(1),
		/**收牌*/
		ShouPai(2),
		;
		private int value;
		private Op_KongPai(int value) {this.value = value;}
		public int value() {return this.value;}

		public static Op_KongPai valueOf(int value) {
			for (Op_KongPai flow : Op_KongPai.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return Op_KongPai.FaPai;
		}
	};
	
	//控牌动作
	public static enum Op_Player{
		/**操作*/
		Op(0),
		/**过*/
		Pass(1),
		/**回退*/
		Back(2),
		;
		private int value;
		private Op_Player(int value) {this.value = value;}
		public int value() {return this.value;}

		public static Op_Player valueOf(int value) {
			for (Op_Player flow : Op_Player.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return Op_Player.Op;
		}
	};

	//看牌状态
	public static enum Op_KanPai{
		/**看*/
		Kan(0),
		/**不看*/
		Not_Kan(1),
		/**明*/
		Ming(2),
		;
		private int value;
		private Op_KanPai(int value) {this.value = value;}
		public int value() {return this.value;}

		public static Op_KanPai valueOf(int value) {
			for (Op_KanPai flow : Op_KanPai.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return Op_KanPai.Kan;
		}
	};

	
}
