package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 位置继续游戏通知
 * 
 * @author Administrator
 *
 */
@SuppressWarnings("serial")
public class SXCPDK_PosContinueGame extends BaseSendMsg {
	// 房间ID
	private long roomID;
	// 位置
	private int pos;

	public static SXCPDK_PosContinueGame make(long roomID, int pos) {
		SXCPDK_PosContinueGame ret = new SXCPDK_PosContinueGame();
		ret.setRoomID(roomID);
		ret.setPos(pos);
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

}
