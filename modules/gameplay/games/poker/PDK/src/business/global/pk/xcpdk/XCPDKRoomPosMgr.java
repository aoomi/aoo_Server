package business.global.pk.xcpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.player.Robot.RobotMgr;
import com.ddm.server.common.utils.CommTime;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 红中麻将 房间内每个位置信息
 * 
 * @author Clark
 *
 */

public class XCPDKRoomPosMgr extends AbsRoomPosMgr {

	public XCPDKRoomPosMgr(AbsBaseRoom room) {
		super(room);
	}

	@Override
	protected void initPosList() {
		// 初始化房间位置
		for (int posID = 0; posID < this.getPlayerNum(); posID++) {
			this.posList.add(new XCPDKRoomPos(posID, room));
		}
	}

	
	//获取牌的位置
	public int checkCard(Integer card){
		int pos = -1;
		for (AbsRoomPos roomPosDelegateAbstract : posList) {
			XCPDKRoomPos roomPos = (XCPDKRoomPos) roomPosDelegateAbstract;
			if(roomPos.checkCard(card)){
				pos = roomPos.getPosID();
				break;
			}
		}
		return pos;
	}
	/**
	 * 获取所有玩家的牌
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<Integer>> getAllPlayBackNotify() {
		return (ArrayList<ArrayList<Integer>>) this.getPosList().stream()
				.map(k -> (ArrayList<Integer>) ((XCPDKRoomPos) k).getPrivateCards().clone()).collect(Collectors.toList());
	}

	/**
	 * 清理开始标识
	 */
	public void clearBeginFlag(){
		if (CollectionUtils.isEmpty(this.getPosList())) {
			// 玩家信息列表没数据
			return;
		}
		// 遍历通知所有玩家
		this.getPosList().forEach(key -> {
			if (Objects.nonNull(key)) {
				((XCPDKRoomPos)key).setBeginFlag(false);
			}
		});
	}

	/**
	 * 检查用户超时
	 */
	@Override
	public void checkOverTime(int ServerTime) {
		if (ServerTime == 0) {
			return;
		}
		for (AbsRoomPos pos : this.getPosList()) {
			if (Objects.isNull(pos) || pos.getPid() <= 0L) {
				continue;
			}
			if (pos.getLatelyOutCardTime() <= 0) {
				continue;
			}
			if (pos.isTrusteeship()) {
				continue;
			}
			if (pos.isRobot() && CommTime.nowMS() > pos.getLatelyOutCardTime() + RobotMgr.getInstance().getThinkTime()) {
				this.getRoom().RobotDeal(pos.getPosID());
				continue;
			}

			boolean enCanTru = false;
			if(((XCPDKRoom) room).getRoomCfg().getFangJianXianShi() != 0){
				XCPDKRoomSet curSet = (XCPDKRoomSet) room.getCurSet();
				if(curSet!=null){
					long total = curSet.getTime(((XCPDKRoom) getRoom()).getRoomCfg().getFangJianXianShi());
					int time = room.getCurSet() != null ? ((XCPDKRoomSet) room.getCurSet()).getTime1(pos.getPosID()) : -1;
					if(time!=-1&&total<=time){
						enCanTru = true;
					}
				}
			}
			if (CommTime.nowMS() > pos.getLatelyOutCardTime() + ServerTime || enCanTru) {
				pos.setLatelyOutCardTime(CommTime.nowMS());
				if (Objects.nonNull(this.getRoom())) {
					// 启动定时器
					this.getRoom().startTrusteeShipTime();
				}
				pos.setTrusteeship(true, false);
				if (Objects.nonNull(this.getRoom())) {
					if(room.needAtOnceOpCard()){
						room.roomTrusteeship(pos.getPosID());
					}
				}
			}
		}

	}
}
