package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import cenum.mj.OpPointEnum;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 七对的额外牌型
 * 七连对 相连的七个对子
 * 一色双龙 固定是 112233 55 667788
 *
 * @author leo_wi
 */
public class MJTemplateQiDuiImpl extends DDHuCardImpl {


    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        OpPointEnum opPointEnum = (OpPointEnum) super.checkHuCardReturn(mSetPos, mCardInit);
        if (OpPointEnum.Not.equals(opPointEnum)) {
            return opPointEnum;
        }
        if (MJFactory.getHuCard(QingYiSeImpl.class).checkHuCard(mSetPos, mCardInit)) {
            //七连对
            if (checkLianDui(mCardInit, 7)) {
                return OpPointEnum.LianQiDui;
            }
            //一色双龙 112233 55 778899
            if (checkYiSeShuangLong(mCardInit)) {
                return OpPointEnum.YiSeShuangLong;
            }
        }
        if (checkLianDui(mCardInit, 6)) {
            return OpPointEnum.XSQDDSG;
        } else if (checkLianDui(mCardInit, 5)) {
            return OpPointEnum.XSQDDEG;

        } else if (checkLianDui(mCardInit, 4)) {
            return OpPointEnum.XSQDDYG;

        } else if (checkLianDui(mCardInit, 3)) {
            return OpPointEnum.XSQD;
        }
        return OpPointEnum.QDHu;

    }

    /**
     * @param mCardInit
     * @param lianNum   几连
     * @return
     */
    public boolean checkLianDui(MJCardInit mCardInit, int lianNum) {
        //花色
        Map<Integer, List<Integer>> colorMap = mCardInit.getAllCardInts().stream().collect(Collectors.groupingBy(k -> k / 10));
        for (Map.Entry<Integer, List<Integer>> entry : colorMap.entrySet()) {
            if (entry.getValue().size() < lianNum * 2) {
                continue;
            }
            if (checkLian(entry.getValue(), lianNum)) {
                return true;
            }
        }
        return false;

    }

    /**
     * 检查是否相连
     *
     * @param checkList
     * @param lianNum
     */
    public boolean checkLian(List<Integer> checkList, int lianNum) {
        //检查连续存在
        int count = 0;
        Set<Integer> ranks = checkList.stream().map(card -> card % 10).collect(Collectors.toSet());
        for (int i = 1; i < 9; i++) {
            if (!ranks.contains(i)) {
                count = 0;
            } else {
                count++;
            }
            if (count >= lianNum) {
                return true;
            }
        }
        return count == lianNum;
    }

    /**
     * 一色双龙：由一种花色的2个老少副和一对5为将牌组成的胡牌；  123 123 55 789 789
     *
     * @param mCardInit
     * @return
     */
    public boolean checkYiSeShuangLong(MJCardInit mCardInit) {

        //不能存在 4，6
        if (mCardInit.getAllCardInts().stream().anyMatch(k -> k % 10 == 4 || k % 10 == 6)) {
            return false;
        }
        Map<Integer, Long> collect = mCardInit.getAllCardInts().stream().map(k -> k % 10).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        int sizeJin = mCardInit.sizeJin();
        Long value;
        for (int i = 1; i <= 9; i++) {
            if (i == 4 || i == 6) {
                continue;
            }
            value = collect.get(i);
            if (value == null) {
                sizeJin -= 2;
            } else if (value > 2) {
                //此时三张以上的不符
                return false;
            } else {
                sizeJin -= 2 - value;
            }
            if (sizeJin < 0) {
                return false;
            }
        }

        return true;

    }

    /**
     * 检测七连
     *
     * @param mCardInit
     * @return
     */
    public boolean checkQiLian(MJCardInit mCardInit) {
        //字牌不是
        if (mCardInit.getAllCardInts().stream().anyMatch(k -> k > 40)) {
            return false;
        }
        Map<Integer, Long> collect = mCardInit.getAllCardInts().stream().collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        Integer min = collect.keySet().stream().min(Integer::compareTo).get();
        Integer max = collect.keySet().stream().max(Integer::compareTo).get();
        //最大值跟最小值 相差6
        if (min + 6 != max) {
            return false;
        }
        return collect.values().stream().allMatch(k -> k == 2);
    }
}
