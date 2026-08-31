package jsproto.c2s.iclass.zypk;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KongPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;

/**
 * 自由扑克
 * @author huaxing
 *
 */

public class SZYPK_Clean extends BaseSendMsg {
	public long roomID;
    public Op_KongPai kongPai;  //位置
    
    public static SZYPK_Clean make(long roomID,Op_KongPai kongPai) {
    	SZYPK_Clean ret = new SZYPK_Clean();
        ret.roomID = roomID;
        ret.kongPai = kongPai;
        return ret;
    }
}
