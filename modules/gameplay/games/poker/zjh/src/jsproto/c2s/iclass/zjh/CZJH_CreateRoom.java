package jsproto.c2s.iclass.zjh;

import jsproto.c2s.cclass.room.BaseCreateRoom;

/**
 * 炸金花
 * 接收客户端数据
 * 创建房间
 * @author zaf
 *
 */

public class CZJH_CreateRoom extends BaseCreateRoom {
	public int 		difen = 0; 				//底分
	public int 		dingzhu  = 0;				//顶注
	public int 		dizhu  = 0;				//底注
    public int 		kebilunshu = 0;  		//可比轮数
    public boolean 	xiqian = false;		//喜钱
    public boolean 	gaojixuanxiang = false;//游戏开始后禁止进入
    public int 		lunshushangxian = 2;//轮数上限

    public static CZJH_CreateRoom make(int endPoints,int topPoint, int bottomPoint,int setCount, int paymentRoomCardType, 
    		int comporeCount, boolean isXiQian, boolean isKeptOutAfterStartGame,boolean isContinue,
    		long clubID, long gameIndex, int 	lunshushangxian,int createType) {
    	CZJH_CreateRoom ret = new CZJH_CreateRoom();
    	ret.difen = endPoints;
        ret.dingzhu = topPoint;
        ret.setSetCount(setCount);
        ret.dizhu = bottomPoint;
        ret.setPaymentRoomCardType(paymentRoomCardType);
        ret.kebilunshu = comporeCount;
        ret.xiqian = isXiQian;
        ret.gaojixuanxiang = isKeptOutAfterStartGame;
        ret.lunshushangxian = lunshushangxian;
        ret.setClubId(clubID);
        ret.setGameIndex(gameIndex);
        ret.setCreateType(createType);
        return ret;
    }
}
