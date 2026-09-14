package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 玩家实名认证
 *
 * @author Huaxing
 */
public class CPlayer_RealAuthen extends BaseSendMsg {
    public String realName;
    public String realNumber;

    public static CPlayer_RealAuthen make(String realName, String realNumber) {
        CPlayer_RealAuthen ret = new CPlayer_RealAuthen();
        ret.realName = realName;
        ret.realNumber = realNumber;
        return ret;
    }


}
