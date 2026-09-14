package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 操作
 * @author zaf
 *
 */
public class CZJH_OpenCard extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置


    public static CZJH_OpenCard make(long roomID, int pos) {
        CZJH_OpenCard ret = new CZJH_OpenCard();
        ret.roomID = roomID;
        ret.pos = pos;
        return ret;


    }
}