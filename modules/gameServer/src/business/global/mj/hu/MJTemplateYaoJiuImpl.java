package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 全幺九（部分游戏也算在幺九胡里）：手中全是一、九序数牌和字牌；（参考韶关麻将）
 * 一、九、字牌可以是碰牌；
 *
 * @author leo_wi
 */
public class MJTemplateYaoJiuImpl extends BaseHuCard {
    /**
     * 幺九只有牌型 不检测胡牌
     *
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (Objects.isNull(mCardInit)) {
            return OpPointEnum.Not;
        }
        if (this.checkYaoJiu(mSetPos, mCardInit)) {
            // 带字牌的幺九
            if (mCardInit.getAllCardInts().stream().anyMatch(k -> k > 40)) {
                return OpPointEnum.HunYaoJiu;
            }
            return OpPointEnum.QingYaoJiu;
        }
        return OpPointEnum.Not;

    }


    /**
     * 幺九：由幺九（即1、9）牌和东、南、西、北、中、发、白，组成的胡牌牌型；
     * 不必同时有幺、九，只要是幺或九就行；
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected boolean checkYaoJiu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        //如果全都是幺九
        if (checkAllYaoJiuCard(mSetPos, mCardInit)) {
            //如果都可以胡牌
            return true;
        }
        return false;
    }

    /**
     * 检查是否都是yaojiu 的牌
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected boolean checkAllYaoJiuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 获取打出公共牌列表
        allInt.addAll(this.publicCardList(mSetPos));
        // 分组牌类型
        Map<Integer, Long> groupingByMap = allInt.stream().collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        // 检查是否有分组
        if (null == groupingByMap || groupingByMap.size() <= 0) {
            return false;
        }
        for (Map.Entry<Integer, Long> entry : groupingByMap.entrySet()) {
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

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return !OpPointEnum.Not.equals(checkHuCardReturn(mSetPos, mCardInit));
    }
}
