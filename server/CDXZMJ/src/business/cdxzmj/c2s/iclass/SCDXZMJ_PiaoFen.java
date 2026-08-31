package business.cdxzmj.c2s.iclass;			
			
import jsproto.c2s.cclass.BaseSendMsg;			
			
/**			
 * 莆田麻将			
 * 接收客户端数据			
 * 创建房间			
 *			
 * @author Huaxing			
 */			
@SuppressWarnings("serial")			
public class SCDXZMJ_PiaoFen extends BaseSendMsg {			
			
    public long roomID;			
    public int piaoFen;			
    public int pos;			
			
			
    public static SCDXZMJ_PiaoFen make(long roomID, int pos, int piaoFen) {			
        SCDXZMJ_PiaoFen ret = new SCDXZMJ_PiaoFen();			
        ret.roomID = roomID;			
        ret.piaoFen = piaoFen;			
        ret.pos = pos;			
        return ret;			
    }			
}											
