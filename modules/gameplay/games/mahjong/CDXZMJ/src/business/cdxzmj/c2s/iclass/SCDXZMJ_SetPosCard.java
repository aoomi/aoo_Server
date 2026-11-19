package business.cdxzmj.c2s.iclass;			
			
import jsproto.c2s.cclass.BaseSendMsg;			
			
import java.util.ArrayList;			
import java.util.List;			
			
/**			
 * 重新设置玩家手牌			
 * @author Huaxing			
 * @param <T>			
 *			
 */			
public class SCDXZMJ_SetPosCard<T> extends BaseSendMsg  {			
    public long roomID;			
	// 每个玩家的牌面			
	public List<T> setPosList = new ArrayList<>();			
			
			
    public static <T>SCDXZMJ_SetPosCard make(long roomID,List<T> setPosList) {			
    	SCDXZMJ_SetPosCard ret = new SCDXZMJ_SetPosCard();			
        ret.roomID = roomID;			
        ret.setPosList = setPosList;			
			
        return ret;			
    			
			
    }			
}			
