package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 重新登录
 */
public class C1009_ResetLogin extends BaseSendMsg {
    public long accountID;
    public int serverType;

    public static C1009_ResetLogin make(long accountID, int serverType) {
        C1009_ResetLogin ret = new C1009_ResetLogin();
        ret.accountID = accountID;
        ret.serverType = serverType;
        return ret;
    }
}
