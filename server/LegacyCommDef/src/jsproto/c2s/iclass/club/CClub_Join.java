package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class CClub_Join extends BaseSendMsg {
    /**
     * 俱乐部编号
     */
    public int clubSign;

    public static CClub_Join make(int clubSign) {
        CClub_Join ret = new CClub_Join();
        ret.clubSign = clubSign;
        return ret;
    }
}