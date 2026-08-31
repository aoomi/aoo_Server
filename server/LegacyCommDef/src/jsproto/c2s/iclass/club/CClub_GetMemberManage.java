package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;


public class CClub_GetMemberManage extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public int pageNum;//页数
    public String query;
    /**
     * 0 全部 1 在线
     */
    private int type;
    /**
     * 0 已经加入
     * 1 加入为批准
     * 2 退出未批准
     */
    private int pageType;

    /**
     * 1只显示负分
     */
    private int losePoint;

    public static CClub_GetMemberManage make(long clubId, int pageNum, String query) {
        CClub_GetMemberManage ret = new CClub_GetMemberManage();
        ret.clubId = clubId;
        ret.pageNum = pageNum;
        ret.query = query;
        return ret;
    }

    public int getPageType() {
        return pageType;
    }

    public void setPageType(int pageType) {
        this.pageType = pageType;
    }

    public long getClubId() {
        return clubId;
    }

    public int getPageNum() {
        return pageNum;
    }

    public String getQuery() {
        return query;
    }

    public int getType() {
        return type;
    }

    public int getLosePoint() {
        return losePoint;
    }

    public void setLosePoint(int losePoint) {
        this.losePoint = losePoint;
    }
}