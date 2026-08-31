package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 庄家设置
 * @author huaxing
 *
 */

public class CZYPK_Zhuang extends BaseSendMsg {

    public long roomID;//房间号
    public int pos;  //位置
    public int opType;//操作类型
    public int targetPos;//比较位置

    public static CZYPK_Zhuang make(long roomID,int pos, int opType,int targetPos) {
    	CZYPK_Zhuang ret = new CZYPK_Zhuang();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.targetPos = targetPos;
        return ret;
    }
}
