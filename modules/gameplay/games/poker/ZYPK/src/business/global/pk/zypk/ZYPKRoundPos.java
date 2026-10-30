package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

/**
 * 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡
 * 
 * @author Administrator
 *
 */
public class ZYPKRoundPos  {

	
	protected ZYPKSetSound round;
	protected ZYPKRoomSet set;
	protected ZYPKRoomPos<?> pos;

	protected int opPos; // 当前等待操作的pos
	protected boolean isFlash = false;

	protected final int FlashSec = 3; // ms
	protected final int FastOutSec = 5;// s

	// 可接受的操作
	protected List<Op_Player> recieveOpTypes = new ArrayList<>();
	protected boolean publicWait = false; // 公开在操作，别人可以看到读条（出牌行为是公开的）

	protected Op_Player opType; // 最终执行的操作
	protected int opCard; // 操作的牌

	public void addOpType(List<Op_Player> recieveOpTypes) {
		if (null == recieveOpTypes)
			return;
		this.recieveOpTypes.addAll(recieveOpTypes);
		this.publicWait = this.recieveOpTypes.contains(Op_Player.Pass);
	}

	public void addOpType(Op_Player opType) {
		if (null == opType)
			return;
		this.recieveOpTypes.add(opType);
		this.publicWait = this.recieveOpTypes.contains(Op_Player.Pass);
	}

	public List<Op_Player> getRecieveOpTypes() {
		return this.recieveOpTypes;
	}

	public boolean checkRecieveOpTypes(Op_Player opType) {
		return getRecieveOpTypes().contains(opType);
	}


	public boolean isPublicWait() {
		return publicWait;
	}



	public void tryEndRound(boolean isHu) {
		this.round.tryEndRound(isHu);
	}
	
	
	/**
	 * 操作错误
	 * 
	 * @param request
	 * @param opType
	 * @return
	 */
	public int errorOpType(WebSocketRequest request, Op_Player opType) {
		// 操作错误
		if (!checkRecieveOpTypes(opType)) {
			request.error(ErrorCode.NotAllow, "opType : "+opType);
			return -1;
		}
		return 1;
	}







	
	
	public ZYPKRoundPos(ZYPKSetSound round, int opPos) {
		this.round = round;
		this.opPos = opPos;
		this.set = round.getSet();
		this.pos = (ZYPKRoomPos<?>) set.getRoom().getPosMgr().getPos(opPos);
	}

	// 3 接打牌
	// 3.1过
	public int op_Pass(WebSocketRequest request, Op_Player opType) {
		// 操作错误
		if (errorOpType(request, opType) <= 0)
			return -1;
		this.round.setOpType(Op_Player.Pass);
		this.tryEndRound(false);
		return 0;
	}



	public int getOpPos() {
		return opPos;
	}


	
	// 暂未实现 TODO
	public int op_Default() {
		this.tryEndRound(false);
		return 0;
	}
	public Op_Player getOpType() {
		return opType;
	}
	
	public int op(WebSocketRequest request, Op_Player opType) {
		int opCardRet = -1;
		if (this.getOpType() != null) {
			 request.error(ErrorCode.NotAllow, "opPos has opered");
			return opCardRet;
		}
		switch (opType) {
		case Pass:
			opCardRet = op_Pass(request, opType);
			break;
		default:
			break;
		}
		return opCardRet;
	}

}
