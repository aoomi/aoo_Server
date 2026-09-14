package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1006_RoleLogin extends BaseSendMsg {

    public long accountID;


    public static C1006_RoleLogin make(long accountID) {
        C1006_RoleLogin ret = new C1006_RoleLogin();
        ret.accountID = accountID;

        return ret;


    }
}