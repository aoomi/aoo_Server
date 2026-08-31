package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 赛事经营项
 */
@Data
public class CUnion_RoomConfigPrizePoolItem extends CUnion_Base {
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
}
