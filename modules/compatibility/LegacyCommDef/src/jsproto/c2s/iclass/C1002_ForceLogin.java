package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1002_ForceLogin extends BaseSendMsg {
    /**
     * 登录key
     */
    public int loginkey;
    /**
     * 用户Pid
     */
    public long pid;


    public static C1002_ForceLogin make(int loginkey, long pid) {
        C1002_ForceLogin ret = new C1002_ForceLogin();
        ret.loginkey = loginkey;
        ret.pid = pid;

        return ret;


    }
}