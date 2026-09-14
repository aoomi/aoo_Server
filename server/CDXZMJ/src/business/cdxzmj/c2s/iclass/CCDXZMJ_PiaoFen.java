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
public class CCDXZMJ_PiaoFen extends BaseSendMsg {			
			
    public long roomID;			
    public int piaoFen;			
			
    public static CCDXZMJ_PiaoFen make(long roomID, int piaoFen) {			
        CCDXZMJ_PiaoFen ret = new CCDXZMJ_PiaoFen();			
        ret.roomID = roomID;			
        ret.piaoFen = piaoFen;			
        return ret;			
    }			
}											
