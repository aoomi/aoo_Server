package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * 定义3：19 筒19条 19万+东南西北中白发+456万筒条中的1张；
 *
 * @author leo_wi
 */
public class MJTemplateSSYHu456Impl extends SSYHuImpl {


    /**
     * 定义3：19 筒19条 19万+东南西北中白发+456万筒条中的1张；
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected boolean checkSSY(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 分组牌类型
        int sameInt = 0;
        Map<Integer, Long> groupingByMap = allInt.stream().collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        // 检查是否有分组
        if (null == groupingByMap || groupingByMap.size() <= 0) {
            return false;
        }
        for (Entry<Integer, Long> entry : groupingByMap.entrySet()) {
            // 检查相同的牌 > 2
            if (entry.getValue() > 2L) {
                return false;
            } else if (entry.getValue() == 2L) {
                return false;
            }
            if (entry.getKey() < 40) {
                int value = entry.getKey() % 10;
                // 一九万、一九条、一九筒
                if (value == 1 || value == 9) {
                    continue;
                } else if (value == 4 || value == 5 || value == 6) {
                    sameInt++;
                    if (sameInt > 1) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
