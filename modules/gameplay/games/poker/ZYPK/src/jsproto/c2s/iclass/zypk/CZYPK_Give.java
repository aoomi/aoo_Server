package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 赠送玩家筹码
 * @author huaxing
 *
 */

public class CZYPK_Give extends BaseSendMsg {

    public long roomID;//房间号
    public int pos;  //位置
    public int chouMa;//筹码
    public int targetPos;//目标位置

    public static CZYPK_Give make(long roomID,int pos, int chouMa,int targetPos) {
    	CZYPK_Give ret = new CZYPK_Give();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.chouMa = chouMa;
        ret.targetPos = targetPos;
        return ret;
    }
}
