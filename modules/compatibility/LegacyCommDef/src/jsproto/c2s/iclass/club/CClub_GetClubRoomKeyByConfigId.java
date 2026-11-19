package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取俱乐部房间ROOMKEY通过配置id
 * @author zaf
 *
 */
@Data
public class CClub_GetClubRoomKeyByConfigId extends BaseSendMsg {
	public long configId;
    public long clubId;
    public long unionId;
    public static CClub_GetClubRoomKeyByConfigId make(long configId,long unionId,long clubId) {
        CClub_GetClubRoomKeyByConfigId ret = new CClub_GetClubRoomKeyByConfigId();
        ret.configId = configId;
        ret.clubId=clubId;
        ret.unionId=unionId;
        return ret;
    }
}