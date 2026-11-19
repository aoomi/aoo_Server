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
public class CCDXZMJ_AutoChoose extends BaseSendMsg {
	
    public long roomID;
    public int autoHu;// 0取消 1选择
    public int autoOut;// 0取消 1选择

    public static CCDXZMJ_AutoChoose make(long roomID, int autoHu,int autoOut) {
        CCDXZMJ_AutoChoose ret = new CCDXZMJ_AutoChoose();
        ret.autoHu = autoHu;
        ret.autoOut = autoOut;
        return ret;	
    }	
}						
