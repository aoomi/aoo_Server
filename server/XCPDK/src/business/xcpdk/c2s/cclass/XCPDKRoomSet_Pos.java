package business.xcpdk.c2s.cclass;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局中每个位置信息
 * @author zaf
 *
 */
public class XCPDKRoomSet_Pos {

	public int 			posID = 0; 					// 座号ID
	public long 		pid = 0; 					// 账号
	public List<Integer> 	cards = new ArrayList<>();	//牌
	public int 			addDouble = 0;				//加倍
	public int 			point;						//积分
	public int 			robClose;					//抢关门
	public int 			openCard;					//明牌
	public boolean 		beShutDow; 	//是否被关门
	/**
	 * 竞技点
	 */
	public Double sportsPoint;
	public Double 		sportsPoint1;						//积分
	public ArrayList<Integer> surplusCardList = new ArrayList<Integer>();		//剩余牌数
	//整大局剩余的时间，剩余几秒，几秒后进入托管
	private int secTotal = 0;
	public XCPDKRoomSet_Pos(int posID, long pid, List<Integer> cards, int addDouble,  int point, int robClose, int openCard, ArrayList<Integer> surplusCardList, boolean beShutDow,Double sportsPoint1) {
		super();
		this.posID = posID;
		this.pid = pid;
		this.beShutDow = beShutDow;
		this.cards = cards;
		this.addDouble = addDouble;
		this.point = point;
		this.robClose = robClose;
		this.openCard = openCard;
		this.surplusCardList = surplusCardList;
		this.sportsPoint1 = sportsPoint1;
	}

	public XCPDKRoomSet_Pos() {
	}

	public void setSecTotal(int secTotal) {
		this.secTotal = secTotal;
	}
}
