package jsproto.c2s.iclass.zypk;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_KongPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;

/**
 * 自由扑克控牌通知
 * @author huaxing
 *
 */

public class SZYPK_KongPai extends BaseSendMsg {
	public long roomID;
    public ZYPK_KongPai kongPai;  //控牌状态
    public int kongPaiPos = 0;	  //控牌者
    
    public static SZYPK_KongPai make(long roomID,ZYPK_KongPai kongPai,int kongPaiPos) {
    	SZYPK_KongPai ret = new SZYPK_KongPai();
        ret.roomID = roomID;
        ret.kongPai = kongPai;
        ret.kongPaiPos = kongPaiPos;
        return ret;
    }
}
