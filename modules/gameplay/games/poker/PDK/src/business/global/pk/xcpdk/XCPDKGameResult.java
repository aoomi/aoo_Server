package business.global.pk.xcpdk;

import business.xcpdk.c2s.cclass.XCPDK_Bomb;
import business.xcpdk.c2s.cclass.XCPDK_define;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_WANFA;
import cenum.RoomTypeEnum;
import jsproto.c2s.cclass.pk.Victory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class XCPDKGameResult {
	public XCPDKRoom room; //房间


	public  XCPDKGameResult(XCPDKRoom room){
		this.room = room;
	}

	/**
	 * 结算
	 * */
	public  void resultCalc(){
		XCPDKRoomSet set = (XCPDKRoomSet) this.room.getCurSet();
		int winPos = -1;
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if(set.resultCalcList.get(i)) {
                continue;
            }
			XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
			if ( roomPos.cards().size() <= 0 ) {
				winPos = i;
				break;
			}
		}

		if(winPos == -1) {
            return;
        }
		int bombBeiShu = 0;
		switch (XCPDK_define.BombScore.valueOf(room.getRoomCfg().zhadanfenshu)){
			case ADD_TEN:
				bombBeiShu=10;
				break;
			case ADD_FIVE:
				bombBeiShu=5;
				break;
		}
		//这里加炸弹哦(只有赢家得炸弹分)
		for (List<XCPDK_Bomb> item : set.bombList) {
			int score = 0;
			int chiNum = 0;
			for(int i=item.size()-1;i>=0;i--){
				if(chiNum<=0){
					if(item.get(i).pos==winPos){
						int addNum = bombBeiShu;
						if(item.get(i).isLast){
							addNum*=2;
						}
						if(item.get(i).isLianDui){
							addNum*=2;
						}
						chiNum=i+1;
						score+=addNum;
					}
				}else{
					int addNum = bombBeiShu;
					if(item.get(i).isLast){
						addNum*=2;
					}
					if(item.get(i).isLianDui){
						addNum*=2;
					}
					score+=addNum;
				}
			}
			if(chiNum>0){
				set.addNewRoomDouble(winPos, chiNum,score);
			}
		}
		//这里算关门
		int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
		long count = room.getRoomPosMgr().getRoomPosList().stream().filter(z -> ((XCPDKRoomPos) z).getPrivateCards().size() == cardNum).count();
		if(count==room.getPlayerNum()-1){
			//关门了
			for (int i = 0; i < this.room.getPlayerNum(); i++) {
				if(winPos!=i){
					set.beShutDowList.set(i, true);
				}
			}
		}
		set.resultCalcList.set(winPos, true);
		if (this.room.getConfigMgr().getJiPaiFen() == this.room.getRoomCfg().resultCalc) {
			this.onJiPaiFenCalc(winPos);
		} else if (this.room.getConfigMgr().getGuDingFen() ==this.room.getRoomCfg().resultCalc){
			this.onGuDingFenCalc(winPos);
		}else if (this.room.getConfigMgr().getPaiDuoTongShu() ==this.room.getRoomCfg().resultCalc){
			this.onPaiDuoTongShuCalc(winPos);
		}
	}


	/**
	 * 记牌分
	 * */
	public  void onJiPaiFenCalc(int pos){
		XCPDKRoomSet set = (XCPDKRoomSet) this.room.getCurSet();
		int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
		int robCloseAddDouble = this.room.getConfigMgr().getRobCloseAddDouble();
		int winTimer = Math.max(1, set.getAddDoubleNum(pos));

		//是否需要算分
		List<Boolean> needCalPoint = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), true));
		if (set.room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(set.room.getRoomTypeEnum())) {
			for (int i = 0; i < this.room.getPlayerNum(); i++) {
				XCPDKRoomPos posByPosID = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
				if(posByPosID.getGameBeginSportsPoint()<=0){
					needCalPoint.set(i,false);
				}else{
					needCalPoint.set(i,true);
				}
			}
		}
		boolean existDaGuan = set.beShutDowList.stream().anyMatch(z -> z);
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if(i == pos) {
                continue;
            }
			if(set.resultCalcList.get(i)) {
                continue;
            }
			//如果你当局带的身上竞技点小于0不能算赢的牌型分等
			if(needCalPoint.get(pos)){
				XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
				int num = Math.max(1, set.getAddDoubleNum(i)) * winTimer;

//				if (!set.isRobCloseCalc()) {
//				/*if (i == set.getFirstOpPos() && set.surplusCardList.get(i) == set.getFirstOpNum()) {
//					num *= robCloseAddDouble;
//				} else */if(set.surplusCardList.get(i) == cardNum){
//						num *= robCloseAddDouble;
//					}
//				}


				if(set.beShutDowList.get(i)){
					//被关门了
					num *= 3;
				}

				if(!existDaGuan){
					if(roomPos.cards().size()>=cardNum-3){
						//小关
						set.xiaoGuanList.set(i,true);
						num *= 2;
					}
				}

				if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_WEIZHANGSUANFEN)){
					num *=   roomPos.cards().size() <= 0 ? 0 : roomPos.cards().size();//尾张算分
				}else{
					num *=  (roomPos.cards().size() <= 0 || roomPos.cards().size() == 1) ? 0 : roomPos.cards().size();//尾张不算分
				}

				//没有红桃十炸鸟
//				boolean isHongTaoShiZhaNiao = set.zhaNiao.get(i) || set.zhaNiao.get(pos);
//				num *= (isHongTaoShiZhaNiao?2:1);
				/*// 双倍得分
				if (XCPDK_define.BombScore.DOUBLE_.has(room.getRoomCfg().zhadanfenshu)) {
					num *= Math.max(1, getBombScore(pos,false));
					// 加10分
				} else */if (XCPDK_define.BombScore.ADD_TEN.has(room.getRoomCfg().zhadanfenshu)) {
					num += getBombScore(pos,true);
				}else if (XCPDK_define.BombScore.ADD_FIVE.has(room.getRoomCfg().zhadanfenshu)) {
					num += getBombScore(pos,true);
				}

				set.pointList.set(pos, set.pointList.get(pos) + num);
				set.pointList.set(i, set.pointList.get(i) - num);
			}

//			//输家身上的炸弹算分
//			if (XCPDK_define.BombScore.ADD_TEN.has(room.getRoomCfg().zhadanfenshu) || XCPDK_define.BombScore.ADD_FIVE.has(room.getRoomCfg().zhadanfenshu)) {
//				//如果你当局带的身上竞技点小于0不能算赢的炸弹分
//				if(!needCalPoint.get(i)){
//					continue;
//				}
//				otherCostScore(i,getBombScore(i,true));
//			}
		}
	}

	/**
	 * 其他人扣分，自己加分
	 * @param pos
	 * @param score
	 */
	private void otherCostScore(int pos,int score) {
		if(room.getCurSet()!=null ){
			XCPDKRoomSet_FJ set = ((XCPDKRoomSet_FJ)room.getCurSet());
			for (int i = 0; i < this.room.getPlayerNum(); i++) {
				if(pos == i) continue;
				set.pointList.set(pos, set.pointList.get(pos) + score);
				set.pointList.set(i, set.pointList.get(i) - score);
			}
		}
	}

	/**
	 * 获取炸弹得分
	 * @param winnerPos
	 * @param needSelfCal 是否算的是自己的分
	 * @return
	 */
	private int getBombScore(int winnerPos,boolean needSelfCal) {
		XCPDKRoomSet_FJ roomSet = (XCPDKRoomSet_FJ) this.room.getCurSet();
		if(needSelfCal){
			return roomSet.roomDouble.stream().filter(z->z.getPos()==winnerPos).map(XCPDKVictory::bombScoreOf).findFirst().orElse(0);
		}else{
			return roomSet.roomDouble.parallelStream()
					.mapToInt(XCPDKVictory::bombScoreOf).sum();
		}
//		switch (XCPDK_define.BombAlgorithm.valueOf(room.getRoomCfg().zhadansuanfa)) {
//			case ALWAYS:
//				if(needSelfCal){
//					bombScoreHandler.accept(roomSet.getNumByList(roomSet.roomDouble, winnerPos));
//				}else{
//					roomSet.roomDouble.parallelStream()
//							.map(Victory::getNum)
//							.forEach(bombScoreHandler);
//				}
//				break;
//			case WINNER:
//				if(needSelfCal){
//					bombScoreHandler.accept(roomSet.getNumByList(roomSet.roomDouble, winnerPos));
//				}else{
//					roomSet.roomDouble.parallelStream()
//							.map(Victory::getNum)
//							.forEach(bombScoreHandler);
//				}
//				break;
//			case GETROUNDALLBOMB:
//				if(needSelfCal){
//					bombScoreHandler.accept(roomSet.getNumByList(roomSet.roomDouble, winnerPos));
//				}else{
//					roomSet.roomDouble.parallelStream()
//							.map(Victory::getNum)
//							.forEach(bombScoreHandler);
//				}
//				break;
//		}
	}

	/**
	 * 固定分
	 * */
	public void onGuDingFenCalc(int pos){
		XCPDKRoomSet set = (XCPDKRoomSet) this.room.getCurSet();
		int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
		int robCloseAddDouble = this.room.getConfigMgr().getRobCloseAddDouble();
		ArrayList<Integer> doubleList = this.getLoseList(pos);
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if(i == pos) {
                continue;
            }
			int num =  Math.max(1, set.getAddDoubleNum(pos));
			int addDoubleI = Math.max(1, set.getAddDoubleNum(i));
			num *= addDoubleI  * Math.max(1, set.getRoomDouble(pos));

			if (!set.isRobCloseCalc()) {
				/*if (i == set.getFirstOpPos() && set.surplusCardList.get(i) == set.getFirstOpNum()) {
					num *= robCloseAddDouble;
				} else*/ if(set.surplusCardList.get(i) == cardNum){
					num *= robCloseAddDouble;
				}
			}

			if (this.room.getRoomCfg().maxAddDouble > 0) {
				num = Math.min(this.room.getConfigMgr().getMaxAddDoubleList().get(this.room.getRoomCfg().maxAddDouble), num);
			}
			num *=   doubleList.get(i) ;

			set.pointList.set(pos, set.pointList.get(pos) + num);
			set.pointList.set(i, set.pointList.get(i) - num);
		}
	}

	/**
	 * 牌多通输
	 * */
	public  void onPaiDuoTongShuCalc(int pos){
		this.onJiPaiFenCalc(pos);

		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_XUEZHANDAODI)) {
            return;
        }

		XCPDKRoomSet set = (XCPDKRoomSet) this.room.getCurSet();
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if(i == pos ) {
                continue;
            }
			if(set.resultCalcList.get(i)) {
                continue;
            }
			XCPDKRoomPos roomPosI = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
			for (int j = i; j < this.room.getPlayerNum(); j++) {
				if(i == j) {
                    continue;
                }
				if(j == pos) {
                    continue;
                }
				if(set.resultCalcList.get(j)) {
                    continue;
                }
				int num = Math.max(1, set.getAddDoubleNum(i));
				XCPDKRoomPos roomPosJ = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(j);
				boolean isWin = roomPosI.cards().size() - roomPosJ.cards().size() >= 0 ? false : true;
				num *= Math.max(1, set.getAddDoubleNum(j)) * Math.max(1, set.getRoomDouble(pos));

				if (this.room.getRoomCfg().maxAddDouble > 0) {
					num = Math.min(this.room.getConfigMgr().getMaxAddDoubleList().get(this.room.getRoomCfg().maxAddDouble), num);
				}
				num *=   Math.abs( roomPosI.cards().size() - roomPosJ.cards().size() ) ;

				set.pointList.set(j, set.pointList.get(j) + num * (isWin ? -1 : 1));
				set.pointList.set(i, set.pointList.get(i) + num * (isWin ? 1 : -1));
			}
		}
	}


	/*
	 * 获取输家排序
	 * **/
	public ArrayList<Integer> getLoseList(int winPos){
		int playerNum = this.room.getPlayerNum();
		ArrayList<Integer> list = new ArrayList<Integer>(Collections.nCopies(playerNum, 0));
		ArrayList<Victory> cardList = new ArrayList<Victory>();
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if(i == winPos) {
                continue;
            }
			XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
			cardList.add(new Victory(i, roomPos.cards().size()));
		}
		cardList.sort(sorter);
		for (int i = 1; i <= cardList.size(); ) {
			Victory victory  = cardList.get(i-1);
			if(null != victory){
				int count = this.getSameCardNum(cardList, victory.getNum());
				for (int j = 0; j < count; j++) {
					Victory vic = cardList.get(i-1+j);
					if(null != vic) {
                        list.set(vic.getPos(), this.getTimesByLoseNum(i+count-1));
                    }
				}
				i += count;
			}else{
				++i;
			}
		}
		return list;
	}

	/*
	 * 排序 大到小
	 */
	public  Comparator<Victory> sorter = (left, right) -> {
		return  left.getNum() -  right.getNum();
	};

	/*
	 * 获取剩余牌相同玩家个数
	 * **/
	public int getSameCardNum(ArrayList<Victory> cardList, int sameCardNum){
		int count = 0;
		for (Victory victory : cardList) {
			if (null != victory) {
				if (sameCardNum == victory.getNum()) {
					count++;
				}
			}
		}
		return count ;
	}

	/*
	 * 计算分数
	 * **/
//	public int calcPoint(int pos, int size){
//		int point = 0;
//		XCPDKRoomSet set = (XCPDKRoomSet) this.room.getCurSet();
//		int num = Math.max(1, set.getAddDoubleNum(pos));
//		//point = size * this.getSurplusNum(size) * num * set.getRoomDouble();
//		point = size  * num * set.getRoomDouble();
//		return point;
//	}

//	/*
//	 *根据牌数计算倍数 
//	 * **/
//	public int getSurplusNum(int size){
//		int num = 1;
//		int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().cardNum);
//		if (size == cardNum) {
//			num = 4;
//		} else if(size < cardNum &&  size >= cardNum*3/4){
//			num = 3;
//		}else if(size < cardNum*3/4 &&  size >= cardNum/2){
//			num = 2;
//		}
//		return num;
//	}

	/*
	 * 根据输家排位获取倍数
	 * */
	public int  getTimesByLoseNum(int num) {
		int time = 1;
		switch (num) {
		case 1:
			time = 1;
			break;
		case 2:
			time = 2;
			break;
		case 3:
			time = 3;
			break;
		default:
			time = 1;
			break;
		}
		return time;
	}
}
