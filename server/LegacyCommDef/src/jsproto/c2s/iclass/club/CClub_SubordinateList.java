package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_SubordinateList extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;

    /**
     * 目标pid
     */
    private long pid;
    /**
     * 页数
     */
    private int pageNum;

    /**
     * 查询
     */
    private String query;

    /**
     * 分成类型（0：百分比，1：固定值）
     */
    private int type;
    /**
     * 区间信息的时候 是否赛事页面(1 是赛事 0 不是赛事)
     */
    private int unionFlag;
}
