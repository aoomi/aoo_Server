package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 联盟获取总收益和洗牌信息
 */
@Data
public class UnionSportPointInfo {

    /**
     * 总收益分成
     */
    private double sportPointIncome;

    public static String getItemsNameBySportsPoint() {
        return "sum(sportPointIncome) as sportPointIncome";
    }
}
