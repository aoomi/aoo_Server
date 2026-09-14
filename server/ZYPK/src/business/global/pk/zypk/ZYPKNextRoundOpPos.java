package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;

/**
 * 下回合玩家位置
 * @author Huaxing
 *
 * @param <T>
 */
public class ZYPKNextRoundOpPos<T>{
	public T fromRound; // 来源于round的后续
	public List<Integer> opPos;
	// 可接受的操作
	public List<Op_Player> recieveOpTypes = new ArrayList<>();
	public boolean isUsed = false;
	public HashMap<Integer, Op_Player> posOpTypeMap = new HashMap<Integer, Op_Player>();

	public ZYPKNextRoundOpPos(List<Integer> opPos, List<Op_Player> recieveOpTypes, T fromRound){
		this.opPos = opPos;
		this.recieveOpTypes = recieveOpTypes;
		this.fromRound = fromRound;
	}
	
	public ZYPKNextRoundOpPos(List<Integer> opPos, HashMap<Integer, Op_Player> posOpTypeMap, T fromRound){
		this.opPos = opPos;
		this.posOpTypeMap = posOpTypeMap;
		this.fromRound = fromRound;
	}
	
}
