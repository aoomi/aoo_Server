package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 客户端请求，房间战绩
 *
 * @author Huaxing
 */
public class CPlayer_RoomRecord extends BaseSendMsg {
    public int gameType = -1;
    public int pageNum;
    public long clubId;
    public int getType;
    public int sort;

    public static CPlayer_RoomRecord make(int gameType, int pageNum, long clubId) {
        CPlayer_RoomRecord ret = new CPlayer_RoomRecord();
        ret.gameType = gameType;
        ret.pageNum = pageNum;
        ret.clubId = clubId;
        return ret;
    }
}
