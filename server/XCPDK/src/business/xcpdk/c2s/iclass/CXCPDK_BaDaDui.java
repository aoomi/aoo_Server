package business.xcpdk.c2s.iclass;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 * @author Huaxing
 *
 */
@SuppressWarnings("serial")
public class CXCPDK_BaDaDui extends BaseSendMsg{

	public long roomID;  
	public int dui;  // 0不八大队 1八大队
	
    public static CXCPDK_BaDaDui make(long roomID, int dui) {
    	CXCPDK_BaDaDui ret = new CXCPDK_BaDaDui();
    	ret.roomID = roomID;
    	ret.dui = dui ;
        return ret;
    }
}
