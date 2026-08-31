package business.global.pk.xcpdk;

import business.global.room.base.AbsRoomPos;
import business.xcpdk.c2s.cclass.XCPDK_Bomb;
import business.xcpdk.c2s.cclass.XCPDK_define;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_CARD_TYPE;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_ROBCLOSE_STATUS;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_WANFA;
import business.xcpdk.c2s.iclass.CXCPDK_OpCard;
import business.xcpdk.c2s.iclass.SXCPDK_OpCard;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import core.db.persistence.BaseDao;
import jsproto.c2s.cclass.pk.BasePocker;
import jsproto.c2s.cclass.pk.BasePocker.PockerValueType;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 跑得快一局游戏逻辑
 *
 * @author zaf
 *
 */

public class XCPDKRoomSetSound {

	public XCPDKRoom room = null;
	public XCPDKRoomSet set = null;
	private int m_OpPos = -1 ; //当前操作位置
	private int m_lastOpPos = -1;//最后一次操作位置
	private int m_lastOpPosBack = -1;//最后一次操作位置
	private int m_opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value();  //XCPDK_CARD_TYPE 操作类型及牌的类型
	private int m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value();  //XCPDK_CARD_TYPE 操作类型及牌的类型
	private ArrayList<Integer> m_cardList = new ArrayList<Integer>();
	private boolean m_bTurnEnd = false;
	private boolean m_bSetEnd = false;

	private static final int INTERVAL = 30000;//时间间隔
	private static final int AVAULE = 0x0E;//a的值
	private int roundBombCount = 0; //本轮炸弹个数
	private int maxBombPos = -1;//本轮最大炸弹玩家
	private boolean isCheck = false;//是否进行了最后一手检测
	private List<Integer> threeList = Arrays.asList(0x03,0x13,0x33);
	private Integer twoValue = 15;
	public List<XCPDK_Bomb> bombList = new ArrayList<>() ;		//炸弹列表

	public XCPDKRoomSetSound( XCPDKRoomSet set) {
		this.set = set;
		this.room = set.room;
		m_lastOpPosBack = /*this.m_lastOpPos = */ this.m_OpPos = set.getOpPos();
	}

	public void clean () {
		this.room  = null;
		this.set = null;
		this.m_cardList = null;
	}

	/**
	 *  尝试开始回合, 如果失败，则set结束
	 * @return
	 */
	public boolean tryStartRound() {

		return true;
	}

	public boolean update(int sec){
		if(m_bTurnEnd || m_bSetEnd) {
			return true;
		}
		return false;
	}

	/**
	 * 托管
	 * @param pos
	 */
	@SuppressWarnings("unchecked")
	public void roomTrusteeship(int pos) {

		if(m_bSetEnd || this.m_OpPos != pos) {
			return;
		}

		if(CommTime.nowMS()  - this.set.startMS <= 200 ){
			return;
		}

		WebSocketRequest request = new WebSocketRequestDelegate();
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);

		if(roomPos.getPrivateCards().size() <= 0){
			this.checkEndSet(pos);
			return;
		}

		//最后一手
		if(!isCheck){
			ArrayList<Integer> bombByList = getBombByList(roomPos.getPrivateCards(), 0);
			List<XCPDK_CARD_TYPE> types = Arrays.asList(XCPDK_CARD_TYPE.XCPDK_WANFA_SINGLECARD,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_DUIZI,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_SHUNZI,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI,
//					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI1,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI2,
//					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI1,
//					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI2,
//					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI3,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN,
					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_FEIJI3,
//					XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_FEIJI4,
					XCPDK_CARD_TYPE.XCPDK_WANFA_LIANDUI);
			for (XCPDK_CARD_TYPE type : types) {
				int daiNum = 0;
				if(!XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.equals(type) && CollectionUtils.isNotEmpty(bombByList)){
					continue;
				}
				Map<Integer, Long> collect = roomPos.getPrivateCards().stream().collect(Collectors.groupingBy(BasePocker::getCardValueEx, Collectors.counting()));
				long count = collect.values().stream().filter(z -> z >= 3).count();
				if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI1==type) {
					daiNum = 1;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI2==type){
					daiNum = 2;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI1==type){
					daiNum = 1;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI2==type){
					daiNum = 2;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI3==type){
					daiNum = 3;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_FEIJI3==type){

					for (int i=2;i<=count;i++){
						for(int j =0;j<2;j++){
							daiNum = i*XCPDKRoomSet.DEFAULTDAINUM*j;
							CXCPDK_OpCard make = CXCPDK_OpCard.make(this.room.getRoomID(), pos, type.value(), new ArrayList<>(roomPos.getPrivateCards()), daiNum, true);
							make.feiJiNum = i;
							boolean flag =  this.onOpCard(request, make,false);
							if(flag){
								return;
							}
						}
					}
					continue;
				}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_FEIJI4==type){
					daiNum = XCPDKRoomSet.DEFAULTDAINUM+1;
				}else if(XCPDK_CARD_TYPE.XCPDK_WANFA_LIANDUI==type){
					if(roomPos.getPrivateCards().size()%2!=0 || roomPos.getPrivateCards().size()>8){
						continue;
					}
				}
				boolean flag = this.onOpCard(request, CXCPDK_OpCard.make(this.room.getRoomID(), pos, type.value(), new ArrayList<>(roomPos.getPrivateCards()), daiNum,true),false);
				if(flag){
					return;
				}
			}
			isCheck = true;
		}

		ArrayList<Integer> tempCard = (ArrayList<Integer>) roomPos.cards().clone();
		tempCard.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		ArrayList<Integer> outList = new ArrayList<Integer>();
		int daiNum = 0;

		int opCardType = m_opCardType;
		m_opCardTypeBackUp = m_opCardType;
		switch (XCPDK_CARD_TYPE.valueOf(m_opCardType)) {
			case XCPDK_CARD_TYPE_DUIZI:  			//对子
			{
				outList = this.getSameCard(tempCard, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB,false);
			}
			break;
			case XCPDK_CARD_TYPE_3BUDAI:  		//3不带
			{
				outList = this.getSameCard(tempCard, 3, 0, PockerValueType.POCKER_VALUE_TYPE_THREE,false);
			}
			break;
//			case XCPDK_CARD_TYPE_3DAI1: 			//3带1
//			{
//				daiNum = 1;
//				outList = this.getSameCard(tempCard, 3, 1, PockerValueType.POCKER_VALUE_TYPE_THREE,false);
//			}
//			break;
			case XCPDK_CARD_TYPE_3DAI2:  			//3带2
			{
				daiNum = 2;
				outList = this.getSameCard(tempCard, 3, 2, PockerValueType.POCKER_VALUE_TYPE_THREE,true);
			}
			break;
//			case XCPDK_CARD_TYPE_4DAI1:  			//4带1
//			{
//				daiNum = 1;
//				outList = this.getSameCard(tempCard, 4, 1, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI2:  			//4带2
//			{
//				daiNum = 2;
//				outList = this.getSameCard(tempCard, 4, 2, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI3:  			//4带3
//			{
//				daiNum = 3;
//				outList = this.getSameCard(tempCard, 4, 3, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
			case 	XCPDK_CARD_TYPE_ZHADAN:  			//炸弹
			{
				outList = getBombByList(tempCard,getFou1ZhaTypeValue(m_cardList));
//				outList = this.getSameCard(tempCard, 4, 0, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
			}
			break;
			case XCPDK_CARD_TYPE_SHUNZI:  		//顺子
			{
				outList = this.getShunZi(tempCard);
			}
			break;
			case 	XCPDK_CARD_TYPE_FEIJI3:  			//飞机
			{
				if(m_cardList.size()%3==0){
					daiNum = 0;
				}else{
					daiNum = XCPDKRoomSet.DEFAULTDAINUM;
				}
				outList = this.getLianDui(tempCard, 3, daiNum, PockerValueType.POCKER_VALUE_TYPE_THREE,true);
			}
			break;
//			case 	XCPDK_CARD_TYPE_FEIJI4:  //飞机带翅膀
//			{
//				daiNum = XCPDKRoomSet.DEFAULTDAINUM+1;
//				outList = this.getLianDui(tempCard, 4, daiNum, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
			case 	XCPDK_WANFA_LIANDUI:  //联队
			{
				outList = this.getLianDui(tempCard, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB,false);
			}
			break;
			case 	XCPDK_WANFA_SINGLECARD:
			{
				outList = this.getSinglecard(tempCard, 1,  m_cardList.size() > 0 ? m_cardList.get(0) : Integer.valueOf((byte) 0),false,(ArrayList<Integer>) tempCard.clone());
				if(m_opCardTypeBackUp != XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()){
					if (outList.size() > 0 && tempCard.size() > 0 ) {
						if (!this.checkNextIsOneCard(pos, outList.get(0))) {
							tempCard.sort(BasePockerLogic.sorterBigToSmallNotTrump);
							outList.clear();
							if(tempCard.size() > 0) {
								outList.add(tempCard.get(0));
							}
						}
					}
				}
			}
			break;
			case XCPDK_CARD_TYPE_NOMARL:
			{
				daiNum = this.getNomarlTypeCard(pos,outList,  (ArrayList<Integer>)tempCard.clone(),(ArrayList<Integer>)tempCard.clone());
				if(this.set.isFirstOp() && !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_HEITAO3BUBI) && !outList.contains(this.set.m_FirstOpCard) && this.set.m_FirstOpCard>0){
					//首出没带首出的牌
					ArrayList<Integer> sameCard = BasePockerLogic.getSameCard(tempCard, this.set.m_FirstOpCard, true);
					if(CollectionUtils.isNotEmpty(sameCard)){
						sameCard.sort((o2,o1)->{
							if(o2==this.set.m_FirstOpCard){
								return -1;
							}
							return 0;
						});
					}
					if(sameCard.size()>=3){
						List<Integer> daiList = tempCard.stream().filter(z -> BasePocker.getCardValueEx(z) != BasePocker.getCardValueEx(this.set.m_FirstOpCard) && BasePocker.getCardValueEx(z) != twoValue).collect(Collectors.toList());
						if(CollectionUtils.isNotEmpty(daiList)){
							//4带1炸弹,333+x炸弹 AAA+x炸弹
							List<Integer> unionList = ListUtils.union(Arrays.asList(daiList.get(0)),sameCard);
							if(getFou1ZhaTypeValue(unionList)>0){
								m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
								outList.clear();
								outList.addAll(unionList);
							}else{
								//3带
								m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI.value();
								outList.clear();
								outList.addAll(sameCard.subList(0,3));
							}
						}else{
							//出对子
							m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_DUIZI.value();
							outList.clear();
							outList.addAll(sameCard.subList(0,2));
						}
					}else if(sameCard.size()>=2){
						m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_DUIZI.value();
						outList.clear();
						outList.addAll(sameCard.subList(0,2));
					}else if(sameCard.size()>=1){
						m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_WANFA_SINGLECARD.value();
						outList.clear();
						outList.addAll(sameCard.subList(0,1));
					}
				}
			}
			break;
			default:
				CommLogD.error("not find opTYpe ="+m_opCardType +","+ XCPDK_CARD_TYPE.valueOf(m_opCardType));
				break;
		}
		opCardType = m_opCardTypeBackUp;
		if (CollectionUtils.isEmpty(outList)) {
			outList = getBombByList(tempCard,XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() == opCardType?getFou1ZhaTypeValue(m_cardList):0);
			if(CollectionUtils.isNotEmpty(outList)){
				opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
			}else{
				opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value();
			}
		}

//		//炸弹不可拆3A
//		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3AZHA) && !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_ZHADANKECHAI)){
//			if(opCardType!=XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value() && outList.stream().anyMatch(n->BasePocker.getCardValueEx(n) == BasePocker.getCardValueEx(AVAULE))){
//				ArrayList<Integer> tempList = BasePockerLogic.getSameCard(roomPos.getPrivateCards(), 0x0E, true);
//				if(null != tempList &&  tempList.size() == 3){
//					opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
//					outList = tempList;
//				}
//			}
//		}

		boolean flag =  this.onOpCard(request, CXCPDK_OpCard.make(this.room.getRoomID(), pos, opCardType, outList, daiNum,true),true);
		if (!flag) {
			if (CommTime.nowMS()  - this.set.startMS > INTERVAL ) {
				opCardType  = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value();
				flag = this.onOpCard(request, CXCPDK_OpCard.make(this.room.getRoomID(), pos, opCardType, outList, daiNum,true),true);
				if (!flag) {
					this.set.endSet();
					CommLogD.error("roomTrusteeship is not robot roomID:{}, handCount:{}, opType:{}, playedCount:{}", this.room.getRoomID(), tempCard.size(), m_opCardType, m_cardList.size());
				}
				return;
			}
		}
	}

	/**
	 * 检查炸弹可拆
	 * @return
	 */
	private boolean checkZhaDan(CXCPDK_OpCard opCard,ArrayList<Integer> cardList) {
//		final boolean is3AZha = this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3AZHA);
//		if(!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_ZHADANKECHAI)){
//			//默认打牌或者不出不检验
//			if(opCard.opCardType!=XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value() && opCard.opCardType!=XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value() && opCard.opCardType!=XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()){
//				//分组
//				Map<Integer, Long> cardGroupMap = cardList.stream().collect(
//						Collectors.groupingBy(p -> BasePocker.getCardValueEx(p), Collectors.counting()));
//				if(opCard.cardList!=null){
//					//检测炸弹
//					return opCard.cardList.stream().allMatch(n->{
//						if(cardGroupMap.get(BasePocker.getCardValueEx(n))!=null){
//							boolean isNormalBomb = cardGroupMap.get(BasePocker.getCardValueEx(n))==4;
//							boolean is3ABomb = is3AZha?cardGroupMap.get(BasePocker.getCardValueEx(n))==3 && BasePocker.getCardValueEx(n) == BasePocker.getCardValueEx(AVAULE):false;
//							return !isNormalBomb && !is3ABomb;
//						}
//						return false;
//					});
//				}
//				return false;
//			}
//			return true;
//		}
		return true;
	}

	public synchronized boolean onOpCard(WebSocketRequest request, CXCPDK_OpCard opCard,boolean needNoTify){
		if(m_bSetEnd){
			if(null != request) {
				request.error(ErrorCode.NotAllow, "onOpCard error: m_bSetEnd is ture");
			}
			return false;
		}
		if (m_bTurnEnd) {
			if(null != request) {
				request.error(ErrorCode.NotAllow, "onOpCard error: m_bTurnEnd is ture");
			}
			return false;
		}

		if(opCard.pos != m_OpPos){
			if(null != request) {
				request.error(ErrorCode.NotAllow, "onOpCard error: not current pos op oppos: "+m_OpPos);
			}
			return false;
		}
		// 首出判断
		if (this.set.isFirstOp() && !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_HEITAO3BUBI) && set.m_FirstOpCard != 0) {
			boolean isConstantFirstCard = opCard.cardList.stream().anyMatch(n -> n == set.m_FirstOpCard);
			if (!isConstantFirstCard) {
				if (null != request) {
					request.error(ErrorCode.NotAllow, "onOpCard error: not current pos op oppos: " + m_OpPos);
				}
				return false;
			}
		}
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(opCard.pos);
		//炸弹不可拆限制
		boolean isCanOp = checkZhaDan(opCard,roomPos.getPrivateCards());
		if(!isCanOp){
			if(null != request) request.error(ErrorCode.ZhaDanBuKeChai, "onOpCard error: zha dan bu ke chai: "+opCard.cardList);
			return false;
		}


		if( XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value() == opCard.opCardType ){
			if(!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_FEIBICHU)){
				if (CommTime.nowMS()  - this.set.startMS >= INTERVAL && roomPos.isRobot()) {

				}else{
					if(this.m_opCardType == XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value()){
						if(null != request) {
							request.error(ErrorCode.NotAllow, "onOpCard error:XCPDK_WANFA_FEIBICHU");
						}
						return false;
					}
					if(this.m_opCardType != XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value() && this.checkHaveMaxCard(opCard.pos)){
						if(null != request) {
							request.error(ErrorCode.NotAllow, "onOpCard error:your have max card");
						}
						return false;
					}
				}
			}else{
				//非必出是你的回合不能不出
				if(this.m_opCardType == XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value()){
					if(null != request) {
						request.error(ErrorCode.NotAllow, "onOpCard error:XCPDK_WANFA_FEIBICHU");
					}
					return false;
				}
			}
		}else if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() == opCard.opCardType){

			if(!this.checkIsMyCard(opCard)){
				if(null != request) {
					request.error(ErrorCode.OP_CARD_ERROR, "onOpCard error:card is not myself");
				}
				return false;
			}

			if(!this.checkBomb(opCard.opCardType, opCard.cardList)){
				if(null != request && needNoTify) {
					request.error(ErrorCode.NotAllow, "onOpCard error:card checkBomb fail");
				}
				return false;
			}
		}else if (XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() != opCard.opCardType ) {

			if( XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value() != this.m_opCardType &&  this.m_opCardType != opCard.opCardType){
				if(null != request && needNoTify) {
					request.error(ErrorCode.NotAllow, "onOpCard error:optype do not op,this last opType:"+ this.m_opCardType+",your optype:"+opCard.opCardType);
				}
				return false;
			}

			if(!this.checkIsMyCard(opCard)){
				if(null != request) {
					request.error(ErrorCode.OP_CARD_ERROR, "onOpCard error:card is not myself");
				}
				return false;
			}

			if(!this.checkCardList(opCard)){
				if(null != request && needNoTify) {
					request.error(ErrorCode.NotAllow, "----------------------onOpCard error:card check fail"+room.getRoomKey()+"///"+opCard.cardList+"//"+opCard.opCardType+"///"+m_cardList);
				}
				return false;
			}

			if (XCPDK_CARD_TYPE.XCPDK_WANFA_SINGLECARD.value() == opCard.opCardType && !this.checkNextIsOneCard(opCard.pos, opCard.cardList.get(0)) ) {
				if(null != request) {
					request.error(ErrorCode.NotAllow, "onOpCard error:you must op max card");
				}
				return false;
			}
		}

		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_4DAIFAN)){
			if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_FEIJI4.value() == opCard.opCardType || XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI1.value() == opCard.opCardType || XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI2.value() == opCard.opCardType || XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_4DAI3.value() == opCard.opCardType){
				this.set.addRoomDouble(opCard.pos, XCPDKRoomSet.FOURDAIFANDOUBLE);
			}
		}


		if(opCard.cardList.size() > 0 && !roomPos.deleteCard(opCard.cardList)){
			if(null != request) {
				request.error(ErrorCode.NotAllow, "card delete validation failed");
			}
			return false;
		}

		if(null != request) {
			request.response();
		}

		this.set.setFirstOp(false);
		roomPos.clearLatelyOutCardTime();
		if(opCard.opCardType == XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value()){
			XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
			//更新打牌时间
			if(room.getRoomCfg().getFangJianXianShi() != 0){
				roomPos.setSecTotal(roomPos.getSecTotal() - (CommTime.nowMS() - this.set.startMS));
			}
			this.addOpPos(true);
			long runWaitSec = (CommTime.nowMS() - this.set.startMS)/1000 ;
			int secTotal = 0;
			int dataSecTotal = 0;
			//每个回合开始之前设置时间
			if (room.getRoomCfg().getFangJianXianShi() != 0) {//不是罚分玩法 用过的时间
				//本回合跑了多少秒
				long total = this.set.getTime(this.room.getRoomCfg().getFangJianXianShi());
				//除了本回合，之前的所有回合跑了多少秒
				long useTime2 = (((XCPDKRoomPos) room.getRoomPosMgr().getPosByPosID(m_OpPos)).getSecTotal() / 1000);
				long useTime1 = total - useTime2;
				//总的跑多少(本回合+之前所有回合跑的时间)，让客户端自己拿剩余多少秒去减
				int zongjie = (int)useTime1 + (int)runWaitSec;
//                    //该玩家使用的时间
				secTotal = zongjie;
				dataSecTotal = (int)total-((int)roomPos.getSecTotal() / 1000);
			}
			for (int i = 0; i < this.room.getPlayerNum(); i++) {
				ArrayList<Integer> privateList = resolveCardList(roomPos.cards(),i,roomPos.getPosID());
				if (0 == i) {
					this.set.getRoomPlayBack().playBack2Pos(i, SXCPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, m_OpPos, opCard.cardList, m_bTurnEnd, opCard.daiNum, m_bSetEnd,privateList,opCard.isFlash,runWaitSec,secTotal,dataSecTotal,getTrusteeshipList()), roomPosMgr.getAllPlayBackNotify());
				} else {
					this.room.getRoomPosMgr().notify2Pos(i, SXCPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, m_OpPos, opCard.cardList, m_bTurnEnd, opCard.daiNum, m_bSetEnd,privateList,opCard.isFlash,runWaitSec,secTotal,dataSecTotal,getTrusteeshipList()));
				}
			}
			return true;
		}

		if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value() == this.m_opCardType){
			this.m_opCardType = opCard.opCardType;
		}
		if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() == opCard.opCardType){
			this.m_opCardType = opCard.opCardType;
			int fou1ZhaTypeValue = getFou1ZhaTypeValue(opCard.cardList);
			bombList.add(new XCPDK_Bomb(roomPos.getPosID(),opCard.cardList,fou1ZhaTypeValue>20,CollectionUtils.isEmpty(roomPos.getPrivateCards())));
			roundBombCount++;
			maxBombPos = opCard.pos;
//			addBombByAtOnce(opCard.pos);
		}

		if(!this.set.isRobCloseCalc()){
			//设置抢关门是否成功
			if (this.set.getRobClosePos() >= 0 &&  this.set.getRobClosePos() != opCard.pos && this.set.getRobCloseNum() == XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()) {
				this.set.setRobCloseNum(XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_FAIL.value());
			}

			//设置反关门是否成功
			if (-1 == this.set.getReverseRobClosePos() && this.set.getFirstOpPos() != opCard.pos) {
				this.set.setReverseRobClosePos(opCard.pos);
				this.set.setReverseRobCloseNum(XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value());
			}
			else if ( -1 != this.set.getReverseRobClosePos() && this.set.getReverseRobClosePos() != opCard.pos) {
				this.set.setReverseRobCloseNum(XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_FAIL.value());
			}
		}

		this.set.addOpCardList(opCard.cardList,opCard.opCardType,opCard.pos);

		m_lastOpPosBack = this.m_lastOpPos = opCard.pos;
		this.m_cardList = opCard.cardList;

		if (m_cardList.size() <= 0) {
			CommLogD.error("cardList.size<=0");
		}

		if (this.set.getFirstOpPos() != opCard.pos && this.set.getFirstOpNum() == XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_NOMAL.value()) {
			XCPDKRoomPos firstRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(this.set.getFirstOpPos());
			this.set.setFirstOpNum(firstRoomPos.getPrivateCards().size());
		}

		//更新打牌时间
		if(room.getRoomCfg().getFangJianXianShi() != 0){
			roomPos.setSecTotal(roomPos.getSecTotal() - (CommTime.nowMS() - this.set.startMS));
		}
		this.addOpPos(false);
		XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();

		this.checkEndSet(opCard.pos);

		long runWaitSec = (CommTime.nowMS() - this.set.startMS)/1000 ;
		int secTotal = 0;
		int dataSecTotal = 0;
		//每个回合开始之前设置时间
		if (room.getRoomCfg().getFangJianXianShi() != 0) {//不是罚分玩法 用过的时间
			//本回合跑了多少秒
			long total = this.set.getTime(this.room.getRoomCfg().getFangJianXianShi());
			//除了本回合，之前的所有回合跑了多少秒
			long useTime2 = (((XCPDKRoomPos) room.getRoomPosMgr().getPosByPosID(m_OpPos)).getSecTotal() / 1000);
			long useTime1 = total - useTime2;
			//总的跑多少(本回合+之前所有回合跑的时间)，让客户端自己拿剩余多少秒去减
			int zongjie = (int)useTime1 + (int)runWaitSec;
//                    //该玩家使用的时间
			secTotal = zongjie;
			dataSecTotal = (int)total-((int)roomPos.getSecTotal() / 1000);
		}

		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			ArrayList<Integer> privateList = resolveCardList(roomPos.cards(),i,roomPos.getPosID());
			if (0 == i) {
				this.set.getRoomPlayBack().playBack2Pos(i, SXCPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, m_OpPos, opCard.cardList, m_bTurnEnd, opCard.daiNum, m_bSetEnd,privateList,opCard.isFlash,runWaitSec,secTotal,dataSecTotal,getTrusteeshipList()), roomPosMgr.getAllPlayBackNotify());
			} else {
				this.room.getRoomPosMgr().notify2Pos(i, SXCPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, m_OpPos, opCard.cardList, m_bTurnEnd, opCard.daiNum, m_bSetEnd,privateList,opCard.isFlash,runWaitSec,secTotal,dataSecTotal,getTrusteeshipList()));
			}
		}
		return true;
	}

	/**
	 * 处理私有牌显示
	 * @param cardsList 牌
	 * @param currentPos 自己的位置
	 * @param showOps 显示的位置
	 * @return
	 */
	public ArrayList<Integer> resolveCardList(ArrayList<Integer> cardsList,int currentPos,int showOps) {
		ArrayList<Integer> cards = new ArrayList<>();
		for(Integer card:cardsList){
			if(showOps == currentPos){
				cards.add(card);
			}else{
				cards.add(0);
			}
		}
		return cards;
	}

	/**
	 * 检查是否结束
	 * **/
	public void checkEndSet(int pos){
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);

		if(roomPos.cards().size() <= 0 && !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_XUEZHANDAODI)){
			//this.set.endSet();
			addBombScore();
			this.m_bSetEnd = true;
		}else if(roomPos.cards().size() <= 0 && this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_XUEZHANDAODI)){
			this.set.resultCalcEx();
		}
		if (this.getPlayerPlaying() <=  1) {
			//this.set.endSet();
			this.m_bSetEnd = true;
		}
	}

	/**
	 * 还有多少玩家
	 * **/
	public int getPlayerPlaying(){
		int count = 0;
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
			if(roomPos.cards().size() > 0){
				count++;
			}
		}
		return count;
	}


	/**
	 * 验证是否是下载最后一张单牌
	 * **/
	@SuppressWarnings("unchecked")
	public boolean checkNextIsOneCard(int pos, int card){
		int nextPos = pos;
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(nextPos);
		if (roomPos.getPrivateCards().size() == 1) {
			return true;
		}
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			nextPos = (++nextPos)%this.room.getPlayerNum();
			XCPDKRoomPos tempRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(nextPos);
			int cardNum = tempRoomPos.getPrivateCards().size();
			if(cardNum > 0){
				if(cardNum == 1) {
					ArrayList<Integer> cardList = (ArrayList<Integer>) roomPos.getPrivateCards().clone();
					cardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
					if (BasePocker.getCardValue( card ) != BasePocker.getCardValue(cardList.get(0))) {
						return false;
					}
				}
				break;
			}
		}

		return true;
	}

	/**
	 * 验证是否是自己的牌
	 * */
	public boolean checkIsMyCard(CXCPDK_OpCard opCard){
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(opCard.pos);
		for (Integer byte1 : opCard.cardList) {
			if (!roomPos.cards().contains(byte1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是不是最后一手的牌
	 * @return
	 */
	private boolean checkIsLastCard(ArrayList<Integer> cardList,XCPDKRoomPos roomPos) {
		long count = cardList.stream().distinct().count();
		if (count == cardList.size() && roomPos.getPrivateCards().containsAll(cardList)) {
			return roomPos.getPrivateCards().size() == cardList.size();
		}
		return false;
	}


	/**
	 * 牌验证
	 * **/
	public boolean  checkCardList(CXCPDK_OpCard opCard) {
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(opCard.pos);

		boolean flag = false;
		XCPDK_CARD_TYPE cardType = XCPDK_CARD_TYPE.valueOf(opCard.opCardType);
		switch (cardType) {
			case XCPDK_CARD_TYPE_DUIZI:  			//对子
			{
				flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB);
			}
			break;
			case XCPDK_CARD_TYPE_SHUNZI:  		//顺子
			{
				flag = this.checkShunZi(cardType, opCard.cardList);
			}
			break;
			case XCPDK_CARD_TYPE_3BUDAI:  		//3不带
			{
//				if (m_opCardType != opCard.opCardType &&  !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3BUDAI)  && roomPos.getPrivateCards().size() != 3) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 3, 0, PockerValueType.POCKER_VALUE_TYPE_THREE);
//				}
				flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 3, 0, PockerValueType.POCKER_VALUE_TYPE_THREE);

			}
			break;
//			case XCPDK_CARD_TYPE_3DAI1: 			//3带1
//			{
//				if (m_opCardType != opCard.opCardType && !this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3DAI1)  && roomPos.getPrivateCards().size() != 4 ) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 3, 1, PockerValueType.POCKER_VALUE_TYPE_THREE);
//				}
//			}
//			break;
			case XCPDK_CARD_TYPE_3DAI2:  			//3带2
			{
//				if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3DAI2)  ) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 3, 2, PockerValueType.POCKER_VALUE_TYPE_THREE);
//				}
				flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 3, 2, PockerValueType.POCKER_VALUE_TYPE_THREE);
			}
			break;
//			case XCPDK_CARD_TYPE_4DAI1:  			//4带1
//			{
//				if (m_opCardType != opCard.opCardType && !(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_4DAI1)||this.room.isSiDaiByNum(1))  && roomPos.getPrivateCards().size() != 5 ) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 4, 1, PockerValueType.POCKER_VALUE_TYPE_BOMB);
//				}
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI2:  			//4带2
//			{
//				if (m_opCardType != opCard.opCardType && !((this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_4DAI2)||this.room.isSiDaiByNum(2)))  && roomPos.getPrivateCards().size() != 6 ) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 4, 2, PockerValueType.POCKER_VALUE_TYPE_BOMB);
//				}
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI3:  			//4带3
//			{
//				if (!(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_4DAI3)||this.room.isSiDaiByNum(3))) {
//					flag = false;
//				} else {
//					flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, 4, 3, PockerValueType.POCKER_VALUE_TYPE_BOMB);
//				}
//			}
//			break;
			case 	XCPDK_CARD_TYPE_ZHADAN:  			//炸弹
			{
				flag = getFou1ZhaTypeValue(opCard.cardList) > getFou1ZhaTypeValue(m_cardList);
//				flag = this.checkSameCard(opCard.pos, cardType, opCard.cardList, opCard.cardList.size(), 0, PockerValueType.POCKER_VALUE_TYPE_BOMB);
			}
			break;
			case 	XCPDK_CARD_TYPE_FEIJI3:  			//飞机
			{
				flag = this.checkLianDui(cardType, opCard.cardList, 3, opCard.daiNum, PockerValueType.POCKER_VALUE_TYPE_THREE,checkIsLastCard(opCard.cardList,roomPos),opCard.feiJiNum,true);
			}
			break;
//			case 	XCPDK_CARD_TYPE_FEIJI4:  //飞机带翅膀
//			{
//				flag = this.checkLianDui(cardType, opCard.cardList, 4, opCard.daiNum, PockerValueType.POCKER_VALUE_TYPE_BOMB,false,0,false);
//			}
//			break;
			case 	XCPDK_WANFA_LIANDUI:  //联队
			{
				if(opCard.cardList.size()<=8){
					flag = this.checkLianDui(cardType, opCard.cardList, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB,false,0,false);
				}
			}
			break;
			case 	XCPDK_WANFA_SINGLECARD:
			{
				flag = this.checkSingleCard(cardType, opCard.cardList);
			}
			break;
			default:
				break;
		}
		return flag;
	}


	/**
	 * 顺子的牌
	 * @param cardType
	 * @param cardList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean checkShunZi(XCPDK_CARD_TYPE cardType, ArrayList<Integer> cardList){
		if (cardList.size() < 5) {
			return false;
		}


		boolean isNomarlOpCard = true;
		if(m_cardList.size() > 0){
			if(m_cardList.size()!=cardList.size()){
				return false;
			}
			isNomarlOpCard = false;
		}

		ArrayList<Integer> tempList = (ArrayList<Integer>) cardList.clone();
		//从大到小
		tempList.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		boolean isLaiZi = this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_LAIZI);

		if (isLaiZi) {
			ArrayList<Integer> List = this.getShunZiByList(tempList);
			if (List == null) {
				return false;
			}
			if(isNomarlOpCard) {
				return true;
			}
			ArrayList<Integer> mCardList = this.getShunZiByList(this.m_cardList);
			if (mCardList == null) {
				CommLogD.error("checkShunZi error: m_cardList do not get shun zi");
				return false;
			}else{
				return this.compareOneCard(List.get(0), mCardList.get(0));
			}
		} else {
			for (int i = 0 ; i < tempList.size() -1; i++) {
				if(Math.abs( BasePocker.getCardValue( tempList.get(i) )  - BasePocker.getCardValue( tempList.get(i+1) ) ) != 1){
					return false;
				}
			}
		}

		this.m_cardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		return isNomarlOpCard ? true : this.compareOneCard(tempList.get(0), this.m_cardList.get(0));
	}

	/**
	 * 相同的牌
	 * */
	public boolean  checkSameCard (int pos, XCPDK_CARD_TYPE cardType, ArrayList<Integer> cardList, int sameNum, int daiNum, PockerValueType pockerType) {
		boolean isLast = false;
		if(cardList.size()>(sameNum + daiNum)){
			return false;
		}
		if (cardList.size() !=  sameNum + daiNum) {
			XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);
			if(roomPos.getPrivateCards().size() != cardList.size()){
				return false;
			}
			isLast = true;
		}

		ArrayList< ArrayList<Integer> > opOutList = new ArrayList< ArrayList<Integer> > ();
		int count = BasePockerLogic.getSameCardByType(opOutList, cardList, pockerType);
		//只能3带1对
		if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI2.equals(cardType) && !isLast && (sameNum + daiNum)==5 && BasePockerLogic.getSameCardByType(new ArrayList<>(), cardList, PockerValueType.POCKER_VALUE_TYPE_SUB)!=2){
			return false;
		}
		boolean isLaiZi = this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_LAIZI);
		boolean isNomarlOpCard = true;

		ArrayList<ArrayList<Integer>> mCardList = this.getSameCardByList(this.m_cardList, sameNum);
		if(m_cardList.size() > 0){
			isNomarlOpCard = false;

			if(mCardList == null ||(null != mCardList && mCardList.size() <= 0)){
				CommLogD.error("checkSameCard error: sameNum:{}, cardCount:{}", sameNum, m_cardList.size());
				return false;
			}
		}




		if(!isLaiZi){
			if(count <= 0) {
				return false;
			}
			return isNomarlOpCard ? true : this.compare(opOutList, mCardList);
		}

		ArrayList<ArrayList<Integer>> outlist = this.getSameCardByList(cardList, sameNum);
		if(outlist == null || outlist.size() <= 0) {
			return false;
		}

		return isNomarlOpCard ? true : this.compare(outlist, mCardList);
	}

	/**
	 * 根据list获取联队list
	 * */
	public ArrayList<ArrayList<Integer>> getLianDuiList(ArrayList<ArrayList<Integer>> list, int size) {
		ArrayList<ArrayList<Integer>> tempList = new ArrayList<ArrayList<Integer>>();
		if (list == null || (null != list &&  list.size() < 2)) {
			return tempList;
		}
		int count  = list.size();
		ArrayList<Integer> opList = new ArrayList<Integer>();;
		for (int i = 0; i < count; i++) {
			opList.add(list.get(i).get(0));
		}

		ArrayList<Integer> tempOpList = this.getShunZiByListEx(opList, size );
		if(tempOpList == null ){
			return tempList;
		}

		ArrayList< ArrayList<Integer> >  lianDuiList = new ArrayList< ArrayList<Integer> >();
		for (Integer byte1 : tempOpList) {
			for (int i = 0; i < count; i++) {
				if (BasePocker.getCardValue(byte1) == BasePocker.getCardValue(list.get(i).get(0))) {
					lianDuiList.add(list.get(i));
					break;
				}
			}
		}
		return lianDuiList;
	}

	/**
	 * 联队
	 * */
	public boolean  checkLianDui (XCPDK_CARD_TYPE cardType, ArrayList<Integer> cardList, int sameNum, int daiNum, PockerValueType pockerType,boolean isLastCardList,int feiJiNum,boolean needPairs) {
		ArrayList< ArrayList<Integer> > opOutList = new ArrayList< ArrayList<Integer> > ();
		int count = BasePockerLogic.getSameCardByType(opOutList, cardList, pockerType);
		int size = feiJiNum>0?feiJiNum:(cardList.size() - daiNum)/sameNum ;

		ArrayList<ArrayList<Integer>> mCardList = this.getLianDuiList( this.getSameCardByList(this.m_cardList, sameNum), size);

		boolean isNomarlOpCard = true;
		if(m_cardList.size() > 0){
			isNomarlOpCard = false;

			if(mCardList == null || mCardList.size() < 2){
				CommLogD.error("checkLianDui error: sameNum:{}, cardCount:{}", sameNum, m_cardList.size());
				return false;
			}
		}

		if(feiJiNum>0 && feiJiNum*5<cardList.size()){
			return false;
		}

		//飞机最后一手牌不够不能出的bug，剑锋说只改鄱阳的
		if (!isNomarlOpCard && cardList.size() <  m_cardList.size() && !isLastCardList) {
			return false;
		}

		boolean isLaiZi = this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_LAIZI);

		if(!isLaiZi){

			ArrayList< ArrayList<Integer> >  lianDuiList = this.getLianDuiList(opOutList,size);
			if (null == lianDuiList || lianDuiList.size() < 2) {
				return false;
			}
			if(needPairs && daiNum>0){//飞机
				//只能飞机带1对
				ArrayList<Integer> cloneList = (ArrayList<Integer>)cardList.clone();
				cloneList.removeAll(lianDuiList.stream().flatMap(y->y.stream()).collect(Collectors.toList()));
				if(cloneList.size()<lianDuiList.size()*2 && isLastCardList){
					//最后一手
					return isNomarlOpCard ? true : this.compare(lianDuiList, mCardList);
				}else{
					//肥嘴猴一手必须严格相等
					if(BasePockerLogic.getSameCardByType(new ArrayList<>(), cloneList, PockerValueType.POCKER_VALUE_TYPE_SUB)!=size){
						return false;
					}
					return isNomarlOpCard ? true : this.compare(lianDuiList, mCardList);
				}
			}
			return isNomarlOpCard ? true : this.compare(lianDuiList, mCardList);
		}

		opOutList.clear();
		opOutList = this.getSameCardByList(cardList, sameNum);
		if(opOutList == null || opOutList.size() < 2) {
			return false;
		}

		count = opOutList.size();

		ArrayList<Integer> opList = new ArrayList<Integer>();;
		for (int i = 0; i < count; i++) {
			opList.add(opOutList.get(i).get(0));
		}

		ArrayList<Integer> tempOpList = this.getShunZiByList(opList);
		if(tempOpList == null ){
			return false;
		}

		if(needPairs){//飞机
			//只能飞机带1对
			ArrayList<Integer> cloneList = (ArrayList<Integer>)cardList.clone();
			cloneList.removeAll(tempOpList);
			if(!isLastCardList && BasePockerLogic.getSameCardByType(new ArrayList<>(), cloneList, PockerValueType.POCKER_VALUE_TYPE_SUB)!=size){
				return false;
			}
//			//检测对子是否满足
//			Map<Integer, List<Integer>> cards = cloneList.stream().collect(Collectors.groupingBy(n -> BasePocker.getCardValueEx(n)));
//			int leaveSize = 0;
//			for(Map.Entry<Integer,List<Integer>> entry:cards.entrySet()){
//				size-=entry.getValue().size()/2;
//				leaveSize+=entry.getValue().size()%2;
//			}
//			if(isLastCardList && leaveSize>size){
//				return false;
//			}
		}

		return isNomarlOpCard ? true :  this.compare(opOutList, mCardList);
	}

	/**
	 * 单牌
	 * */
	public boolean  checkSingleCard (XCPDK_CARD_TYPE cardType,  ArrayList<Integer> cardList) {
		if (cardList.size() !=  1 ) {
			return false;
		}
		boolean isNomarlOpCard = true;
		if(m_cardList.size() > 0){
			isNomarlOpCard = false;
		}
		return isNomarlOpCard ? true :  this.compareOneCard(cardList.get(0), m_cardList.get(0));
	}


	/**
	 * 比较牌的大小
	 * */
	public boolean compare(ArrayList< ArrayList<Integer> > leftList, ArrayList< ArrayList<Integer> > RightList) {
		if(leftList.size() != RightList.size()) {
			return false;
		}

		int num =  leftList.size() ;

		ArrayList<Integer> soundList = new ArrayList<Integer>();
		ArrayList<Integer> opList = new ArrayList<Integer>();
		for (int i = 0; i < num; i++) {
			if(RightList.size() > i) {
				soundList.add(RightList.get(i).get(0));
			}
			if(leftList.size() > i) {
				opList.add(leftList.get(i).get(0));
			}
		}

		ArrayList<Integer> tempLeft = this.getShunZiByList(opList);
		ArrayList<Integer> tempRight = this.getShunZiByList(soundList);

		tempLeft.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		tempRight.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		return this.compareOneCard(tempLeft.get(0), tempRight.get(0));
	}


	/**
	 * 比较一张牌的大小
	 * **/
	public boolean compareOneCard(Integer leftCard, Integer rightCard) {
		int cbLeftMaxValue= BasePocker.getCardValue(leftCard);
		int cbRightMaxValue= BasePocker.getCardValue(rightCard);
		return cbLeftMaxValue > cbRightMaxValue;
	}

	/**
	 * 获取带牌
	 * @param card
	 * **/
	public ArrayList<Integer> getSinglecard(ArrayList<Integer> cardList, int daiNum, int card,boolean needPair,ArrayList<Integer> cloneCardList){
		if(!needPair){//不需要对子
			return getSinglecard(cardList,daiNum,card,cloneCardList);
		}
		ArrayList<Integer> temp = new ArrayList<Integer>();
		Map<Integer, List<Integer>> valueMap = cardList.stream()
				.collect(Collectors.groupingBy(p -> BasePocker.getCardValueEx(p)));
		for(Map.Entry<Integer, List<Integer>> n:valueMap.entrySet()){
			if(n.getValue().size()>=4){
				List<Integer> cardDaiList = cloneCardList.stream().filter(z -> BasePocker.getCardValueEx(z) != n.getKey() && BasePocker.getCardValueEx(z) !=twoValue).collect(Collectors.toList());
				if(CollectionUtils.isNotEmpty(cardDaiList)){
					//4带1炸弹（如果需要不拆3带1这里也可以写上去>=3，并检测带牌，如果不拆连队炸弹，这里也可以检测下是不是满足连队）
					temp = new ArrayList<>(n.getValue().subList(0,4));
					temp.add(cardDaiList.get(0));
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
					return temp;
				}else{
					ArrayList<Integer> itemList = new ArrayList<>(n.getValue());
					if ((0 != card && BasePockerLogic.getCardValue(card) < n.getKey()) || card==0)  {
						temp.addAll(new ArrayList<>(itemList.subList(0,2)));
						if(temp.size() >= daiNum){
							return temp;
						}
						temp.addAll(new ArrayList<>(itemList.subList(2,4)));
						if(temp.size() >= daiNum){
							return temp;
						}
					}
				}
			}else if(n.getValue().size()>=2){
				if (0 != card && BasePockerLogic.getCardValue(card) < n.getKey())  {
					temp.addAll(new ArrayList<>(n.getValue().subList(0,2)));
				} else if(card == 0){
					temp.addAll(new ArrayList<>(n.getValue().subList(0,2)));
				}
			}
			if(temp.size() >= daiNum)
				return temp;
		}
		return temp;
	}

	/**
	 * 获取单牌
	 * @param card
	 * **/
	public ArrayList<Integer> getSinglecard(ArrayList<Integer> cardList, int daiNum, int card,ArrayList<Integer> cloneCardList){
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for (int i = PockerValueType.POCKER_VALUE_TYPE_SINGLE.value(); i <= PockerValueType.POCKER_VALUE_TYPE_BOMB.value(); i++) {
			ArrayList< ArrayList<Integer> > opOutList = new ArrayList< ArrayList<Integer> > ();
			int count = BasePockerLogic.getSameCardByTypeEx(opOutList, cardList, PockerValueType.valueOf(i));

			if(count <= 0) {
				continue;
			}
			if(i == PockerValueType.POCKER_VALUE_TYPE_BOMB.value()){
				ArrayList<Integer> bombList = opOutList.get(count - 1);
				List<Integer> cardDaiList = cloneCardList.stream().filter(z -> BasePocker.getCardValueEx(z) != BasePocker.getCardValueEx(bombList.get(0)) && BasePocker.getCardValueEx(z) !=twoValue).collect(Collectors.toList());
				if(CollectionUtils.isNotEmpty(cardDaiList)){
					//4带1炸弹（如果需要不拆3带1这里也可以写上去>=3，并检测带牌，如果不拆连队炸弹，这里也可以检测下是不是满足连队）
					temp.addAll(bombList);
					temp.add(cardDaiList.get(0));
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
					return temp;
				}else{
					//炸弹拆了，变成带牌
					if ((0 != card && BasePockerLogic.getCardValue(card) < BasePockerLogic.getCardValue(bombList.get(0))) || card == 0)  {
						for (Integer item : bombList) {
							temp.add(item);
							if(temp.size() >= daiNum) {
								return temp;
							}
						}
					}
				}

			}else {
				for (int j = 0; j < count; j++) {
					for (int j2 = 0; j2 < opOutList.get(j).size(); j2++) {
						int tempCard = opOutList.get(j).get(j2);
						if ((0 != card && BasePockerLogic.getCardValue(card) < BasePockerLogic.getCardValue(tempCard)) || card == 0)  {
							temp.add(tempCard);
						}

						if(temp.size() >= daiNum) {
							return temp;
						}
					}
				}
			}
		}
		return temp;
	}

	/**
	 * 相同的牌
	 * */
	public ArrayList<Integer>   getSameCard (ArrayList<Integer> cardList, int sameNum, int daiNum, PockerValueType pockerType,boolean isPair) {
		ArrayList<Integer> cardClone = (ArrayList<Integer>) cardList.clone();
		if ((cardList.size() <  sameNum + daiNum)&&daiNum==0) {
			return new ArrayList<Integer>();
		}

		cardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		ArrayList<ArrayList<Integer>> mCardList = this.getSameCardByList(this.m_cardList, sameNum);
		boolean isNomarlOpCard = true;

		if(m_cardList.size() > 0){
			isNomarlOpCard = false;

			if(mCardList == null || mCardList.size() <= 0){
				CommLogD.info("getSameCard error: sameNum:{}, cardCount:{}", sameNum, m_cardList.size());
				return new ArrayList<Integer>();
			}
		}


		for (int i = pockerType.value(); i < PockerValueType.POCKER_VALUE_TYPE_FLUSH.value(); i++) {
			ArrayList<Integer> temp = new ArrayList<Integer>();
			ArrayList< ArrayList<Integer> > opOutList = new ArrayList< ArrayList<Integer> > ();
			int count = BasePockerLogic.getSameCardByTypeEx(opOutList, cardList, PockerValueType.valueOf(i));
			if(count <= 0) {
				continue;
			}

			if (BasePocker.PockerValueType.POCKER_VALUE_TYPE_BOMB.value() == i) {
				if (XCPDK_define.XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() != m_opCardType) {
					ArrayList<Integer> bombList = opOutList.get(count - 1);
					List<Integer> cardDaiList = cardClone.stream().filter(z -> BasePocker.getCardValueEx(z) != BasePocker.getCardValueEx(bombList.get(0)) && BasePocker.getCardValueEx(z) !=twoValue).collect(Collectors.toList());
					if(CollectionUtils.isNotEmpty(cardDaiList)){
						//4带1炸弹（如果需要不拆3带1这里也可以写上去>=3，并检测带牌，如果不拆连队炸弹，这里也可以检测下是不是满足连队）
						temp.addAll(bombList);
						temp.add(cardDaiList.get(0));
						m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
						return temp;
					}
				} else {
					//上手出的是炸弹不走这里了，走新方法getBombByList
//					if (m_cardList.size() > 3) {
//						int cardValue = BasePocker.getCardValue(m_cardList.get(0));
//						for (ArrayList<Integer> opList : opOutList) {
//							int currentCardValue = BasePocker.getCardValue(opList.get(0));
//							if (currentCardValue > cardValue) {
//								temp.addAll(opList);
//								m_opCardTypeBackUp = XCPDK_define.XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
//								return temp;
//							}
//						}
//					}
				}

			}

			for (int j = 0; j < count; j++) {
				ArrayList<Integer> list = opOutList.get(j);
				if(isNomarlOpCard || ( !isNomarlOpCard && BasePockerLogic.getCardValue( list.get(0)) > BasePockerLogic.getCardValue(mCardList.get(0).get(0)))){
					for (int k = 0; k < sameNum; k++) {
						if(k < list.size()) {
							temp.add(list.get(k));
						}
					}
					break;
				}
			}
			if(temp.size() <= 0) {
				continue;
			}

			if(temp.size() == sameNum){
				cardList.removeAll(temp);
				if (daiNum > 0) {
					ArrayList<Integer> singlecard = this.getSinglecard(cardList, daiNum, (byte) 0,isPair,cardClone);
					if(m_opCardTypeBackUp==XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()){
						//出炸弹
						return singlecard;
					}else{
						//少带
						if(cardList.size()<daiNum){
							temp.addAll(cardList);
							return temp;
						}else {
							if(CollectionUtils.isNotEmpty(singlecard)){
								temp.addAll(singlecard);
							}
							//带的一样张数
							if(temp.size() == this.m_cardList.size()) {
								return temp;
							}
						}
					}
				}else{
					if(temp.size() == sameNum + daiNum) {
						return temp;
					}
				}
			}
			temp.clear();
		}

		return new ArrayList<Integer>();
	}


	/***
	 * 顺子的牌
	 * @param cardList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getShunZi(ArrayList<Integer> cardList){
		if (cardList.size() < 5) {
			return new ArrayList<Integer>();
		}
		ArrayList<Integer> tempCardList = (ArrayList<Integer>) cardList.clone();
		ArrayList< ArrayList<Integer> > outList = new ArrayList< ArrayList<Integer> > ();
		BasePockerLogic.getShunZiByCount(outList, tempCardList, m_cardList.size());

		boolean isNomarlOpCard = true;
		if(m_cardList.size() > 0){
			isNomarlOpCard = false;

			if(outList.size() <= 0) {
				return new ArrayList<Integer>();
			}
		}

		ArrayList<Integer> mCardList = (ArrayList<Integer>) m_cardList.clone();
		mCardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		for(int i = outList.size() - 1 ; i >= 0 ; i--){
			ArrayList<Integer> temp = (ArrayList<Integer>) outList.get(i).clone();
			//判断是否有2
			if(temp.indexOf(new Integer((byte) 0x0F)) >= 0 || temp.indexOf(new Integer((byte) 0x1F))>= 0 || temp.indexOf(new Integer((byte) 0x2F))>= 0 || temp.indexOf(new Integer((byte) 0x3F))>= 0) {
				continue;
			}
			temp.sort(BasePockerLogic.sorterBigToSmallNotTrump);
			if( isNomarlOpCard || ( !isNomarlOpCard && this.compareOneCard(temp.get(0), mCardList.get(0)))){
				if(cardList.containsAll(temp)) {
					return temp;
				}
			}
		}
		return new ArrayList<Integer>();
	}

	/**
	 * 联队
	 * */
	public ArrayList<Integer>   getLianDui (ArrayList<Integer> cardList, int sameNum, int daiNum, PockerValueType pockerType,boolean isPair) {
		ArrayList<Integer> cardClone = (ArrayList<Integer>) cardList.clone();
		if (cardList.size() <  m_cardList.size()&&daiNum==0) {
			return new ArrayList<Integer>();
		}
		ArrayList<ArrayList<Integer>> mCardList = this.getSameCardByList(this.m_cardList, sameNum);
		if(mCardList == null || mCardList.size() < 2){
			CommLogD.error("getLianDui error: sameNum:{}, cardCount:{}", sameNum, m_cardList.size());
			return new ArrayList<Integer>();
		}

		ArrayList<Integer> mShunZiList = new ArrayList<Integer>();
		for (int j = 0; j < mCardList.size(); j++) {
			mShunZiList.add(mCardList.get(j).get(0));
		}

		ArrayList<Integer> tempMShunZilist = this.getShunZiByList2(mShunZiList,this.m_cardList.size()/5);
		if(tempMShunZilist == null || tempMShunZilist.size() < 2){
			CommLogD.error("getLianDui error: sameNum:{}, sequenceCount:{}", sameNum, mShunZiList.size());
			return new ArrayList<Integer>();
		}

		tempMShunZilist.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		int minCard = 0;
		if (tempMShunZilist.size() > 0) {
			minCard = BasePocker.getCardValue(tempMShunZilist.get(tempMShunZilist.size() -1));
		}
		if(daiNum>0){
			//顺子更新
			if(mCardList.size()!=tempMShunZilist.size()){
				mCardList = (ArrayList<ArrayList<Integer>>)mCardList.stream().filter(n->tempMShunZilist.contains(n.get(0))).collect(Collectors.toList());
			}
			//顺子取错
			if(tempMShunZilist.size()*5!=this.m_cardList.size()){
				if(tempMShunZilist.size()>this.m_cardList.size()/5){
					while(tempMShunZilist.size()!=this.m_cardList.size()/5){
						Integer removeCard = BasePockerLogic.getCardValue(tempMShunZilist.remove(tempMShunZilist.size() - 1));
						mCardList = (ArrayList<ArrayList<Integer>>)mCardList.stream().filter(n -> BasePockerLogic.getCardValue(n.get(0)) != removeCard).collect(Collectors.toList());
					}
				}
			}
		}
		ArrayList<Integer> temp = new ArrayList<Integer>();
		cardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		for (int i = pockerType.value(); i < PockerValueType.POCKER_VALUE_TYPE_BOMB.value(); i++) {

			ArrayList< ArrayList<Integer> > opOutList = new ArrayList< ArrayList<Integer> > ();
			int count = BasePockerLogic.getSameCardByType(opOutList, cardList, PockerValueType.valueOf(i));

			if(count < 2) {
				continue;
			}
			ArrayList< ArrayList<Integer> >  lianDuiList = this.getLianDuiList(opOutList,mCardList.size());
			if (lianDuiList == null || lianDuiList.size() < 2) {
				continue;
			}

			ArrayList<Integer> opList = new ArrayList<Integer>();
			for (int j = 0; j < lianDuiList.size(); j++) {
				if(BasePocker.getCardValue(lianDuiList.get(j).get(0)) > minCard) {
					opList.add(lianDuiList.get(j).get(0));
				}
			}

			ArrayList<Integer> tempShunZilist = this.getShunZiByList(opList);
			if(null == tempShunZilist ||  (null != tempShunZilist && tempShunZilist.size() < mCardList.size())) {
				continue;
			}
			for (int j = tempShunZilist.size() ; j >=0; j--) {
				if(j >= tempShunZilist.size()) {
					continue;
				}
				if (BasePocker.getCardValue(tempShunZilist.get(j)) > BasePocker.getCardValue(tempMShunZilist.get(tempMShunZilist.size() - 1 ))) {

					ArrayList<Integer> sameCardList = BasePockerLogic.getSameCard(cardList, tempShunZilist.get(j), true);
					while (sameCardList.size() > sameNum) {
						sameCardList.remove(0);
					}
					temp.addAll(sameCardList);
				}

				if(temp.size() == sameNum * mCardList.size()) {
					break;
				}
			}

			if(temp.size() == sameNum * mCardList.size()){
				cardList.removeAll(temp);

				if (daiNum > 0) {
					int num = this.m_cardList.size() - temp.size();
					ArrayList<Integer> singlecard = this.getSinglecard(cardList, num, (byte) 0, isPair,cardClone);
					if(m_opCardTypeBackUp==XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()){
						//出炸弹
						return singlecard;
					}else{
						//少带
						if(cardList.size()<num){
							temp.addAll(cardList);
							return temp;
						}else {
							if(CollectionUtils.isNotEmpty(singlecard)){
								temp.addAll(singlecard);
							}
							//带的一样张数
							if(temp.size() == this.m_cardList.size()) {
								return temp;
							}
						}
					}
				}else{
					if(temp.size() == sameNum * mCardList.size()) {
						return temp;
					}
				}
			}
		}

		return new ArrayList<Integer>();
	}

	/**
	 * 判断是不是炸弹
	 * **/
	public boolean checkBomb(int opCardType, ArrayList<Integer> list){
		if(XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() != opCardType){
			return false;
		}
		//判断是不是4带类型炸弹（包扣3A），或者普通类型炸弹（包扣3A）
		int fou1ZhaType = getFou1ZhaTypeValue(list);
		if(fou1ZhaType<=0){
			return false;
		}
		if(m_opCardType == opCardType){//上一手是炸弹
			int fou1ZhaType1 = getFou1ZhaTypeValue(m_cardList);
			if (fou1ZhaType1>=fou1ZhaType) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 操作位改变
	 * */
	public void addOpPos(boolean isCalcEndTurn){
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			m_OpPos = (++m_OpPos)%this.room.getPlayerNum();
			XCPDKRoomPos tempRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(m_OpPos);
			if(tempRoomPos.getPrivateCards().size() > 0){
				break;
			}
		}
		this.set.startMS = CommTime.nowMS();
		this.isCheck = false;
		this.set.setOpPos(m_OpPos);
		if(!isCalcEndTurn) {
			return;
		}

		int tempLastOpPos = m_lastOpPos;
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(m_lastOpPos);
		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_XUEZHANDAODI) && roomPos.cards().size() <= 0) {
			for (int i = 0; i < this.room.getPlayerNum(); i++) {
				m_lastOpPos = (++m_lastOpPos)%this.room.getPlayerNum();
				XCPDKRoomPos tempRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(m_lastOpPos);
				if(tempRoomPos.getPrivateCards().size() > 0){
					break;
				}
			}
		}
		if(tempLastOpPos != m_lastOpPos) {
			return;
		}
		if(m_OpPos == m_lastOpPos){
			addBombScore();
			m_bTurnEnd = true;
		}
	}

	/**
	 * 增加炸弹分数
	 */
	private void addBombScore() {
		if(CollectionUtils.isNotEmpty(bombList)){
			set.bombList.add(bombList);
		}
//		if(maxBombPos>=0){
//			if(XCPDK_define.BombAlgorithm.GETROUNDALLBOMB.has(room.getRoomCfg().zhadansuanfa)){
//				this.set.addRoomDouble(maxBombPos, roundBombCount);
//			}else if(XCPDK_define.BombAlgorithm.WINNER.has(room.getRoomCfg().zhadansuanfa)){
//				this.set.addRoomDouble(maxBombPos, 1);
//			}
//		}
//		maxBombPos = -1;
	}

//	/**
//	 * 有炸就算，炸下去立马算分
//	 * @param pos
//	 */
//	private void addBombByAtOnce(int pos) {
//		if(XCPDK_define.BombAlgorithm.ALWAYS.has(room.getRoomCfg().zhadansuanfa)){
//			this.set.addRoomDouble(pos, 1);
//		}
//	}

	/**
	 * 首出 或新一轮首出
	 * @return 带牌数量
	 * **/
	public int   getNomarlTypeCard (int pos , ArrayList<Integer> outList,ArrayList<Integer> intList,ArrayList<Integer> cloneList){
		ArrayList<Integer> cardClone = (ArrayList<Integer>) intList.clone();
		int daiNum = 0;
		if(intList.size() <= 0) {
			return daiNum;
		}
//		ArrayList<Integer> list = new ArrayList<Integer>();
		intList.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		int index = Math.max( intList.size() - 1, 0);
		int size = intList.size();
		for(int a = 0; a < size; a++){
			int card = intList.get(index);
			int count = BasePockerLogic.getCardCount(intList, card, true);
			if (1 == count) {
				if (!this.checkNextIsOneCard(pos, card)) {
					index = Math.max(index - 1, 0);
				}else{
					outList.add(card);
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_WANFA_SINGLECARD.value();
					break;
				}
			} else if(2 == count){
				outList.add(card);
				outList.add(intList.get(index - 1));
				m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_DUIZI.value();
				break;
			}else if(3 == count){
				if (intList.size() == 3) {
					outList.addAll(intList);
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI.value();
				} else {
					outList.add(intList.remove(index));
					outList.add(intList.remove(index - 1));
					outList.add(intList.remove(index - 2));
					if (intList.size() < XCPDKRoomSet.DEFAULTDAINUM) {
						if(intList.size() == XCPDKRoomSet.DEFAULTDAINUM-1) {
							m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI1.value();
//							outList.addAll(intList);
							daiNum = XCPDKRoomSet.DEFAULTDAINUM-1;
						}else if(intList.size() == 0){
							m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI.value();
						}
						for (int i = 0; i < daiNum; i++) {
							if(intList.size()>0){
								outList.add(intList.remove(0));
							}
						}
					} else {
						ArrayList<Integer> cards = this.getSinglecard(intList, XCPDKRoomSet.DEFAULTDAINUM, (byte) 0,true,cardClone);
						if(m_opCardTypeBackUp==XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()){
							outList.clear();
							outList.addAll(cards);
						}else{
							if(cards.size()==0){//找不带带的牌或者对子出3不带
								m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI.value();
							}else{//找到了3带一对
								List<Integer> tempList = cards.size()>=XCPDKRoomSet.DEFAULTDAINUM?cards.subList(0,XCPDKRoomSet.DEFAULTDAINUM):cards;
								outList.addAll(tempList);
								daiNum = XCPDKRoomSet.DEFAULTDAINUM;
								m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3DAI2.value();
							}
						}
					}
				}
				break;
			}else if(4 ==  count && this.set.isFirstOp()){
				outList.add(intList.remove(index));
				outList.add(intList.remove(index - 1));
				outList.add(intList.remove(index - 2));
				List<Integer> cardDaiList = cloneList.stream().filter(z -> BasePocker.getCardValueEx(z) != BasePocker.getCardValueEx(outList.get(0)) && BasePocker.getCardValueEx(z) !=twoValue).collect(Collectors.toList());
				if(CollectionUtils.isNotEmpty(cardDaiList)){
					//4带1炸弹（如果需要不拆3带1这里也可以写上去>=3，并检测带牌，如果不拆连队炸弹，这里也可以检测下是不是满足连队）
					outList.add(intList.remove(index - 3));
					outList.add(cardDaiList.get(0));
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
				}else{
					//将就出3不带算了
					m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_3BUDAI.value();
				}
				break;
			}else{
				index = Math.max(index - count, 0);
			}

		} ;


		if(outList.size() <= 0 && intList.size() > 0){
			outList.add(intList.get(0));
			daiNum = 0;
			m_opCardTypeBackUp = XCPDK_CARD_TYPE.XCPDK_WANFA_SINGLECARD.value();
		}

		return daiNum;
	}

	/**
	 * 获取赖子牌
	 * */
	public ArrayList<Integer> getLaiZiList(ArrayList<Integer> cardList){
		if(!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_LAIZI)){
			//CommLogD.info("getLaiZiList error: is not laizi wanfa");
			return null;
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		int razzValue = BasePocker.getCardValue(this.set.getRazz());
		for (Integer byte1 : cardList) {
			if(BasePocker.getCardValue(byte1) == razzValue){
				list.add(byte1);
			}
		}
		return list;
	}

	/**
	 * 在有赖子的情况的 没有考虑没有赖子的
	 * 根据传入的牌返回顺子
	 * **/
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getShunZiByList(ArrayList<Integer> cardList){
		ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
		ArrayList<Integer> laiziList = getLaiZiList(list);
		if(null != laiziList && laiziList.size() > 0 ) {
			list.removeAll(laiziList);
		}
		list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		for (int i = 0; i < cardList.size() - 1; i++) {
			if( Math.abs( BasePocker.getCardValue( list.get(i) )  - BasePocker.getCardValue( list.get(i+1) ) ) != 1){
				if(laiziList == null || laiziList.size() <= 0){
					return null;
				}
				int card = laiziList.remove(0);
				card = (byte) (BasePocker.getCardColor(list.get(i)) - 1);
				list.add(i+1, card);
			}
		}
		if(list.size() == cardList.size()){
			list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
			return list;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getShunZiByList2(ArrayList<Integer> cardList,int size){
		ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
		ArrayList<Integer> targetList = new ArrayList<>();
		list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		for (int i = 0; i < cardList.size() - 1; i++) {
			if(Math.abs( BasePocker.getCardValue( list.get(i) )  - BasePocker.getCardValue( list.get(i+1) ) ) == 1){
				if(!targetList.contains(list.get(i))){
					targetList.add(list.get(i));
				}
				if(!targetList.contains(list.get(i+1))){
					targetList.add(list.get(i+1));
				}
				if(i+1>=(cardList.size() - 1)){
					if(targetList.size()>=size){
						targetList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
						return targetList;
					}
				}
			}else{
				if(targetList.size()>=size){
					targetList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
					return targetList;
				}
				targetList = new ArrayList<>();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getShunZiByListEx(ArrayList<Integer> cardList, int size){
		ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
		ArrayList<Integer> laiziList = this.getLaiZiList(list);
		if(null != laiziList && laiziList.size() > 0 ) {
			list.removeAll(laiziList);
		}
		list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		ArrayList<Integer> tempList = new ArrayList<Integer>();
		tempList.add(list.get(0));
//		for (int j = 0; j < size; j++) {
		for (int i = 0; i < cardList.size() - 1; i++) {
			if( Math.abs( BasePocker.getCardValue( list.get(i) )  - BasePocker.getCardValue( list.get(i+1) ) ) != 1){
				if(laiziList == null || laiziList.size() <= 0){
					tempList.clear();
					tempList.add(list.get(i+1));
					continue;
//						return null;
				}
				int card = laiziList.remove(0);
				card = (BasePocker.getCardColor(list.get(i)) - 1);
				list.add(i+1, card);
			}else{
				tempList.add(list.get(i+1));
			}
			if (tempList.size() == size) {
				tempList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
				return tempList;
			}
		}
//		}

//		if(list.size() == cardList.size()){
//			list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
//			return list;
//		}
		return null;
	}

	/**
	 * 在有赖子的情况的 没有考虑没有赖子的
	 * 根据传入的牌返回顺子
	 * **/
	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<Integer>> getSameCardByList(ArrayList<Integer> cardList, int sameNum){
		ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
		ArrayList<Integer> laiziList = getLaiZiList(list);
		if(laiziList != null && laiziList.size() > 0) {
			list.removeAll(laiziList);
		}
		ArrayList<ArrayList<Integer>> outList = new ArrayList<ArrayList<Integer>>();
		int count = BasePockerLogic.getPockerEqualValue(outList, list);
		for (int i = 0; i < count; ) {
			if (i >= outList.size()) {
				break;
			}
			int size = outList.get(i).size();

			if(size > sameNum){
//				outList.remove(i);
				while (outList.get(i).size() > sameNum) {
					outList.get(i).remove(0);
				}
				i++;
			}
//			else if (sameNum >  size && (null == laiziList || (null != laiziList && laiziList.size() <= 0) )) {
//				outList.remove(i);
//			}
			else {
				for (int j = 0; j < sameNum - size; j++) {
					if(laiziList == null || laiziList.size() <= 0) {
						break;
					}
					laiziList.remove(0);
					outList.get(i).add(BasePocker.getCardValue(outList.get(i).get(0)));
				}

				if (sameNum < outList.get(i).size()) {
					outList.remove(i);
				}else  if(sameNum > outList.get(i).size()) {
					while (outList.get(i).size() > sameNum) {
						outList.get(i).remove(0);
					}
					i++;
				}else{
					i++;
				}
			}
		}
		if (null != laiziList &&  laiziList.size() >= sameNum) {
			int num = laiziList.size() / sameNum;
			for (int i = 0; i < num; i++) {
				ArrayList<Integer> temp = new ArrayList<Integer>();
				for (int j = 0; j < sameNum; j++) {
					temp.add(laiziList.remove(j));
				}
				outList.add(temp);
			}
		}

		for (int i = 0; i < count; ) {
			if(outList.size() > i &&  outList.get(i).size() < sameNum){
				outList.remove(i);
			}else{
				i++;
			}
		}
		return outList;
	}

	/**
	 * @return m_OpPos
	 */
	public int getOpPos() {
		return m_OpPos;
	}

	/**
	 * @return m_lastOpPos
	 */
	public int getLastOpPos() {
		return m_lastOpPosBack;
	}

	/**
	 * @return m_opCardType
	 */
	public int getOpCardType() {
		return m_opCardType;
	}

	/**
	 * @return m_cardList
	 */
	public ArrayList<Integer> getCardList() {
		return m_cardList;
	}

	/**
	 * @return m_bSetEnd
	 */
	public boolean isSetEnd() {
		return m_bSetEnd;
	}


	/**
	 * 判断牌有没有大牌
	 * */
	@SuppressWarnings("unchecked")
	public boolean checkHaveMaxCard(int pos){
		XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);

		if(roomPos.getPrivateCards().size() <= 0){
			return false;
		}

		ArrayList<Integer> tempCard = (ArrayList<Integer>) roomPos.cards().clone();
		tempCard.sort(BasePockerLogic.sorterBigToSmallNotTrump);

		ArrayList<Integer> outList = new ArrayList<Integer>();
		int daiNum = 0;
		int opCardType = m_opCardType;
		m_opCardTypeBackUp = m_opCardType;
		switch (XCPDK_CARD_TYPE.valueOf(m_opCardType)) {
			case XCPDK_CARD_TYPE_DUIZI:  			//对子
			{
				outList = this.getSameCard(tempCard, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB,false);
			}
			break;
			case XCPDK_CARD_TYPE_3BUDAI:  		//3不带
			{
				outList = this.getSameCard(tempCard, 3, 0, PockerValueType.POCKER_VALUE_TYPE_THREE,false);
			}
			break;
//			case XCPDK_CARD_TYPE_3DAI1: 			//3带1
//			{
//				daiNum = 1;
//				outList = this.getSameCard(tempCard, 3, 1, PockerValueType.POCKER_VALUE_TYPE_THREE,false);
//			}
//			break;
			case XCPDK_CARD_TYPE_3DAI2:  			//3带2
			{
				daiNum = 2;
				outList = this.getSameCard(tempCard, 3, 2, PockerValueType.POCKER_VALUE_TYPE_THREE,true);
			}
			break;
//			case XCPDK_CARD_TYPE_4DAI1:  			//4带1
//			{
//				daiNum = 1;
//				outList = this.getSameCard(tempCard, 4, 1, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI2:  			//4带2
//			{
//				daiNum = 2;
//				outList = this.getSameCard(tempCard, 4, 2, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
//			case XCPDK_CARD_TYPE_4DAI3:  			//4带3
//			{
//				daiNum = 3;
//				outList = this.getSameCard(tempCard, 4, 3, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
			case 	XCPDK_CARD_TYPE_ZHADAN:  			//炸弹
			{
//				outList = this.getSameCard(tempCard, 4, 0, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
				outList = getBombByList(tempCard,getFou1ZhaTypeValue(m_cardList));
			}
			break;
			case XCPDK_CARD_TYPE_SHUNZI:  		//顺子
			{
				outList = this.getShunZi(tempCard);
			}
			break;
			case 	XCPDK_CARD_TYPE_FEIJI3:  			//飞机
			{
				if(m_cardList.size()%3==0){
					daiNum = 0;
				}else{
					daiNum = XCPDKRoomSet.DEFAULTDAINUM;
				}
				outList = this.getLianDui(tempCard, 3, daiNum, PockerValueType.POCKER_VALUE_TYPE_THREE,true);
			}
			break;
//			case 	XCPDK_CARD_TYPE_FEIJI4:  //飞机带翅膀
//			{
//				daiNum = XCPDKRoomSet.DEFAULTDAINUM+1;
//				outList = this.getLianDui(tempCard, 4, daiNum, PockerValueType.POCKER_VALUE_TYPE_BOMB,false);
//			}
//			break;
			case 	XCPDK_WANFA_LIANDUI:  //联队
			{
				outList = this.getLianDui(tempCard, 2, 0, PockerValueType.POCKER_VALUE_TYPE_SUB,false);
			}
			break;
			case 	XCPDK_WANFA_SINGLECARD:
			{
				outList = this.getSinglecard(tempCard, 1,  m_cardList.size() > 0 ? m_cardList.get(0) : Integer.valueOf((byte) 0),false,(ArrayList<Integer>) tempCard.clone());
				if(m_opCardTypeBackUp != XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value()) {
					if (null != outList && outList.size() > 0 && tempCard.size() > 0) {
						if (!this.checkNextIsOneCard(pos, outList.get(0))) {
							tempCard.sort(BasePockerLogic.sorterBigToSmallNotTrump);
							outList.clear();
							if (tempCard.size() > 0) {
								outList.add(tempCard.get(0));
							}
						}
					}
				}
			}
			break;
			default:
				CommLogD.error("not find opTYpe ="+m_opCardType +","+ XCPDK_CARD_TYPE.valueOf(m_opCardType));
				break;
		}
		opCardType = m_opCardTypeBackUp;


		if (CollectionUtils.isEmpty(outList)) {
			outList = getBombByList(tempCard,XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value() == opCardType?getFou1ZhaTypeValue(m_cardList):0);
			if(CollectionUtils.isNotEmpty(outList)){
				opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_ZHADAN.value();
			}else{
				opCardType = XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value();
			}
		}

		if (CollectionUtils.isEmpty(outList)) {
			return false;
		}
		return true;
	}

	/**
	 * 获取托管列表
	 * @return
	 */
	public List<Boolean> getTrusteeshipList(){
		return room.getRoomPosMgr().getPlayingPos().stream().map(AbsRoomPos::isTrusteeship).collect(Collectors.toList());
	}

	/**
	 * 在有赖子的情况的 没有考虑没有赖子的
	 * 根据传入的牌返回顺子
	 * **/
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getBombByList(ArrayList<Integer> cardList, int cardValue){
		Map<Integer, List<Integer>> cardGroup = cardList.stream().collect(Collectors.groupingBy(BasePocker::getCardValueEx));
		if(MapUtils.isNotEmpty(cardGroup)){
			for (Map.Entry<Integer, List<Integer>> entry : cardGroup.entrySet()) {
				if(entry.getValue().size()>=3){
					List<Integer> daiList = cardList.stream().filter(z -> BasePocker.getCardValueEx(z) != entry.getKey() && BasePocker.getCardValueEx(z) != twoValue).collect(Collectors.toList());
					if(CollectionUtils.isNotEmpty(daiList)){
						//4带1炸弹,333+x炸弹 AAA+x炸弹
						List<Integer> unionList = ListUtils.union(Arrays.asList(daiList.get(0)),entry.getValue());
						if(getFou1ZhaTypeValue(unionList)>cardValue){
							return (ArrayList<Integer>)unionList;
						}
					}
				}
			}
		}

		//用连对炸弹
		for (int i = 8; i >= 5; i--) {
			if(cardValue>20){
				int length = cardValue / 20 + 4;
				if(length<=i){
					List<Integer> straight = getStraight(cardList, i , 2, cardValue,false,true);
					if(CollectionUtils.isNotEmpty(straight)){
						return (ArrayList<Integer>)straight;
					}
				}
			}else{
				List<Integer> straight = getStraight(cardList, i , 2, cardValue,false,true);
				if(CollectionUtils.isNotEmpty(straight)){
					return (ArrayList<Integer>)straight;
				}
			}
		}

		return new ArrayList<>();
	}

	/**
	 * 获取炸弹值
	 *
	 * @param cardList 卡列表
	 * @return int
	 */
	public int getFou1ZhaTypeValue(List<Integer> cardList){
		Map<Integer, Long> cardGroup = cardList.stream().collect(Collectors.groupingBy(BasePocker::getCardValue, Collectors.counting()));
		if(cardGroup.values().stream().allMatch(z->z==2)){
			if(cardList.size()/2<5){
				return 0;
			}
			if(getStraight((ArrayList<Integer>) cardList,cardList.size()/2,2,0,false,false).size()<=0){
				return 0;
			}
			//5连队炸弹(23-34) value/20=1是5=1+4连，value%20代表某连的值
			//6连队炸弹(43-54) value/20=2是6=2+4连，value%20代表某连的值
			//7连队炸弹(63-74) value/20=3是7=3+4连，value%20代表某连的值
			//8连队炸弹(83-94) value/20=4是8=4+4连，value%20代表某连的值
			int color = cardList.size() / 2;
			List<Integer> sortKeyList = cardGroup.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
			return (color-4) * 20 + sortKeyList.get(0);
		}
		if(cardList.stream().anyMatch(z->BasePocker.getCardValueEx(z)==twoValue)){
			return 0;
		}
		//2,14
		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3AZHA)){
			if(cardList.stream().filter(n->BasePocker.getCardValue(n)== BasePocker.getCardValue(AVAULE)).count()==3 &&cardList.size()==4){
				if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_3AZHAMAX)){
					return 14;
				}
				return 2;
			}
		}
		//1
		if(this.room.is333Zha()){
			//333+1非333+3
			if(cardList.containsAll(threeList) && cardList.size()==4 && cardGroup.containsValue(3L)){
				return 1;
			}
		}
		//3-13
		if(cardGroup.containsValue(4L) && cardList.size()==5){
			Optional<Map.Entry<Integer, Long>> first = cardGroup.entrySet().stream().filter(z -> z.getValue() >= 4).findFirst();
			return first.map(Map.Entry::getKey).orElse(0);
		}
		return 0;
	}

	/**
	 * 获取顺子
	 *
	 * @param cardList     牌列表
	 * @param bodyLength   顺子长度
	 * @param bodyNum      顺子个数,连队2，顺子1
	 * @param compareValue 比较值
	 * @return 顺子
	 */
	public List<Integer> getStraight(ArrayList<Integer> cardList, int bodyLength, int bodyNum, int compareValue,boolean needEqual,boolean positiveOrder) {
		cardList = (ArrayList<Integer>) cardList.stream().filter(n -> BasePocker.getCardValueEx(n)!=twoValue).collect(Collectors.toList());
		Map<Integer, List<Integer>> valueListMap = cardList.stream().collect(Collectors.groupingBy(BasePocker::getCardValueEx));
		Integer[] keys = valueListMap.keySet().toArray(new Integer[0]);
		Arrays.sort(keys);
		for (int i = 0; i <= keys.length - bodyLength; i++) {
			List<Integer> straightList = new ArrayList<>(bodyLength * bodyNum);
			boolean isEqual;
			try{
				isEqual = ((!needEqual&&valueListMap.get(keys[i]).size() >= bodyNum)||needEqual&&valueListMap.get(keys[i]).size() == bodyNum);
			}catch (Exception e){
				BaseDao.stackTrace();
				throw e;
			}
			if (isEqual) {
				straightList.addAll(valueListMap.get(keys[i]).subList(0, bodyNum));
				for (int j = 1; j < bodyLength; j++) {
					int before = i + j - 1;
					int current = i + j;
					if (Math.abs(keys[current] - keys[before]) != 1) {
						break;
					}
					if (!needEqual && valueListMap.get(keys[current]).size() < bodyNum) {
						break;
					}
					if (needEqual && valueListMap.get(keys[current]).size() != bodyNum) {
						break;
					}
					straightList.addAll(valueListMap.get(keys[current]).subList(0, bodyNum));
				}
				if (straightList.size() != bodyLength * bodyNum) {
					continue;
				}
				int color = straightList.size() / 2;
				int nowValue = (color-4) * 20 + BasePocker.getCardValueEx(straightList.get(0));
				if (nowValue > compareValue) {
					return straightList;
				}
			}
		}
		return new ArrayList<>();
	}
}
