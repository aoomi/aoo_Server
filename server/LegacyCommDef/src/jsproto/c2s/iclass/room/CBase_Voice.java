package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

public class CBase_Voice extends BaseSendMsg {
    public long roomID;
    public String key;
    public String url;

    public static CBase_Voice make(long roomID, String key, String url) {
        CBase_Voice ret = new CBase_Voice();
        ret.roomID = roomID;
        ret.key = key;
        ret.url = url;
        return ret;
    }


}