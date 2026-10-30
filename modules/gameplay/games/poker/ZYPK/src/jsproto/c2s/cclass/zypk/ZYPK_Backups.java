package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KanPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;

public class ZYPK_Backups {
	private int posID = 0; // 0-3->1-4号位置
	private long pid = 0;// 玩家ID
	private ArrayList<Integer> privateCards = new ArrayList<>(); // 私有牌
	private ArrayList<Integer> outCards = new ArrayList<Integer>(); // 打出牌
	private ArrayList<Integer> mingBuCards = new ArrayList<Integer>(); //明牌补牌
	private int beiShu = 0;	 //加倍
	private int yaZhu = 0;	 //加注
	private Op_KanPai oPpai = Op_KanPai.Kan;
	private boolean isQiPai = false;//是否弃牌
	private List<Long> kanPaiList = new ArrayList<Long>();
	private ZYPK_AnNiu anNiu = ZYPK_AnNiu.Not;
	private Integer cardByte;
	public int getPosID() {
		return posID;
	}
	public void setPosID(int posID) {
		this.posID = posID;
	}
	public long getPid() {
		return pid;
	}
	public void setPid(long pid) {
		this.pid = pid;
	}
	public ArrayList<Integer> getPrivateCards() {
		return privateCards;
	}
	public void setPrivateCards(ArrayList<Integer> privateCards) {
		this.privateCards = privateCards;
	}
	public ArrayList<Integer> getOutCards() {
		return outCards;
	}
	public void setOutCards(ArrayList<Integer> outCards) {
		this.outCards = outCards;
	}
	public ArrayList<Integer> getMingBuCards() {
		return mingBuCards;
	}
	public void setMingBuCards(ArrayList<Integer> mingBuCards) {
		this.mingBuCards = mingBuCards;
	}
	public int getBeiShu() {
		return beiShu;
	}
	public void setBeiShu(int beiShu) {
		this.beiShu = beiShu;
	}
	public int getYaZhu() {
		return yaZhu;
	}
	public void setYaZhu(int yaZhu) {
		this.yaZhu = yaZhu;
	}
	public Op_KanPai getoPpai() {
		return oPpai;
	}
	public void setoPpai(Op_KanPai oPpai) {
		this.oPpai = oPpai;
	}
	public boolean isQiPai() {
		return isQiPai;
	}
	public void setQiPai(boolean isQiPai) {
		this.isQiPai = isQiPai;
	}
	public List<Long> getKanPaiList() {
		return kanPaiList;
	}
	public void setKanPaiList(List<Long> kanPaiList) {
		this.kanPaiList = kanPaiList;
	}
	public ZYPK_AnNiu getAnNiu() {
		return anNiu;
	}
	public void setAnNiu(ZYPK_AnNiu anNiu) {
		this.anNiu = anNiu;
	}
	
	/**
	 * 获取牌
	 * @return
	 */
	public Integer getCardBype() {
		if (ZYPK_AnNiu.BuPai.equals(this.anNiu) || ZYPK_AnNiu.BuMingPai.equals(this.anNiu)) {
			return this.cardByte;
		}
		return null;
	}
	
	/**
	 * 添加牌
	 * @param cardByte
	 */
	public void addCardBype (Integer cardByte) {
		if (ZYPK_AnNiu.BuPai.equals(this.anNiu) || ZYPK_AnNiu.BuMingPai.equals(this.anNiu)) {
			this.cardByte = cardByte;
		}
	}
}
