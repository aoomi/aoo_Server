package jsproto.c2s.iclass.zypk;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;

/**
 * 自由扑克
 * 选庄
 * @author huaxing
 *
 */

public class SZYPK_XuanZhuang extends BaseSendMsg {
	public long roomID;
    public int dPos;
    
    public static SZYPK_XuanZhuang make(long roomID,int dPos) {
    	SZYPK_XuanZhuang ret = new SZYPK_XuanZhuang();
        ret.roomID = roomID;
        ret.dPos = dPos;
        return ret;
    }
}
