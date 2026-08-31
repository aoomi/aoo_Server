package business.xcpdk.c2s.iclass;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 * @author Huaxing
 *
 */
@SuppressWarnings("serial")
public class SXCPDK_BaDaDui extends BaseSendMsg{

	public long roomID;
	public int dui;  // 0不八大队 1八大队
	public int  pos;
	public int  nextPos = -1;

	public static SXCPDK_BaDaDui make(long roomID, int dui,int pos, int nextPos) {
    	SXCPDK_BaDaDui ret = new SXCPDK_BaDaDui();
    	ret.roomID = roomID;
    	ret.nextPos = nextPos ;
    	ret.dui = dui;
    	ret.pos = pos;
        return ret;
    }

    public static void main(String args[]){
    	System.out.println(new Gson().toJson(new SXCPDK_BaDaDui()));
	}
}
