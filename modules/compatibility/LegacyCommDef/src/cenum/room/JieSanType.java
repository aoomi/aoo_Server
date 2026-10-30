package cenum.room;
/**
 * 解散类型
 * @author Administrator
 *
 */

/**
 * -1发起解散 0默认 1:同意解散 2:超时解散（阿泽说不用发起解散）
 */
public enum JieSanType {
	FaQiJieSan(-1),
	MoRen(0),
	TongYi(1),
	ChaoShiTongYi(2),
	;
	private int value;
	JieSanType(int value){
		this.value=value;
	}
	public static JieSanType valueOf(int value) {
		for (JieSanType flow : JieSanType.values()) {
			if (flow.ordinal() == value) {
				return flow;
			}
		}
		return JieSanType.MoRen;
	}
	public int value() {
		return this.value;
	}
};