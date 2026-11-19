package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

import cenum.room.SetState;

/**
 * 自由扑克 当前局游戏信息
 * @author huaxing
 * @param <T>
 *
 */
public class ZYPKRoom_Set<T> {

	public long roomID = 0; // 房间ID
	public int  setID = 0; // 游戏局ID
	public int state = SetState.Init.value();
	public long startTime = 0;
	public int dPos = -1;// 庄家位置
	public int kongPaiPos = -1;//控牌位置
	public int  lastOpPos = -1;//最后出牌的位置
	public ArrayList<Integer> publicCardList = new ArrayList<Integer>();//公共牌
	public ArrayList<Integer> liuPaiList = new ArrayList<Integer>();//留牌
	public int liuPaiNum = 0;
	public int pkCardSize = 0;
	
	public List<T> posInfo = new ArrayList<>(); // 一局玩家列表
	
	public ZYPKRoom_SetRound setRound = new ZYPKRoom_SetRound(); 
}
