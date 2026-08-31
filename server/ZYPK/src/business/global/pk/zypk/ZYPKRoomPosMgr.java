package business.global.pk.zypk;

import java.util.ArrayList;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;

public class ZYPKRoomPosMgr extends AbsRoomPosMgr {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ZYPKRoomPosMgr(AbsBaseRoom room) {
		super(room);
	}

	@Override
	protected void initPosList() {
		for (int i = 0; i < getPlayerNum(); i++) posList.add(new ZYPKRoomPos(i, room));
	}

	@Override
	public void checkOverTime(int serverTime) {
		// ZYPK has no legacy trusteeship action; room lifecycle still invokes this hook.
	}

	//获取牌的位置
	public int checkCard(Integer card){
		int pos = -1;
		for (AbsRoomPos roomPosDelegateAbstract : posList) {
			ZYPKRoomPos roomPos = (ZYPKRoomPos) roomPosDelegateAbstract;
			if(roomPos.checkCard(card)){
				pos = roomPos.getPosID();
				break;
			}
		}
		return pos;
	}
	
//	//获取牌的位置 -- 检查手上是否有过某张牌
//	public int checkCardHasFocus(Integer card){
//		int pos = -1;
//		for (RoomPosDelegateAbstract<?> roomPosDelegateAbstract : posList) {
//			PDKRoomPos<?> roomPos = (PDKRoomPos<?>) roomPosDelegateAbstract;
//			if(roomPos.checkCardHasFocus(card)){
//				pos = roomPos.posID;
//				break;
//			}
//		}
//		return pos;
//	}
	
	/**
	 * 获取所有玩家的牌
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public  ArrayList<ArrayList<Integer>> getAllPlayBackNotify(){
		ArrayList<ArrayList<Integer>> cardList = new ArrayList<ArrayList<Integer>>();
		for (AbsRoomPos roomPosDelegateAbstract : posList) {
			ZYPKRoomPos roomPos = (ZYPKRoomPos) roomPosDelegateAbstract;
			cardList.add((ArrayList<Integer>) roomPos.getPrivateCards().clone());
		}
		return cardList;
	}
}
