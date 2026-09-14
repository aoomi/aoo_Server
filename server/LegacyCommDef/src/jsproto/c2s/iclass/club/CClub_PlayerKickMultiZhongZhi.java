package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 *加入俱乐部
 * @author zaf
 *
 */
public class CClub_PlayerKickMultiZhongZhi extends BaseSendMsg {

	public long clubId;//俱乐部编号
	public List<Long> pidList;//踢出

    public static CClub_PlayerKickMultiZhongZhi make(long clubId, List<Long> pidList) {
        CClub_PlayerKickMultiZhongZhi ret = new CClub_PlayerKickMultiZhongZhi();
        ret.clubId = clubId;
        ret.pidList = pidList;
        return ret;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public List<Long> getPidList() {
        return pidList;
    }

    public void setPidList(List<Long> pidList) {
        this.pidList = pidList;
    }
}