package jsproto.c2s.iclass.family;

import jsproto.c2s.cclass.BaseSendMsg;

public class CPlayer_BindingFamily extends BaseSendMsg {
    public String pidStr;
    public int familyEnum = 0;

    public static CPlayer_BindingFamily make(String pidStr, int familyEnum) {
        CPlayer_BindingFamily ret = new CPlayer_BindingFamily();
        ret.pidStr = pidStr;
        ret.familyEnum = familyEnum;
        return ret;
    }
}