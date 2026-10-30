package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class S2119_InitPlayerFamily extends BaseSendMsg {

    public long familyID;

    public static S2119_InitPlayerFamily make(long familyID) {
        S2119_InitPlayerFamily ret = new S2119_InitPlayerFamily();
        ret.familyID = familyID;

        return ret;


    }
}