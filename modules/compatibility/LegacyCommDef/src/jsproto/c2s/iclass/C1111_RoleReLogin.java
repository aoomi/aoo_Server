package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1111_RoleReLogin extends BaseSendMsg {
    public long accountID;
    public String uuid;
    public String gameName = "HALL";

    public static C1111_RoleReLogin make(long accountID, String uuid, String gameName) {
        C1111_RoleReLogin ret = new C1111_RoleReLogin();
        ret.uuid = uuid;
        ret.gameName = gameName;
        ret.accountID = accountID;

        return ret;


    }
}