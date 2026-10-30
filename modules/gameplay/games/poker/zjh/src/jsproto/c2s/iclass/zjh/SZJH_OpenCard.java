package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 操作
 * @author zaf
 *
 */
public class SZJH_OpenCard extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置


    public static SZJH_OpenCard make(long roomID, int pos) {
        SZJH_OpenCard ret = new SZJH_OpenCard();
        ret.roomID = roomID;
        ret.pos = pos;
        return ret;
    }
}