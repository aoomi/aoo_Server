package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;

/**
 * 接收客户端数据
 * 操作牌
 *
 * @author zaf
 */

public class SNJPDK_OpCard extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int opCardType;  //PDK_CARD_TYPE 操作类型及牌的类型
    public ArrayList<Integer> cardList;
    public int nextPos;//下一个操作位
    public boolean turnEnd;//是否一轮结束
    public int daiNum;//带几张牌
    public boolean isSetEnd = false;
    public ArrayList<Integer> privateList;
    public int baoDanType = -1;//报单类型 -1 不需要报，1报单，2报双
    public boolean isFlash = false;//是否自动打牌
    public long runWaitSec = 0; //跑了多少时间

    public static SNJPDK_OpCard make(long roomID, int pos, int opCardType, int nextPos, ArrayList<Integer> cardList, boolean turnEnd, int daiNum, boolean isSetEnd, ArrayList<Integer> privateList, int baoDanType, boolean isFlash, long runWaitSec) {
        SNJPDK_OpCard ret = new SNJPDK_OpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.nextPos = nextPos;
        ret.turnEnd = turnEnd;
        ret.daiNum = daiNum;
        ret.isSetEnd = isSetEnd;
        ret.privateList = privateList;
        ret.baoDanType = baoDanType;
        ret.isFlash = isFlash;
        ret.runWaitSec = runWaitSec;
        return ret;
    }
}
