package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 操作
 * @author zaf
 *
 */
public class CZJH_Op extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int opType;//操作类型
    public int addBet;  //押注分数
    public int comporePos;//比较位置


    public static CZJH_Op make(long roomID, int pos,int opType, int addBet, int comporePos) {
        CZJH_Op ret = new CZJH_Op();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.addBet = addBet;
        ret.comporePos = comporePos;
        return ret;


    }
}