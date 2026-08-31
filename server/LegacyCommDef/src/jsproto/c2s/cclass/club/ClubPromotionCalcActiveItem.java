package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubPromotionCalcActiveItem {
    /**
     * 房间配置Id
     */
    private long configId;
    /**
     * 收益百分比
     */
    private double value;
}
