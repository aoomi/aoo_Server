package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 抢庄
 * @author huaxing
 *
 */

public class CZYPK_QiangZhuang extends BaseSendMsg {

    public long roomID;//房间号
    public int pos;  //位置
    public int opType = 0;//0:不抢，1:抢

    public static CZYPK_QiangZhuang make(long roomID,int pos, int opType) {
    	CZYPK_QiangZhuang ret = new CZYPK_QiangZhuang();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        return ret;
    }
}
