package business.njpdk.c2s.cclass;

import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_CARD_TYPE;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_GameStatus;
import business.njpdk.c2s.iclass.SNJPDK_SetEnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 安岳跑得快 当前局游戏信息
 *
 * @author zaf
 */
public class NJPDKRoom_Set {
    public long roomID = 0; // 房间ID
    public int setID = 0; // 游戏局ID
    public int state = NJPDK_GameStatus.PDK_GAME_STATUS_SENDCARD.value(); // 游戏状态
    public long startTime = 0;
    public int opPos = -1;// 当前操作位
    public int firstOpCard = -1;//首出的牌
    public int opType = NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value();//最后出牌的类型
    public int lastOpPos = -1;//最后出牌的位置
    public List<Integer> cardList = new ArrayList<>();//最后打的牌
    public boolean isFirstOp = true;//是否是首出
    public boolean isSetEnd = false;
    public SNJPDK_SetEnd setEnd;
    public List<Integer> roomDoubleList;//房间倍数
    public List<NJPDKRoomSet_Pos> posInfo = new ArrayList<>(); // 一局玩家列表
    public Map<Integer, Integer> cardNumMap;//记牌器
}
