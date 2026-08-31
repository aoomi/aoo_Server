package business.global.mj.extbussiness.dto.iclass;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 * @author Huaxing
 *
 */
@SuppressWarnings("serial")
public class CStandardMJ_PiaoFen extends BaseSendMsg{
    //EXAMPLE
	public long roomID;
	public int choose;  // 0不飘分 1飘分 2飘分

	public static CStandardMJ_PiaoFen make(long roomID, int choose) {
    	CStandardMJ_PiaoFen ret = new CStandardMJ_PiaoFen();
    	ret.roomID = roomID;
    	ret.choose = choose ;
        return ret;
    }
}
