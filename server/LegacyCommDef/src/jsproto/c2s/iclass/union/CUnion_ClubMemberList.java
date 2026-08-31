package jsproto.c2s.iclass.union;

import lombok.Data;

@Data
public class CUnion_ClubMemberList extends CUnion_Base {
    /**
     * 被查询亲友圈的亲友圈Id
     */
    private long opClubId;
    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 查询内容
     */
    private String query;

    /**
     * 0所有，1只显示在线玩家
     */
    private int type;

    /**
     * 1只显示负分
     */
    private int losePoint;

}
