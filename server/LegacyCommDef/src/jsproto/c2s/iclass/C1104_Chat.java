package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1104_Chat extends BaseSendMsg {

    public int type;
    public int quickID;
    public long targetID;
    public String content;


    public static C1104_Chat make(int type, int quickID, long targetID, String content) {
        C1104_Chat ret = new C1104_Chat();
        ret.type = type;
        ret.quickID = quickID;
        ret.targetID = targetID;
        ret.content = content;

        return ret;


    }
}