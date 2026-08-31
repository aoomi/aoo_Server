package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 客户端请求，俱乐部房间战绩
 *
 * @author zaf
 */
public class CClub_RoomRecord extends BaseSendMsg {
    public long clubId;//俱乐部ID
    public int type;//类型(1、麻将，2)
    public int pageNum;

    public static CClub_RoomRecord make(long clubId, int type, int pageNum) {
        CClub_RoomRecord ret = new CClub_RoomRecord();
        ret.clubId = clubId;
        ret.type = type;
        ret.pageNum = pageNum;
        return ret;
    }
}
