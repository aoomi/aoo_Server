package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 接收客户端数据
 * 操作牌
 * @author zaf
 *
 */

public class SXCPDK_OpCard extends BaseSendMsg {

	public long roomID;
    public int pos;  //位置
    public int opCardType;  //XCPDK_CARD_TYPE 操作类型及牌的类型
    public ArrayList<Integer> cardList;
    public int nextPos ;//下一个操作位
    public boolean turnEnd ;//是否一轮结束
    public int daiNum;//带几张牌
    public boolean isSetEnd = false;
    public ArrayList<Integer> privateList;
    public boolean isFlash = false; //是否自动打牌
    public boolean isFirstOp = false;//是否是首出
    public long runWaitSec = 0; //跑了多少时间
    public int secTotal = 0;
    public int dataSecTotal = 0;
    public List<Boolean> trusteeshipList = new ArrayList<>();

    public static SXCPDK_OpCard make(long roomID,int pos,int opCardType, int nextPos,  ArrayList<Integer> cardList, boolean turnEnd, int daiNum, boolean isSetEnd,ArrayList<Integer> privateList,boolean isFlash,long runWaitSec,int secTotal,int dataSecTotal,List<Boolean> trusteeshipList) {
    	SXCPDK_OpCard ret = new SXCPDK_OpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.nextPos = nextPos;
        ret.turnEnd = turnEnd;
        ret.daiNum = daiNum;
        ret.isSetEnd =isSetEnd;
        ret.privateList = privateList;
        ret.isFlash  = isFlash;
        ret.isFirstOp = false;//是否是首出
        ret.runWaitSec = runWaitSec;
        ret.secTotal =secTotal;
        ret.dataSecTotal =dataSecTotal;
        ret.trusteeshipList = trusteeshipList;
        return ret;
    }
}
