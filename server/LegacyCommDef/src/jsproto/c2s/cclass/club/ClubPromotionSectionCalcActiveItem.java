package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubPromotionSectionCalcActiveItem {
    /**
     * 区间配置Id
     */
    private long unionSectionId;
    /**
     * 分配给自己的值
     */
    private double shareToSelfValue;
    /**
     * 变化标志
     */
    private boolean changFlag;
    /**
     * 开始值
     */
    private double beginValue;
    /**
     * 结束值
     */
    private double endValue;
}
