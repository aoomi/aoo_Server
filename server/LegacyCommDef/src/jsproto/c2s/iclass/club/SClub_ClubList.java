package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubInfo;

import java.util.List;

/**
 * 获取俱乐部链表
 *
 * @author zaf
 */
public class SClub_ClubList extends BaseSendMsg {

    public List<ClubInfo> clubInfoList;

    public static SClub_ClubList make(List<ClubInfo> clubInfoList) {
        SClub_ClubList ret = new SClub_ClubList();
        ret.clubInfoList = clubInfoList;
        return ret;
    }
}