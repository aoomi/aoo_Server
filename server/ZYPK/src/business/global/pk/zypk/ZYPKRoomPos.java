package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.zypk.ZYPKSet_Pos;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KanPai;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;

public class ZYPKRoomPos extends AbsRoomPos {
	private ArrayList<Integer> privateCards = new ArrayList<>(); // 私有牌
	private ArrayList<Integer> outCards = new ArrayList<Integer>(); // 打出牌
	private ArrayList<Integer> mingBuCards = new ArrayList<Integer>(); //明牌补牌
	private int beiShu = 0;	 //加倍
	private int yaZhu = 0;	 //加注
	private Op_KanPai oPpai = Op_KanPai.Kan;
	private boolean isQiPai = false;//是否弃牌
	private List<Long> kanPaiList = new ArrayList<Long>();
	public ZYPKRoomPos(int posID, AbsBaseRoom room){
		super(posID, room);
	}
	
	/**
	 * 清除位置
	 */
	public void cleanRoomPos () {
		this.privateCards.clear();
		this.outCards.clear();
		this.mingBuCards.clear();
		this.beiShu = 0;
		this.yaZhu = 0;
		this.oPpai =  Op_KanPai.Kan;
		this.isQiPai = false;
		this.kanPaiList.clear();
	}
	
	
	/**
	 * 清除位置
	 */
	public void shouRoomPos (boolean isKan) {
		this.privateCards.clear();
		this.outCards.clear();
		this.mingBuCards.clear();
		this.beiShu = 0;
		this.yaZhu = 0;
		
		//按钮选择看牌
		if (isKan) {
			//都不看
			this.oPpai = Op_KanPai.Not_Kan;
		} else {
			//正常
			this.oPpai = Op_KanPai.Kan;
		}
		this.isQiPai = false;
		this.kanPaiList.clear();
	}

	
	/**
	 * 加倍
	 * 倍数叠加
	 * 洗牌，收牌-清空
	 * @return
	 */
	public int addJiaBei (int jiaBei) {
		this.beiShu += jiaBei;
		return this.beiShu;
	}
	
	/**
	 * 加注
	 * 加注叠加
	 * 洗牌，收牌-清空
	 * @return
	 */
	public void addJiaZhu (int jiaZhu) {
		this.yaZhu += jiaZhu;
	}
	
	
	
	/**
	 * 增加筹码
	 * @param chouMa
	 */
	public void addchouMa (int chouMa) {
		this.setPoint(this.getPoint() + chouMa);
	}
	
	/**
	 * 设置筹码
	 * @param chouMa
	 */
	public void setChouMa (int chouMa) {
		this.setPoint(chouMa);
	}
	
	/**
	 * 添加明牌的私有牌
	 * @param cardByte
	 */
	public void addMingPrivateCard (Integer cardByte) {
		this.privateCards.add(cardByte);
		this.mingBuCards.add(cardByte);
	}
	
	/**
	 * 弃牌
	 */
	public void qiPai() {
		this.isQiPai = true;
	}
	
	/**
	 * 获取弃牌状态
	 * @return
	 */
	public boolean getQiPai () {
		return this.isQiPai;
	}
	
	/**
	 * 获取倍数
	 * @return
	 */
	public int getBeiShu() {
		return beiShu;
	}

	/**
	 * 获取看牌状态
	 * @return
	 */
	public Op_KanPai getoPpai() {
		return oPpai;
	}

	/**
	 * 获取押注
	 * @return
	 */
	public int getYaZhu() {
		return yaZhu;
	}

	/**
	 * 添加看牌成员
	 * @param pid
	 */
	public void addKanPai (Long pid) {
		if (!this.kanPaiList.contains(pid)) {
			this.kanPaiList.add(pid);
		}
	}

	/**
	 * 初始化手牌
	 * @param cards
	 */
	public void init(List<Integer> cards) {
		this.privateCards = new ArrayList<>(cards);
	}
	
	/**
	 * 初始化手牌
	 * @param cards
	 */
	public void addCard(Integer card) {
		this.privateCards.add(card);
	}
	
	/**
	 * 看牌
	 * @param oPpai
	 */
	public void setoPpai(Op_KanPai oPpai) {
		this.oPpai = oPpai;
	}

	
	public boolean removeGive (int point) {
		if (point > this.getPoint()) {
			return false;
		}
		this.setPoint(this.getPoint() - point);
		return true;
	}
	
	public void getGive (int point ) {
		this.setPoint(this.getPoint() + point);
	}

	public ZYPKSet_Pos getNotifyCard(long pid){
		ZYPKSet_Pos setPos = null;
		switch (this.oPpai) {
		case Ming:
			setPos = getNotifyCard(pid,Op_KanPai.Ming.equals(this.oPpai));
			break;
		case Not_Kan:
			setPos = getNotifyCard(0,false);
			break;
		case Kan:
			setPos = getNotifyCard(pid,false);
			break;
		default:
			break;
		}
		return setPos;
	}

	/**
	 * 获取牌组信息
	 * @return
	 */
	public ZYPKSet_Pos getNotifyCard(long pid, boolean isOpenCard) {
		boolean isSelf = pid == this.getPid();
		if (isOpenCard) {
			isSelf = true;
		} else if (this.kanPaiList.contains(pid)) {
			isSelf = true;
		}
		ZYPKSet_Pos zSetPos = new ZYPKSet_Pos();
		ArrayList<Integer> sArrayList = new ArrayList<Integer>();
		// 是自己
		int length = privateCards.size();
		for (int i = 0; i < length; i++) {
			Integer cardByte = this.privateCards.get(i);
			if (this.mingBuCards.contains(cardByte)) {
				sArrayList.add(cardByte);
			} else {
				sArrayList.add(isSelf ? cardByte : 0x00);
			}
		}
		zSetPos.privateCards = sArrayList;
		zSetPos.outCards = this.outCards;
		return zSetPos;
	}
	
	/**
	 * 清空所有牌
	 */
	public void cleanAllCard () {
		this.privateCards.clear();
		this.outCards.clear();
	}
	
	/**
	 * 删除牌组信息
	 * @return
	 */
	public boolean deleteCard(ArrayList<Integer> cradList) {
		for (Integer byte1 : cradList) {
			boolean flag =  this.privateCards.remove((Integer)byte1);
			if (!flag) {
				return false;
			}
			this.outCards.add(byte1);
		}
		return true;
	}
	
	//获取牌的位置
	public boolean checkCard(Integer card){
		return BasePockerLogic.getCardCount(privateCards, card, false) > 0;
	}
	
	//获取牌的位置
	public boolean checkCardHasFocus(Integer card){
		return BasePockerLogic.getCardCount(privateCards, card, false) > 0;
	}
	
	/**
	 * 
	 * @return privateCards
	 */
	public ArrayList<Integer> getPrivateCards() {
		return privateCards;
	}

	public ArrayList<Integer> getOutCards() {
		return outCards;
	}

	public ArrayList<Integer> getMingBuCards() {
		return mingBuCards;
	}

	public boolean isQiPai() {
		return isQiPai;
	}

	public List<Long> getKanPaiList() {
		return kanPaiList;
	}

	public void setPrivateCards(ArrayList<Integer> privateCards) {
		this.privateCards = privateCards;
	}

	public void setOutCards(ArrayList<Integer> outCards) {
		this.outCards = outCards;
	}

	public void setMingBuCards(ArrayList<Integer> mingBuCards) {
		this.mingBuCards = mingBuCards;
	}

	public void setBeiShu(int beiShu) {
		this.beiShu = beiShu;
	}

	public void setYaZhu(int yaZhu) {
		this.yaZhu = yaZhu;
	}

	public void setQiPai(boolean isQiPai) {
		this.isQiPai = isQiPai;
	}

	public void setKanPaiList(List<Long> kanPaiList) {
		this.kanPaiList = kanPaiList;
	}
	
	public void addAllPrivateCards (ArrayList<Integer> adds) {
		this.privateCards.addAll(adds);
	}
	
	
	
}
