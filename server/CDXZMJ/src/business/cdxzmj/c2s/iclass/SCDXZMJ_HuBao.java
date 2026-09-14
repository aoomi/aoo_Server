package business.cdxzmj.c2s.iclass;	
	
import jsproto.c2s.cclass.BaseSendMsg;	
	
public class SCDXZMJ_HuBao extends BaseSendMsg {	
    public long roomID;	
    public int pos;	
    public int huBaoPos;//互相包	
    public int type;//0:“当心互包” 1:互包	
	
    public static SCDXZMJ_HuBao make(long roomID, int pos, int huBaoPos,int type) {	
        SCDXZMJ_HuBao ret = new SCDXZMJ_HuBao();	
        ret.roomID = roomID;	
        ret.pos = pos;	
        ret.huBaoPos = huBaoPos;	
        ret.type = type;	
        return ret;	
    }	
}									
