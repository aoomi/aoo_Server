package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;


public class CClub_DissolveRoom extends BaseSendMsg {
    public long clubId;        //俱乐部ID
    public String roomKey;


    public static CClub_DissolveRoom make(long clubId, String roomKey) {
        CClub_DissolveRoom ret = new CClub_DissolveRoom();
        ret.clubId = clubId;
        ret.roomKey = roomKey;
        return ret;


    }
}