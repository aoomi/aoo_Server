package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
public class CClub_Sort extends BaseSendMsg {
    /**
     * 排序类型
     */
    private int sort;
    /**
     * 亲友圈Id
     */
    private long clubId;

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
