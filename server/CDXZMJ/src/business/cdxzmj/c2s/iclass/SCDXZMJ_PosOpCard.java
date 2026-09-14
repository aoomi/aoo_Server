package business.cdxzmj.c2s.iclass;			
			
import cenum.mj.OpType;			
import jsproto.c2s.cclass.BaseSendMsg;			
			
			
@SuppressWarnings("serial")			
public class SCDXZMJ_PosOpCard<T> extends BaseSendMsg {			
			
    public long roomID;			
    public int pos;			
    public T set_Pos;			
    public OpType opType;			
    public int opCard;			
    public boolean isFlash;			
			
			
    public static <T> SCDXZMJ_PosOpCard<T> make(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash) {			
        SCDXZMJ_PosOpCard<T> ret = new SCDXZMJ_PosOpCard<T>();			
        ret.roomID = roomID;			
        ret.pos = pos;			
        ret.set_Pos = set_Pos;			
        ret.opType = opType;			
        ret.opCard = opCard;			
        ret.isFlash = isFlash;			
			
        return ret;			
			
			
    }			
}				
