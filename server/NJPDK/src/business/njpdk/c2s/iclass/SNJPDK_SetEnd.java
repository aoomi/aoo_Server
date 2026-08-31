package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;


public class SNJPDK_SetEnd extends BaseSendMsg {

    public long roomID;
    public int setStatus;
    public long startTime;
    public int firstOpPos = -1;//首出牌的位置
    public List<Integer> pointList; //得分
    public ArrayList<Integer> surplusCardList;//剩余牌数
    public List<Integer> bomList;//倍数  Victory:pos 玩家位置  num:玩家pos对应的加了几倍    在list里面没有找到就标识玩家没有加倍
    public List<Integer> totalPointList;//总分
    public List<SNJPDK_OutCardList> cardList;//出手牌顺序
    public List<Integer> shutDownList;//关门顺序 //-1不关0大关1小关
    public List<List<Integer>> privateList;//私有牌列表
    public List<Double> sportsPointList;
    public int playBackCode; //回放码

    public static SNJPDK_SetEnd make(long roomID, int setStatus, long startTime, int firstOpPos, List<Integer> pointList, ArrayList<Integer> surplusCardList, List<Integer> bombList, List<Integer> totalPointList, List<SNJPDK_OutCardList> cardList, List<List<Integer>> privateList, int playBackCode, List<Double> sportsPointList, List<Integer> shutDownList) {
        SNJPDK_SetEnd ret = new SNJPDK_SetEnd();
        ret.roomID = roomID;
        ret.setStatus = setStatus;
        ret.startTime = startTime;
        ret.pointList = pointList;
        ret.surplusCardList = surplusCardList;
        ret.firstOpPos = firstOpPos;
        ret.totalPointList = totalPointList;
        ret.cardList = cardList;
        ret.privateList = privateList;
        ret.playBackCode = playBackCode;
        ret.bomList = bombList;
        ret.sportsPointList = sportsPointList;
        ret.shutDownList = shutDownList;
        return ret;
    }
}
