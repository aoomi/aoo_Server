package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPKRoom_RoundPos;
import jsproto.c2s.cclass.zypk.ZYPKRoom_SetRound;
import jsproto.c2s.cclass.zypk.ZYPKSet_Pos;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;
import jsproto.c2s.iclass.zypk.SZYPK_PosOpCard;
import jsproto.c2s.iclass.zypk.SZYPK_StartRound;

import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

/**
 * 自由扑克一局游戏逻辑
 * 
 * @author zaf
 *
 */

public class ZYPKSetSound {

	protected ZYPKRoomSet set;
	protected ZYPKRoom<?> room;
	protected int roundID;
	protected int startTime;
	protected int endTime;
	protected Hashtable<Integer, ZYPKRoundPos> roundPosDict = new Hashtable<>();
	protected Op_Player opType = null; // 是否操作出牌
	protected ZYPKSetSound waitDealRound = null; // 等待接受处理的round
	protected int waitOpPos;
	private ZYPKRoom_SetRound ret;

	public ZYPKRoomSet getSet() {
		return set;
	}

	public int getWaitOpPos() {
		return waitOpPos;
	}

	public Op_Player getOpType() {
		return opType;
	}

	public void setOpType(Op_Player opType) {
		this.opType = opType;
	}


	/**
	 * 清空
	 */
	public void clear() {
		this.roundID = 1;
		this.roundPosDict.clear();
		this.waitDealRound = null;
	}

	/**
	 * 获取操作位置
	 * 
	 * @return
	 */
	public int getOpPos() {
		int ret = -1;
		for (int pos : this.roundPosDict.keySet()) {
			ret = pos;
			break;
		}
		return ret;
	}

	/**
	 * 获取这回合的操作
	 * 
	 * @return
	 */
	public ZYPKRoundPos getOpPosData() {
		ZYPKRoundPos ret = null;
		for (ZYPKRoundPos pos : this.roundPosDict.values()) {
			ret = pos;
			break;
		}
		return ret;
	}

	/**
	 * 获得前一轮
	 * 
	 * @return
	 */
	public ZYPKSetSound getPreRound() {
		if (this.roundID - 2 < 0)
			return null;
		return this.set.getHistoryRound(this.roundID - 2);
	}

	public void tryEndRound(boolean isHu) {
		boolean allOp = true;

		// 遍历检查是否每个人都执行完毕了
		for (ZYPKRoundPos pos : this.roundPosDict.values()) {
			if (pos.getOpType() == null) {
				allOp = false;
				break;
			}
		}
		if (allOp) {
			this.endTime = CommTime.nowSecond();
		}
	}

//	@SuppressWarnings("unused")
//	public boolean update(int sec) {
//		boolean isEnd = false;
//		// 已经结束
//		if (this.endTime != 0) {
//			if (this.endTime >= this.startTime) {
//				return true;
//			}
//			return false;
//		}
//		// 操作延时，执行默认操作
//		if (MaxOpWaitTime != 0
//				&& sec > startTime + startPlayTime + MaxOpWaitTime) {
//			for (ZYPKRoundPos roundPos : roundPosDict.values()) {
//				roundPos.op_Default();
//			}
//			return false;
//		}
//
//		if (debugWait)
//			return false;
//		return isEnd;
//	}

	/**
	 * 获取本轮信息
	 * 
	 * @param pos
	 *            位置
	 * @param isT
	 *            托管状态下判断
	 * @return
	 */

	public ZYPKRoom_SetRound getNotify_RoundInfo(int pos) {
		ret = new ZYPKRoom_SetRound();
		ret.setWaitID(this.roundID);
		ret.setStartWaitSec(this.startTime);
		for (ZYPKRoundPos roundPos : this.roundPosDict.values()) {
			// 自己 或 公开
			if (pos == roundPos.getOpPos() || roundPos.isPublicWait()) {
				ZYPKRoom_RoundPos data = new ZYPKRoom_RoundPos();
				data.setOpList(roundPos.getRecieveOpTypes());
				data.setWaitOpPos(roundPos.getOpPos());
				this.waitOpPos = roundPos.getOpPos();
				ret.addOpPosList(data);
			}
		}
		return ret;
	}

	public ZYPKSetSound(ZYPKRoomSet set, int roundID) {
		this.roundID = roundID;
		this.set = set;
		this.room = set.getRoom();
		this.startTime = CommTime.nowSecond();
	}

	/**
	 * 通知下一个操作者
	 */
	@SuppressWarnings("rawtypes")
	public void notifyStart() {
		if (!this.room.isLunLiu()) 
			return;
		SZYPK_StartRound other = SZYPK_StartRound.make(
				this.room.getRoomID(), getNotify_RoundInfo(-1));
		for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
			if (!this.set.getPlayerList().get(posID))
				continue;
			if (this.roundPosDict.containsKey(posID)) {
				SZYPK_StartRound joinner = SZYPK_StartRound.make(
						this.room.getRoomID(), getNotify_RoundInfo(posID));
				this.room.notify2Pos(posID, joinner);
			} else {
				this.room.notify2Pos(posID, other);
			}
		}
	}

	public void opCard(WebSocketRequest request, int opPos,Op_Player opType,ZYPK_AnNiu onAnNiu,int comporePos) {
		ZYPKRoundPos pos = this.roundPosDict.get(opPos);
		if (null == pos) {
			request.error(ErrorCode.NotAllow, "opPos has no round power");
			return;
		}
		if (Op_Player.Pass.equals(opType)) {
			pos.op(request, opType);
		} else if (Op_Player.Op.equals(opType)) {
			switch (onAnNiu) {
			case BuPai://补牌		
			case BuMingPai://补明牌
			case OutCard://出牌
			case KanPai://看牌
			case MingPai://明牌
			case BiPai://比牌
				notifyCard(opPos,opType,onAnNiu,comporePos);
				break;
			case GenZhu://跟注
			case YaZhu://压注
			case QiPai://弃牌
			case JiaBei://加倍
				notifyCard(opPos,opType,onAnNiu);
				break;
			default:
				break;
			}
		} else if (Op_Player.Back.equals(opType)) {
			notifyCard(opPos,opType,onAnNiu,comporePos);
		}
	}
	
	/**
	 * 通知所有人牌
	 * @param opPos 
	 * @param opType
	 */
	private void notifyCard(int opPos,Op_Player opType,ZYPK_AnNiu onAnNiu,int comporePos) {
		ZYPKRoomPos<?> zPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(opPos);
		ZYPKSet_Pos posInfoOther = zPos.getNotifyCard(0);
		ZYPKSet_Pos posInfoSelf = zPos.getNotifyCard(zPos.pid);
		this.room.notify2Pos(opPos, SZYPK_PosOpCard.make(this.room.getRoomID(),opPos, posInfoSelf, opType,onAnNiu,comporePos));
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if (!this.set.getPlayerList().get(i))
				continue;
			if (i == opPos)
				continue;
			this.room.notify2Pos(i, SZYPK_PosOpCard.make(this.room.getRoomID(),opPos, posInfoOther, opType,onAnNiu,comporePos));
		}
	}

	/**
	 * 通知所有人牌
	 * @param opPos 位置
	 * @param opType 操作动作
	 * @param onAnNiu 按钮动作
	 * @param comporePos 比较位置
	 */
	private void notifyCard (int opPos,Op_Player opType,ZYPK_AnNiu onAnNiu) {
		ZYPKRoomPos<?> zPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(opPos);
		int number = -1;
		switch (onAnNiu) {
		case GenZhu:
		case YaZhu:
			number = zPos.getYaZhu();
			break;
		case JiaBei:
			number = zPos.getBeiShu();
			break;
		default:
			break;
		}
		this.room.notify2All(SZYPK_PosOpCard.make(this.room.getRoomID(), opPos, opType,onAnNiu,number));
	}
	
	
	/**
	 * 自由扑克回合位置
	 * 
	 * @param pos
	 * @return
	 */
	private boolean ZYPKRoundPos(int pos) {
		ZYPKRoundPos tmPos = new ZYPKRoundPos(this, pos);
		List<Op_Player> recieveOpTypes = new ArrayList<Op_Player>();
		recieveOpTypes.add(Op_Player.Op);
		recieveOpTypes.add(Op_Player.Pass);
		recieveOpTypes.add(Op_Player.Back);
		tmPos.addOpType(recieveOpTypes);
		this.roundPosDict.put(tmPos.getOpPos(), tmPos);
		return true;
	}

	/**
	 * 尝试开始回合, 如果失败，则set结束
	 * 
	 * @return
	 */
	public boolean tryStartRound() {
		ZYPKSetSound preRound = getPreRound(); // 前一个可参考的操作round
		// 第一次，庄家作为操作者，抓牌，等待出牌
		if (null == preRound) {
			int opPos = this.set.dPos;
			if (!ZYPKRoundPos(opPos))
				return false;
			this.notifyStart();
			return true;
		}

		// 上一轮放弃接牌
		if (preRound.getOpType() == Op_Player.Pass) {
			return tryStartRoundPass(preRound);
		}
		return false;
	}

	/**
	 * 过
	 * 
	 * @param preRound
	 * @return
	 */
	private boolean tryStartRoundPass(ZYPKSetSound preRound) {
		int opPos = preRound.getOpPos();
		// 只能顺序的抓牌，打牌
		opPos = this.set.nextOpPos(opPos, true);
		if (!ZYPKRoundPos(opPos))
			return false;
		notifyStart();
		return true;
	}

}
