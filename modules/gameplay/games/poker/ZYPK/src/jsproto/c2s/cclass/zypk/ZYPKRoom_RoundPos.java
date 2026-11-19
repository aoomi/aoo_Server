package jsproto.c2s.cclass.zypk;

import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;

/**
 * 红中麻将 配置
 * 
 * @author Clark
 *
 */

public class ZYPKRoom_RoundPos {
	// 本次等待
	private int waitOpPos = -1; // 当前等待操作的人 暗操作，填-1
	private List<Op_Player> opList = null;// 可执行者独享，可操作列表
	private int LastOpCard = 0;
	private Op_Player opType = Op_Player.Pass;
	private int opCard = 0;

	public int getWaitOpPos() {
		return waitOpPos;
	}

	public void setWaitOpPos(int waitOpPos) {
		this.waitOpPos = waitOpPos;
	}

	public List<Op_Player> getOpList() {
		return opList;
	}

	public void setOpList(List<Op_Player> opList) {
		if (null == opList || opList.size() <= 0) {
			return;
		}
		this.opList = opList;
	}



	public int getLastOpCard() {
		return LastOpCard;
	}

	public void setLastOpCard(int lastOpCard) {
		LastOpCard = lastOpCard;
	}

	public Op_Player getOpType() {
		return opType;
	}

	public void setOpType(Op_Player opType) {
		this.opType = opType;
	}

	public int getOpCard() {
		return opCard;
	}

	public void setOpCard(int opCard) {
		this.opCard = opCard;
	}
	
	

	
	
}
