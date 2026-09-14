package business.cdxzmj.c2s.iclass;				
				
import jsproto.c2s.cclass.BaseSendMsg;				
				
/**				
 * 位置准备通知				
 * 				
 * @author Administrator				
 *				
 */				
@SuppressWarnings("serial")				
public class SCDXZMJ_PosReadyChg extends BaseSendMsg {				
	// 房间ID				
	private long roomID;				
	// 位置				
	private int pos;				
	// T:准备，F:取消准备				
	private boolean isReady;				
				
	public static SCDXZMJ_PosReadyChg make(long roomID, int pos, boolean isReady) {				
		SCDXZMJ_PosReadyChg ret = new SCDXZMJ_PosReadyChg();				
		ret.setRoomID(roomID);				
		ret.setPos(pos);				
		ret.setReady(isReady);				
		return ret;				
	}				
				
	public long getRoomID() {				
		return roomID;				
	}				
				
	public void setRoomID(long roomID) {				
		this.roomID = roomID;				
	}				
				
	public int getPos() {				
		return pos;				
	}				
				
	public void setPos(int pos) {				
		this.pos = pos;				
	}				
				
	public boolean isReady() {				
		return isReady;				
	}				
				
	public void setReady(boolean isReady) {				
		this.isReady = isReady;				
	}				
				
}				
