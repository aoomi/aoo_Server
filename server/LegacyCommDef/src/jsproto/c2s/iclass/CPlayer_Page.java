package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 玩家页
 *
 * @author Administrator
 */
public class CPlayer_Page extends BaseSendMsg {
    public int pageNum;

    public static CPlayer_Page make(int pageNum) {
        CPlayer_Page ret = new CPlayer_Page();
        ret.pageNum = pageNum;
        return ret;


    }
}