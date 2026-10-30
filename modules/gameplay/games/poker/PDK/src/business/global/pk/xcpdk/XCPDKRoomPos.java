package business.global.pk.xcpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.xcpdk.c2s.cclass.XCPDKRoom_PosEnd;
import business.xcpdk.c2s.cclass.XCPDKRoom_RecordPosInfo;
import cenum.RoomTypeEnum;
import com.ddm.server.common.CommLogD;
import com.aoo.bcg.common.reconnect.CardPerspective;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.Victory;
import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 房间内每个位置信息
 * 
 * @author Huaxing
 *
 */
public class XCPDKRoomPos extends AbsRoomPos {

	private ArrayList<Integer> privateCards = new ArrayList<>(); // 权威手牌
	private ArrayList<Integer> backupsCards = new ArrayList<>(); // 发牌快照
	private ArrayList<Integer> privateCardsCopy = new ArrayList<>(); // 操作回滚快照
	private  	int  				m_nWin  = 0 ; // 赢场数
	private  	int    				m_nLose = 0; // 输场数
	private  	int 				m_nFlat = 0; // 平场数
	public int maxPoint = 0; //单局最高
	public int outBombNum = 0; //炸弹数
	private Boolean beginFlag=false;//开始标识
	private int tuoGuanSetCount = 0; // 连续托管局数

	//整大局剩余的时间，剩余几秒，几秒后进入托管
	private long secTotal = 0;
	//赢家的炸弹
	private List<Integer> bombNumList = new ArrayList<>();
	public XCPDKRoomPos(int getPosID, AbsBaseRoom room) {
		super(getPosID, room);
	}

	public Boolean getBeginFlag() {
		return beginFlag;
	}

	public void setBeginFlag(Boolean beginFlag) {
		this.beginFlag = beginFlag;
	}

	/**
	 * 初始化手牌
	 * @param cards
	 */
	public void init(List<Integer> cards) {
		this.privateCards = new ArrayList<>(cards);
		this.backupsCards = new ArrayList<>(cards);
	}
	
	/**
	 * 初始化手牌
	 * @param card
	 */
	public void addCard(Integer card) {
		this.privateCards.add(card);
		this.backupsCards.add(card);
	}
	
//
//	/**
//	 * 作弊初始化
//	 * 暂时没用到。。。先留着
//	 * @param privateCards
//	 * @param publicCards
//	 * @param handCard
//	 */
//	public void init(List<Integer> cards, List<List<Integer>> publicCards, int handCard) {
//		this.privateCards = new ArrayList<>(cards);
//		this.backupsCards = new ArrayList<>(cards);
//	}
	
	
	/**
	 * 获取牌组信息
	 * @return
	 */
	public ArrayList<Integer> getNotifyCard(long pid, boolean isOpenCard) {
		return CardPerspective.hand(privateCards, pid == this.getPid() || isOpenCard);
	}


	/**
	 * 删除牌组信息
	 * @return
	 */
	public boolean deleteCard(ArrayList<Integer> cradList) {
		int sizeCardList = cradList.size();
		this.privateCardsCopy = (ArrayList<Integer>) this.privateCards.clone();
		int sizePrivateCards =this.privateCardsCopy.size();
		for (Integer byte1 : cradList) {
			boolean flag =  this.privateCards.remove(byte1);
			if (!flag) {
				CommLogD.error("deleteCard error :Pid:{},PosID:{},beforeCount:{},requestCount:{},afterCount:{}",this.getPid(),this.getPosID(),privateCardsCopy.size(),cradList.size(),privateCards.size());
				this.privateCards = this.privateCardsCopy;
				return false;
			}
		}
		if(sizePrivateCards - sizeCardList != this.privateCards.size()) {
			CommLogD.error("deleteCard count mismatch :Pid:{},PosID:{},beforeCount:{},requestCount:{},afterCount:{}",this.getPid(),this.getPosID(),privateCardsCopy.size(),cradList.size(),privateCards.size());
			this.privateCards = this.privateCardsCopy;
			return false;
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
	
	
	public XCPDKRoom_PosEnd calcPosEnd(XCPDKSetPos setPos){
		XCPDKRoomSet set = (XCPDKRoomSet) this.getRoom().getCurSet();

		XCPDKRoom_PosEnd posEnd =  new XCPDKRoom_PosEnd();
		int setPoint = set.pointList.get(this.getPosID());
		List<Victory> roomDoubleList = set.getRoomDoubleList();
		Optional<Victory> first = roomDoubleList.stream().filter(n -> n.getPos() == this.getPosID()).findFirst();
		posEnd.doubleNum = first.map(Victory::getNum).orElse(0);
		posEnd.point = (int) set.pointList.get(this.getPosID());
		posEnd.pos = this.getPosID();
		posEnd.pid = this.getPid();	// 竞技点分数
		if (set.room.calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
			posEnd.sportsPoint = setPos.getDeductPointYiKao();
		} else {
			posEnd.sportsPoint = setSportsPoint(setPoint);
		}
		posEnd.beShutDow = set.beShutDowList.get(this.getPosID());
		posEnd.xiaoGuan = set.xiaoGuanList.get(this.getPosID());
		posEnd.baseMark = this.getRoom().getBaseMark();
		Optional<Victory> victory = set.robCloseList.stream().filter(n->n.getPos() == this.getPosID()).findFirst();
		posEnd.robClose = victory.map(Victory::getNum).orElse(0);
		for (ArrayList<Integer> sizeList : set.surplusCardRecordList) {
			posEnd.surplusCardList.add(sizeList.get(this.getPosID()));
		}
		this.calcRoomPoint(set.pointList.get(this.getPosID()));
		return posEnd;
	}
	/**
	 * 计算房间分数
	 */
	@Override
	public void calcRoomPoint(int point) {
		XCPDKRoomSet set = (XCPDKRoomSet) this.getRoom().getCurSet();
		if(set==null|| MapUtils.isEmpty(set.getPosDict())){
			// todo 防止当前局出现空（例如吊蟹在牌局开始之前需扣底分）
			super.calcRoomPoint(point);
			return;
		}
		XCPDKSetPos setPos = set.getPosDict().get(this.getPosID());
		this.setPoint(this.getPoint() + point);
		this.setPointYiKao(CommMath.addDouble(this.getPointYiKao() ,setPos.getDeductEndPoint()));
		this.calcRoomSportsPoint(point,setPos.getDeductEndPoint());
		if (getRoom().isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
			//小于0的 直接重置0
			if (this.getRoomSportsPoint() <= 0) {
				this.setRoomSportsPoint(0D);
			}
		}
	}

	public void calcPosResult() {
		XCPDKRoomSet set = (XCPDKRoomSet) this.getRoom().getCurSet();
		XCPDKRoom_RecordPosInfo cRecord = (XCPDKRoom_RecordPosInfo)getResults();
		if (null == cRecord) {
			cRecord = new XCPDKRoom_RecordPosInfo();
		}
		cRecord.setPid(getPid());
		cRecord.setPosId(getPosID());
		cRecord.addToPoint(set.pointList.get(this.getPosID()));
		setResults(cRecord);
	}
	/**
	 * 竞技点
	 */
	@Override
	public Double sportsPoint() {
		if ((getRoom().calcFenUseYiKao()||getRoom().isRulesOfCanNotBelowZero()) && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
			if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
				return CommMath.FormatDouble(this.getPointYiKao());
			} else {
				return null;
			}
		}
		return super.sportsPoint();
	}
//	/**
//	 * 房间记录结束
//	 * @return
//	 */
//	public Room_RecordPosEnd getRecordPosEnd() {
//		Room_RecordPosEnd ret = new Room_RecordPosEnd();
//		GameRecordBO sssBO = new GameRecordBO();
//		
//		ret.pid = this.pid; // 玩家ID
//		ret.setID = this.room.getCurSetID();//玩家局数
//		ret.roomID = this.room.getRoomID();//房间ID
//		
//		XCPDKCard_Recoed sRecoed = new XCPDKCard_Recoed();
//		
//		XCPDKRoomSet set  = (XCPDKRoomSet) this.room.getCurSet();
//		
//		int point = (int)set.pointList.get(getPosID());
//
//		sRecoed.setCardList(privateCards);
//		sRecoed.setPoint(point);
//		
//		ret.gameType = GameType.XCPDK.value();
//		ret.setEnd = TimeUtil.getStringDate();
//		ret.point = point;
//
//		
//		GameJson<XCPDKCard_Recoed> gameJson = new GameJson<XCPDKCard_Recoed>(sRecoed);
//		ret.gameJson = gameJson.toJson(XCPDKCard_Recoed.class);
//		sssBO.setRoomRecordPosEnd(ret);
//		sssBO.insert_sync();
//		
//		return ret;
//	}

	/**
	 * @return privateCards
	 */
	public ArrayList<Integer> getPrivateCards() {
		return new ArrayList<>(privateCards);
	}

	ArrayList<Integer> cards() { return privateCards; }

	ArrayList<Integer> dealtCards() { return backupsCards; }

	/**
	 * @return m_nWin
	 */
	public int getWin() {
		return m_nWin;
	}

	/**
	 * @param nWin 要设置的 m_nWin
	 */
	public void addWin(int nWin) {
		this.m_nWin += nWin;
	}

	/**
	 * @return m_nLose
	 */
	public int getLose() {
		return m_nLose;
	}

	/**
	 * @param nLose 要设置的 m_nLose
	 */
	public void addLose(int nLose) {
		this.m_nLose += nLose;
	}

	/**
	 * @return m_nFlat
	 */
	public int getFlat() {
		return m_nFlat;
	}

	/**
	 * @param nFlat 要设置的 m_nFlat
	 */
	public void addFlat(int nFlat) {
		this.m_nFlat += nFlat;
	}

	/**
	 * @return backupsCards
	 */
	public ArrayList<Integer> getBackupsCards() {
		return new ArrayList<>(backupsCards);
	}

	/**
	 * 清除最近出手的时间
	 */
	public void clearLatelyOutCardTime() {
		this.setLatelyOutCardTime(0L);
	}

	public void setMaxPoint(int maxPoint) {
		this.maxPoint = this.maxPoint > maxPoint ? this.maxPoint : maxPoint;
	}

	/**
	 * 新增炸弹数
	 */
	public void addOutBomb() {
		this.outBombNum++;
	}

	public int getTuoGuanSetCount() {
		return tuoGuanSetCount;
	}

	public void setTuoGuanSetCount(int tuoGuanSetCount) {
		this.tuoGuanSetCount = tuoGuanSetCount;
	}

	/**
	 * 增加托管局数
	 */
	public void addTuoGuanSetCount(){
		tuoGuanSetCount += 1;
	}

	/**
	 * 连续托管局数清零
	 */
	public void clearTuoGuanSetCount(){
		tuoGuanSetCount = 0;
	}

	/**
	 * 设置托管状态
	 *
	 * @param isTrusteeship 托管状态
	 * @param isOwn         是否屏蔽自己
	 */
	public void setTrusteeship(boolean isTrusteeship, boolean isOwn) {
		if (this.isTrusteeship() == isTrusteeship) {
			return;
		}
		// 托管2小局解散：连续2局托管
		if(!isTrusteeship){ // 玩家取消托管，连续托管局数清零
			this.clearTuoGuanSetCount();
		}else{
			this.addTuoGuanSetCount();
		}
		this.setTrusteeship(isTrusteeship);
		if (isOwn) {
			this.getRoom().getRoomPosMgr().notify2ExcludePosID(this.getPosID(), this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
		} else {
			this.getRoom().getRoomPosMgr().notify2All(this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
		}
	}

	public long getSecTotal() {
		return secTotal;
	}

	public void setSecTotal(long secTotal) {
		this.secTotal = secTotal;
	}

	public List<Integer> getBombNumList() {
		return bombNumList;
	}
}
