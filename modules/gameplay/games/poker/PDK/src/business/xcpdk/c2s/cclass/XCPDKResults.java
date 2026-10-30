package business.xcpdk.c2s.cclass;

import jsproto.c2s.cclass.room.AbsBaseResults;

/**
 * 红中麻将总结算信息
 * 
 * @author Huaxing
 *
 */
public class XCPDKResults extends AbsBaseResults {
	@SuppressWarnings("unused")
	private int shangHuoPoint;// 上火次数
	@SuppressWarnings("unused")
	private int piaoFenPoint;// 飘分次数
	@SuppressWarnings("unused")
	private int anGangPoint; // 次数
	@SuppressWarnings("unused")
	private int gangPoint; // 次数

	public void addAnGangPoint(int anGangPoint) {
		this.anGangPoint += anGangPoint;
	}

	public void addGangPoint(int gangPoint) {
		this.gangPoint += gangPoint;
	}

	/**
	 * @param shangHuoPoint
	 *            要设置的 shangHuoPoint
	 */
	public void addShangHuoPoint(int shangHuoPoint) {
		this.shangHuoPoint += shangHuoPoint;
	}

	/**
	 * @param piaoFenPoint
	 *            要设置的 piaoFenPoint
	 */
	public void addPiaoFenPoint(int piaoFenPoint) {
		this.piaoFenPoint += piaoFenPoint;
	}
}
