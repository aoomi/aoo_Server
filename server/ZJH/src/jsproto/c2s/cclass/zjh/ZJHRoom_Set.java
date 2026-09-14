package jsproto.c2s.cclass.zjh;

import java.util.ArrayList;
import java.util.List;

import cenum.room.SetState;

/**
 * 炸金花 当前局游戏信息
 * @author zaf
 *
 */
public class ZJHRoom_Set {

	public int  setID = 0; // 游戏局ID
	public long startTime = 0;
	public int state = SetState.Init.value(); // 游戏状态
	public int  backerPos = -1;// 当前庄家
	public long bottomPoint = 0;//底注
	public int opPos = -1;				//操作位置
	public int currTurns = 0;// 当前轮数

	public List<ZJHRoomSet_Pos> posInfo = new ArrayList<>(); // 一局玩家列表


}
