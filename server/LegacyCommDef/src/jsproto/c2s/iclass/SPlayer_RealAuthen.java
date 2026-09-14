package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 玩家实名认证
 * @author Huaxing
 *
 */
public class SPlayer_RealAuthen extends BaseSendMsg {
	public String realName;
	public String realNumber;
	
    public static SPlayer_RealAuthen make(String realName, String realNumber) {
    	SPlayer_RealAuthen ret = new SPlayer_RealAuthen();
        ret.realName = realName;
        ret.realNumber = realNumber;
        return ret;
    }
	
	
}
