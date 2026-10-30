package business.cdxzmj.c2s.iclass;				
				
import jsproto.c2s.iclass.room.SBase_Dissolve;				
				
/**				
 * 房间解散通知				
 * 				
 * @author Administrator				
 *				
 */				
public class SCDXZMJ_Dissolve extends SBase_Dissolve {				
				
	/**				
	 * 				
	 */				
	private static final long serialVersionUID = 1L;				
				
	public static SCDXZMJ_Dissolve make(SBase_Dissolve dissolve) {				
		SCDXZMJ_Dissolve ret = new SCDXZMJ_Dissolve();				
		ret.setOwnnerForce(dissolve.isOwnnerForce());				
		ret.setRoomID(dissolve.getRoomID());				
		ret.setDissolveNoticeType(dissolve.getDissolveNoticeType());				
		ret.setMsg(dissolve.getMsg());				
		return ret;				
	}				
}				
