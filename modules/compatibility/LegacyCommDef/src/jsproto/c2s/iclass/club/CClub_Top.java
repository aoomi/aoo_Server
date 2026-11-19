package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * @author zaf
 */
public class CClub_Top extends BaseSendMsg {
    public long clubId;//俱乐部ID

    public static CClub_Top make(long clubId) {
        CClub_Top ret = new CClub_Top();
        ret.clubId = clubId;
        return ret;
    }
}