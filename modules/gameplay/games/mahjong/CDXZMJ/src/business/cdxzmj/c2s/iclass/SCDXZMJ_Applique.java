package business.cdxzmj.c2s.iclass;		
		
import cenum.mj.OpType;		
import jsproto.c2s.cclass.BaseSendMsg;		
		
/**		
 * 补花		
 *		
 * @param <T>		
 * @author Huaxing		
 */		
@SuppressWarnings("serial")		
public class SCDXZMJ_Applique<T> extends BaseSendMsg {		
    public long roomID;		
    public int pos;		
    public OpType opType;		
    public int opCard;		
    public boolean isFlash;		
    public T set_Pos;		
    public int normalMoCnt;		
    public int gangMoCnt;		
		
    @SuppressWarnings("rawtypes")		
    public static <T> SCDXZMJ_Applique make(long roomID, int pos, OpType opType, int opCard, boolean isFlash, T set_Pos, int normalMoCnt, int gangMoCnt) {		
        SCDXZMJ_Applique ret = new SCDXZMJ_Applique();		
        ret.roomID = roomID;		
        ret.pos = pos;		
        ret.opType = opType;		
        ret.opCard = opCard;		
        ret.isFlash = isFlash;		
        ret.set_Pos = set_Pos;		
        ret.normalMoCnt = normalMoCnt;		
        ret.gangMoCnt = gangMoCnt;		
        return ret;		
		
		
    }		
}			
