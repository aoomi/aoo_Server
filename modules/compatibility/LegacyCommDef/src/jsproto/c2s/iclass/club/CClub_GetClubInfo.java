package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
@Data
public class CClub_GetClubInfo extends BaseSendMsg {

    public long clubId;        //俱乐部ID 等于0获取所有的  大于0获取对应的俱乐部

    public static CClub_GetClubInfo make(long clubId) {
        CClub_GetClubInfo ret = new CClub_GetClubInfo();
        ret.clubId = clubId;
        return ret;
    }
}