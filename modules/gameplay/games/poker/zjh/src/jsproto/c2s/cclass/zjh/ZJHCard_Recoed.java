package jsproto.c2s.cclass.zjh;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.BaseSendMsg;

/*
 * 记录
 * **/

public class ZJHCard_Recoed extends BaseSendMsg {
	private List<Integer> cardList = new ArrayList<>(); //最终胡牌的列表
	private int point = 0;//积分
	private int cardType = 0;//牛牛类型
	
	public List<Integer> getCardList() {
		return cardList;
	}
	public void setCardList(List<Integer> cardList) {
		this.cardList = cardList;
	}
	public int getPoint() {
		return point;
	}
	public void setPoint(int point) {
		this.point = point;
	}
	public int getCardType() {
		return cardType;
	}
	public void setCardType(int cardType) {
		this.cardType = cardType;
	}
}
