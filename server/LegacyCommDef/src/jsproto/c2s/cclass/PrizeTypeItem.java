package jsproto.c2s.cclass;

import cenum.PrizeType;
import lombok.Data;

/**
 * 消耗类型项
 */

@Data
public class PrizeTypeItem {
    /**
     * 奖励类型
     */
    public PrizeType prizeType;
    /**
     * 数量
     */
    public int value;

    public PrizeTypeItem(PrizeType prizeType, int value) {
        this.prizeType = prizeType;
        this.value = value;
    }
}
