package jsproto.c2s.iclass.mj;
	
import jsproto.c2s.cclass.BaseSendMsg;

/**	
 * 连接通知	
 * 	
 * @author Administrator	
 *	
 */	
@SuppressWarnings("serial")	
public class _LostConnect extends BaseSendMsg {
	// 房间ID	
	private long roomID;	
	// 玩家PID	
	private long pid;	
	// T 离线,F 连接	
	private boolean isLostConnect;	
	private boolean isShowLeave;	
	
	public static _LostConnect make(long roomID, long pid, boolean isLostConnect,boolean isShowLeave, String gameNameStr) {
		_LostConnect ret = new _LostConnect();	
		ret.setRoomID(roomID);	
		ret.setPid(pid);	
		ret.setLostConnect(isLostConnect);	
		ret.setShowShowLeave(isShowLeave);
		ret.setGameNameStr(gameNameStr);
		return ret;	
	}	
	
	public long getRoomID() {	
		return roomID;	
	}	
	
	public void setRoomID(long roomID) {	
		this.roomID = roomID;	
	}	
	
	public long getPid() {	
		return pid;	
	}	
	
	public void setPid(long pid) {	
		this.pid = pid;	
	}	
	
	public boolean isLostConnect() {	
		return isLostConnect;	
	}	
	
	public void setLostConnect(boolean isLostConnect) {	
		this.isLostConnect = isLostConnect;	
	}	
	
	public void setShowShowLeave(boolean showShowLeave) {	
		isShowLeave = showShowLeave;	
	}	
}	
