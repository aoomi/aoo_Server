package jsproto.c2s.iclass.union;

import lombok.Data;

@Data
public class CUnion_ScorePercentList extends CUnion_Base {
    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;

    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 0：百分比，1：固定值
     */
    private int type;

    public CUnion_ScorePercentList() {

    }

    public CUnion_ScorePercentList(long unionId, long clubId, long opClubId, long opPid, int pageNum, int type) {
        super(unionId, clubId);
        this.opClubId = opClubId;
        this.opPid = opPid;
        this.pageNum = pageNum;
        this.type = type;
    }
}
