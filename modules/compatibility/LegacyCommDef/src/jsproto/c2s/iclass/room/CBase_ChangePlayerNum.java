package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;


public class CBase_ChangePlayerNum extends BaseSendMsg {

    //    public int	  playerNum;  //不用设置为位置 值为-1 否侧为设置固定位置
    public long roomID;

    public static CBase_ChangePlayerNum make(long roomID/*, int playerNum*/) {
        CBase_ChangePlayerNum ret = new CBase_ChangePlayerNum();
        ret.roomID = roomID;
//        ret.playerNum = playerNum;
        return ret;
    }
}