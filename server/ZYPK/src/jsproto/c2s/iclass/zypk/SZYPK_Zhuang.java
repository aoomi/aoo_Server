package jsproto.c2s.iclass.zypk;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;

/**
 * 自由扑克
 * @author huaxing
 *
 */

public class SZYPK_Zhuang extends BaseSendMsg {
	public long roomID;
    public ZYPK_QiangZhuang qiangZhuang;  //位置
    
    public static SZYPK_Zhuang make(long roomID,ZYPK_QiangZhuang qiangZhuang) {
    	SZYPK_Zhuang ret = new SZYPK_Zhuang();
        ret.roomID = roomID;
        ret.qiangZhuang = qiangZhuang;
        return ret;
    }
}
