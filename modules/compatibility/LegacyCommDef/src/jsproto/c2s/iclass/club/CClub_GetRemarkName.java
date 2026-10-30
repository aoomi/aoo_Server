package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;


public class CClub_GetRemarkName extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public String query;//查询  预留
    public long remarkID;//备注的玩家ID
    public String remarkName;//备注的名称

    public static CClub_GetRemarkName make(long clubId, int remarkID, String query, String remarkName) {
        CClub_GetRemarkName ret = new CClub_GetRemarkName();
        ret.clubId = clubId;
        ret.query = query;
        ret.remarkID = remarkID;
        ret.remarkName = remarkName;
        return ret;
    }


    public long getClubId() {
        return clubId;
    }

    public long getRemarkID() {
        return remarkID;
    }

    public String getQuery() {
        return query;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }
}