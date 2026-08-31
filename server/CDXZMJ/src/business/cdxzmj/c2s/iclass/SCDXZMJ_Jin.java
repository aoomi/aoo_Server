package business.cdxzmj.c2s.iclass;			
			
import jsproto.c2s.cclass.BaseSendMsg;			
			
@SuppressWarnings("serial")			
public class SCDXZMJ_Jin extends BaseSendMsg {			
    public long roomID;			
    public int jin1;			
    public int jin2;			
    private int jinJin ;			
    public int normalMoCnt = 0; // 普通摸牌数量			
    public int gangMoCnt = 0; // 杠后摸牌数量			
			
    public static SCDXZMJ_Jin make(long roomID, int jin,int normalMoCnt, int gangMoCnt) {	
        SCDXZMJ_Jin ret = new SCDXZMJ_Jin();			
        ret.roomID = roomID;			
        ret.jin1 = jin;			
        ret.normalMoCnt = normalMoCnt;	
        ret.gangMoCnt = gangMoCnt;			
			
        return ret;			
			
    }			
}			
