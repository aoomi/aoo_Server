package business.xcpdk.c2s.iclass;
import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.Victory;
import jsproto.c2s.iclass.pk.SPDK_OutCardList;


public class SXCPDK_SetEnd extends BaseSendMsg {

    public long roomID;
    public int setStatus;
    public long startTime;
    public List<Integer> 	roomDoubleList;//房间倍数
    public int 	firstOpPos = -1;//首出牌的位置
    public Victory  robCloseVic ;		//关门位置
    public Victory  reverseRobCloseVic ;	//反关门位置
	public List<Integer> pointList; //得分
	public ArrayList<Integer> surplusCardList;//剩余牌数
	public List<Victory> doubleList;//倍数  Victory:pos 玩家位置  num:玩家pos对应的加了几倍    在list里面没有找到就标识玩家没有加倍
	public ArrayList<Boolean> beShutDowList; 	//是否被关门
    public List<SPDK_OutCardList> cardList;//出手牌顺序
    public List<List<Integer>> privateList;//私有牌列表
    public int playBackCode;
    public List<Double> sportsPointList;
    public boolean roomEnd;
    public List<Integer> bombList;//倍数  Victory:pos 玩家位置  num:玩家pos对应的加了几倍    在list里面没有找到就标识玩家没有加倍
    public List<Integer> totalPointList;//总分
    public ArrayList<Boolean> xiaoGuanList; 	//小关列表

    public static SXCPDK_SetEnd make(long roomID,int setStatus,  long startTime,List<Integer> 	roomDoubleList,int firstOpPos, Victory  robCloseVic,Victory  reverseRobCloseVic, List<Integer> pointList, ArrayList<Integer> surplusCardList, List<Integer> bombList, ArrayList<Boolean> beShutDowList,int playBackCode,List<SPDK_OutCardList> cardList,List<List<Integer>> privateList,boolean roomEnd,List<Double> sportsPointList,List<Integer> totalPointList,ArrayList<Boolean> xiaoGuanList) {
        SXCPDK_SetEnd ret = new SXCPDK_SetEnd();
        ret.roomID = roomID;
        ret.setStatus = setStatus;
        ret.startTime = startTime;
        ret.pointList = pointList;
        ret.surplusCardList = surplusCardList;
        ret.roomDoubleList = roomDoubleList;
        ret.robCloseVic = robCloseVic;
        ret.reverseRobCloseVic = reverseRobCloseVic;
        ret.beShutDowList = beShutDowList;
        ret.firstOpPos = firstOpPos;
        ret.playBackCode = playBackCode;
        ret.cardList = cardList;
        ret.privateList = privateList;
        ret.roomEnd = roomEnd;
        ret.sportsPointList = sportsPointList;
        ret.bombList = bombList;
        ret.totalPointList = totalPointList;
        ret.xiaoGuanList = xiaoGuanList;
        return ret;
    }
}
