package jsproto.c2s.cclass.zjh;

import jsproto.c2s.cclass.room.BaseCreateRoom;

/*
 * 创建房间配置
 * */
public class ZJHRoom_Cfg extends BaseCreateRoom {
    public int 		difen = 0; 				//底分
	public int 		dingzhu  = 0;				//顶注
	public int 		dizhu  = 0;				//底注
    public int 		kebilunshu = 1;  		//可比轮数
    public boolean 	xiqian = false;		//喜钱
    public boolean 	gaojixuanxiang = false;//游戏开始后禁止进入
    public int  	baseMark = 1;//积分房的底分
    public int 		lunshushangxian = 2;//轮数上限
}
