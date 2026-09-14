package business.cdxzmj.c2s.iclass;			
			
import jsproto.c2s.cclass.BaseSendMsg;			
			
public class SCDXZMJ_GenZhuang extends BaseSendMsg {			
	public long roomID;			
	public int dPos;			
	public int count;//跟庄次数			
			
	public static SCDXZMJ_GenZhuang make(long roomID, int dPos, int count) {			
		SCDXZMJ_GenZhuang ret = new SCDXZMJ_GenZhuang();			
		ret.roomID = roomID;			
		ret.dPos = dPos;			
		ret.count = count;			
		return ret;			
			
	}			
}									
