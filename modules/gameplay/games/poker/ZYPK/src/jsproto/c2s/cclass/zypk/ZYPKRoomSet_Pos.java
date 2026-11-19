package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KanPai;

/**
 * 一局中每个位置信息
 * 
 * @author zaf
 *
 */
public class ZYPKRoomSet_Pos {

	private int posID = 0; // 座号ID
	private long pid = 0; // 账号
	private int point; // 积分
	private int beiShu = 0; // 加倍
	private int yaZhu = 0; // 加注
	private ArrayList<Integer> privateCards = new ArrayList<>(); // 私有牌
	private ArrayList<Integer> outCards = new ArrayList<Integer>(); // 打出牌
	private ArrayList<Integer> surplusCardList = new ArrayList<Integer>(); // 剩余牌数
	private Op_KanPai oPpai = Op_KanPai.Kan;
	private boolean isQiPai = false;//是否弃牌
	private boolean isPlayTheGame = true;
	
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
	public int getPoint() {
		return point;
	}
	public void setPoint(int point) {
		this.point = point;
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
	public ArrayList<Integer> getSurplusCardList() {
		return surplusCardList;
	}
	public void setSurplusCardList(ArrayList<Integer> surplusCardList) {
		this.surplusCardList = surplusCardList;
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


	
	
}
