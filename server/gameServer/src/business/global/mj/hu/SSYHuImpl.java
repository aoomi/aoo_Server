package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 十三幺：东、南、西、北、中、发、白板、一九万、一九条、一九筒  这十三张牌里任意一张牌组成对，而另十二张各一张，共计十四张，即构成十三幺，不需要组成胡牌牌型；
 *
 * @author Administrator
 */
public class SSYHuImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (Objects.isNull(mCardInit)) {
            return false;
        }
        // 检查十三幺
        return this.checkSSY(mSetPos, mCardInit);
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (Objects.isNull(mCardInit)) {
            return OpPointEnum.Not;
        }
        // 检查十三幺
        if (this.checkSSY(mSetPos, mCardInit)) {
            return OpPointEnum.SSY;
        }
        return OpPointEnum.Not;

    }


    /**
     * 检查十三幺
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
                sameInt++;
                if (sameInt > 1) {
                    return false;
                }
            }
            if (entry.getKey() < 40) {
                int value = entry.getKey() % 10;
                // 一九万、一九条、一九筒
                if (value == 1 || value == 9) {
                    continue;
                } else {
                    // 不是一九万、一九条、一九筒
                    return false;
                }
            }
        }
        return true;
    }
}
