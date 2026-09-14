package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class C1008_Login extends BaseSendMsg {
    public long accountID;
    public String version;

    public static C1008_Login make(long accountID, String version) {
        C1008_Login ret = new C1008_Login();
        ret.accountID = accountID;
        ret.version = version;
        return ret;
    }
}
