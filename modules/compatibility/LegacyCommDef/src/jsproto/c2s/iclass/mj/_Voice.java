package jsproto.c2s.iclass.mj;
	
import jsproto.c2s.cclass.BaseSendMsg;

/**	
 * 语音通知	
 * 	
 * @author Administrator	
 *	
 */	
@SuppressWarnings("serial")	
public class _Voice extends BaseSendMsg {
	// 房间ID	
	private long roomID;	
	// 位置	
	private int pos;	
	// 语音地址	
	private String url;	
	
	public static _Voice make(long roomID, int pos, String url, String gameNameStr) {
		_Voice ret = new _Voice();
		ret.setRoomID(roomID);	
		ret.setPos(pos);	
		ret.setUrl(url);
		ret.setGameNameStr(gameNameStr);
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
	
	public String getUrl() {	
		return url;	
	}	
	
	public void setUrl(String url) {	
		this.url = url;	
	}	
	
}	
