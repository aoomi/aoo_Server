package business.xcpdk.c2s.cclass;

import jsproto.c2s.cclass.room.RoomPosInfo;

public class XCPDKRoomPosInfo extends RoomPosInfo {
	private boolean 	openStartGame;	//明牌 开始

	public boolean isOpenStartGame() {
		return openStartGame;
	}

	public void setOpenStartGame(boolean openStartGame) {
		this.openStartGame = openStartGame;
	}
	
	

}
