package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_PromotionList extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;

    /**
     * 页数
     */
    private int pageNum;

    /**
     * 查询的pid
     */
    private long pid;

    /**
     * 查询
     */
    private String query;
    /**
     * 日期
     * 0 1 2 3 4 5...
     * 今天 昨天前天...
     */
    private int type;
    /**
     * 等级查询
     */
    private List<Integer> levelQuery = new ArrayList<>();

    public CClub_PromotionList(long clubId, int pageNum, long pid, String query, int type) {
        this.clubId = clubId;
        this.pageNum = pageNum;
        this.pid = pid;
        this.query = query;
        this.type = type;
    }
}
