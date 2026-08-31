package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员活跃度日报记录项
 */
@Data
public class ClubPromotionActiveReportFormItem {
    /**
     * 时间
     */
    private String dateTime;
    /**
     * 活跃值
     */
    private double value;

    public static String getItemsName() {
        return "date_time as dateTime, value";
    }
}
