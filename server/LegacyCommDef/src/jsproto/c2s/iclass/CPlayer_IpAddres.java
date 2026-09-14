package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * IP地址
 *
 * @author Huaxing
 */
public class CPlayer_IpAddres extends BaseSendMsg {
    public String ip = "";

    public static CPlayer_IpAddres make(String ip) {
        CPlayer_IpAddres ret = new CPlayer_IpAddres();
        ret.ip = ip;
        return ret;
    }

}
