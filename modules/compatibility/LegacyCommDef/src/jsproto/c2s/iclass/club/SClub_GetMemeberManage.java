package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubMemberManage;

import java.util.HashMap;

/**
 * 获取俱乐部房间信息
 *
 * @author zaf
 */
public class SClub_GetMemeberManage extends BaseSendMsg {


    public HashMap<Long, ClubMemberManage> memberManagers;


    public static SClub_GetMemeberManage make(HashMap<Long, ClubMemberManage> memberManagers) {
        SClub_GetMemeberManage ret = new SClub_GetMemeberManage();
        ret.memberManagers = memberManagers;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}