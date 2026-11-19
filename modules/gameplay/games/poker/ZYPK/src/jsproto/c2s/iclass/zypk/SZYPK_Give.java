package jsproto.c2s.iclass.zypk;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;

/**
 * 自由扑克
 * @author huaxing
 *
 */

public class SZYPK_Give extends BaseSendMsg {
	public long roomID;
    public int pos;  //位置
    public int chouMa;//筹码
    public int targetPos;//目标位置
    
    public static SZYPK_Give make(long roomID,int pos,int chouMa,int targetPos) {
    	SZYPK_Give ret = new SZYPK_Give();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.chouMa  = chouMa;
        ret.targetPos = targetPos;
        return ret;
    }
}
