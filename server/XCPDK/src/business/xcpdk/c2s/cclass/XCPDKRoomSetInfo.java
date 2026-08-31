package business.xcpdk.c2s.cclass;

import business.xcpdk.c2s.iclass.SXCPDK_SetEnd;
import jsproto.c2s.cclass.pk.Victory;
import jsproto.c2s.cclass.room.RoomSetInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 跑得快 当前局游戏信息
 * @author zaf
 *
 */
public class XCPDKRoomSetInfo extends RoomSetInfo{
	public long roomID = 0; // 房间ID
	public int  state = XCPDK_define.XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD.value(); // 游戏状态
	public long startTime = 0;
	public long runWaitSec = 0; //跑了多少时间
	public int  opPos = -1;// 当前操作位
	public int  eightPos = -1;// 8大队操作玩家
	public int razz = -1;//赖子
	public int firstOpCard = -1;//首出的牌
	public int 	firstOpPos = -1;//首出牌的位置
	public int  opType = XCPDK_define.XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_NOMARL.value();//最后出牌的类型
	public int  lastOpPos = -1;//最后出牌的位置
	public List<Integer> cardList = new ArrayList<>();//最后打的牌
	public boolean isFirstOp = true;//是否是首出
	public boolean isSetEnd = false;
	public List<Integer> 	roomDoubleList;//房间倍数
	public Victory  robCloseVic ;		//关门位置
	public Victory  reverseRobCloseVic ;	//反关门位置
	public List<XCPDKRoomSet_Pos> posInfo = new ArrayList<>(); // 一局玩家列表
	public SXCPDK_SetEnd setEnd;

}
