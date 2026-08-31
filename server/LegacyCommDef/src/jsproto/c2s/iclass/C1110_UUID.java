package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1110_UUID extends BaseSendMsg {
    public String gameName = "HALL";

    public static C1110_UUID make(String gameName) {
        C1110_UUID ret = new C1110_UUID();
        ret.gameName = gameName;
        return ret;


    }
}