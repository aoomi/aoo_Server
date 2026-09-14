package business.cdxzmj.c2s.iclass;			
			
import cenum.room.SetState;			
import jsproto.c2s.cclass.BaseSendMsg;			
			
import java.util.ArrayList;			
			
/**			
 * 接收客户端数据			
 * 状态改变			
 *			
 * @author zaf			
 */			
			
@SuppressWarnings("serial")			
public class SCDXZMJ_ChangeStatus extends BaseSendMsg {			
			
    public long roomID;			
    public int setID;//局数									
    public int dPos;//局数			
    public SetState state;  //位置			
    public ArrayList<Integer> piaoFenList = new ArrayList<>(); // 飘分			
			
    public static SCDXZMJ_ChangeStatus make(long roomID, int setID, SetState state,  int dPos,ArrayList<Integer> piaoFenList) {			
        SCDXZMJ_ChangeStatus ret = new SCDXZMJ_ChangeStatus();			
        ret.roomID = roomID;			
        ret.setID = setID;			
        ret.state = state;			
        ret.dPos = dPos;			
        ret.piaoFenList = piaoFenList;			
        return ret;			
    }			
}															
