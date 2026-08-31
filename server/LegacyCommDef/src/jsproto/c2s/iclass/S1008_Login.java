package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class S1008_Login extends BaseSendMsg {

    public long time;
    public int timeZone;
    public long startServerTime;
    public long defaultFamilyID;
    public byte isNeedCreateRole;


    public static S1004_Login make(long time, int timeZone, long startServerTime, long defaultFamilyID, byte isNeedCreateRole) {
        S1004_Login ret = new S1004_Login();
        ret.time = time;
        ret.timeZone = timeZone;
        ret.startServerTime = startServerTime;
        ret.defaultFamilyID = defaultFamilyID;
        ret.isNeedCreateRole = isNeedCreateRole;

        return ret;


    }
}