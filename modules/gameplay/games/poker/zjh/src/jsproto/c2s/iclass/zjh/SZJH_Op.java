package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 操作
 * @author zaf
 *
 */
public class SZJH_Op extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int opType;//操作类型
    public int addBet;  //押注分数
    public int comporePos;//比较位置
    public boolean isWin; //输赢  true:pos赢   false:pos输
    public int nextOpPos;//下一个操作的位置
    public long startTime;//下一个操作的位置的开始时间
    public int currTurns = 0;// 当前轮数

    public static SZJH_Op make(long roomID, int pos,int opType, int addBet, int comporePos, boolean isWin, int nextOpPos, long startTime, int currTurns) {
        SZJH_Op ret = new SZJH_Op();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.addBet = addBet;
        ret.comporePos = comporePos;
        ret.isWin = isWin;
        ret.nextOpPos = nextOpPos;
        ret.startTime = startTime;
        ret.currTurns = currTurns;
        return ret;


    }
}