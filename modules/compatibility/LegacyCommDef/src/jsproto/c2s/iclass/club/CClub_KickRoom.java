package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;


public class CClub_KickRoom extends BaseSendMsg {
    public long clubId;        //俱乐部ID
    public String roomKey;
    public int posIndex;


    public static CClub_KickRoom make(long clubId, String roomKey, int posIndex) {
        CClub_KickRoom ret = new CClub_KickRoom();
        ret.roomKey = roomKey;
        ret.posIndex = posIndex;
        ret.clubId = clubId;
        return ret;
    }
}