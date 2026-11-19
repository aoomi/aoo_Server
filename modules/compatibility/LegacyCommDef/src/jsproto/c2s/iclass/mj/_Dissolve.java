package jsproto.c2s.iclass.mj;
	
import jsproto.c2s.iclass.room.SBase_Dissolve;

/**	
 * 房间解散通知	
 * 	
 * @author Administrator	
 *	
 */	
public class _Dissolve extends SBase_Dissolve {
	
	/**	
	 * 	
	 */	
	private static final long serialVersionUID = 1L;	
	
	public static _Dissolve make(SBase_Dissolve dissolve,String gameNameStr) {
		_Dissolve ret = new _Dissolve();	
		ret.setOwnnerForce(dissolve.isOwnnerForce());	
		ret.setRoomID(dissolve.getRoomID());	
		ret.setDissolveNoticeType(dissolve.getDissolveNoticeType());	
		ret.setMsg(dissolve.getMsg());
		ret.setGameNameStr(gameNameStr);
		return ret;	
	}	
}	
