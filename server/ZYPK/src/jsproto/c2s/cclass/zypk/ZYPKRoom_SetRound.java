package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

public class ZYPKRoom_SetRound{
	// 本次等待
	private int waitID = 0; // 当前第几次等待操作
	private int startWaitSec = 0; //开始等待时间
	private final List<ZYPKRoom_RoundPos> opPosList = new ArrayList<>();
	
	
	public int getWaitID() {
		return waitID;
	}
	public void setWaitID(int waitID) {
		this.waitID = waitID;
	}
	public int getStartWaitSec() {
		return startWaitSec;
	}
	public void setStartWaitSec(int startWaitSec) {
		this.startWaitSec = startWaitSec;
	}
	public List<ZYPKRoom_RoundPos> getOpPosList() {
		return opPosList;
	}
	public void addOpPosList(ZYPKRoom_RoundPos bRoundPos) {
		if (null == bRoundPos)
			return;
		this.opPosList.add(bRoundPos);
	}
	@Override
	public String toString() {
		return "BaseMJRoom_SetRound [waitID=" + waitID + ", startWaitSec="
				+ startWaitSec + ", opPosList=" + opPosList + "]";
	}
	
	

}