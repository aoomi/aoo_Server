package jsproto.c2s.cclass.zjh;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.pk.Victory;

/**
 * 一局中每个位置信息
 * @author zaf
 *
 */
public class ZJHRoomSet_Pos {

	public int 			posID = 0; 					// 座号ID
	public long 		pid = 0; 					// 账号
	public List<Integer> 	cardList = new ArrayList<>();	//牌
	public int			addBet = 0;					//下注分数
	public boolean 		isPlaying = false;			//当前局是否在玩
	public int			cardStatus; //牌状态
	public ArrayList<Victory>  recordList;//记录历史比牌的输赢 棋牌
	public boolean		isCallBacker = false;		//是否是庄家
	public int  		crawType;					//类型
	public int 			point;						//积分

	public ZJHRoomSet_Pos(int posID, long pid, List<Integer> cards, int addBet, boolean isPlaying,
			 int cardStatus, ArrayList<Victory>  recordList, boolean isCallBacker,int  	crawType, int point) {
		super();
		this.posID = posID;
		this.pid = pid;
		this.cardList = cards;
		this.addBet = addBet;
		this.isPlaying = isPlaying;
		this.cardStatus = cardStatus;
		this.recordList = recordList;
		this.isCallBacker = isCallBacker;
		this.crawType = crawType;
		this.point = point;
	}

	public ZJHRoomSet_Pos() {
	}

}
