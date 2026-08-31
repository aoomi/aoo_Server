package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.iclass.union.CUnion_Base;
import lombok.Data;

/**
 * 赛事经营项
 */
@Data
public class CClub_RoomConfigPrizePoolItem extends BaseSendMsg {
    /**
     * 查询内容
     */
    private String query;

    /**
     * 类型 0:今天-1:昨天-2:前天
     */
    private int type;

    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 亲友圈Id
     */
    private long clubId;
}
