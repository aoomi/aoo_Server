package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SPlayer_Address extends BaseSendMsg {
    public long pid = 0;
    public String address;

    public static SPlayer_Address make(long pid, String address) {
        SPlayer_Address ret = new SPlayer_Address();
        ret.pid = pid;
        ret.address = address;
        return ret;


    }
}