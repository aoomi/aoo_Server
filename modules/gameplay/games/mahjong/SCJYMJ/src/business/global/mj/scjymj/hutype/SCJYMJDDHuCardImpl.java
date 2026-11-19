package business.global.mj.scjymj.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import business.global.mj.util.HuDuiUtil;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 七对胡
 *
 * @author Huaxing
 */
public class SCJYMJDDHuCardImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        //检查是否有碰杠吃
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        return HuDuiUtil.getInstance().checkDuiHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());

    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return SCJYMJOpPoint.Not;
        }
        //检查是否有碰杠吃
        if (mSetPos.sizePublicCardList() > 0) {
            return SCJYMJOpPoint.Not;
        }
        if (!HuDuiUtil.getInstance().checkDuiHu(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
            return SCJYMJOpPoint.Not;
        }

        // 分组统计手上的相同类型的牌
        Map<Integer, Long> groupingByMap = mCardInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == groupingByMap || groupingByMap.size() <= 0) {
            return SCJYMJOpPoint.QiDuiHu;
        }
        int countValue = 0;
        // 遍历相同的数>=4
        for (Entry<Integer, Long> map : groupingByMap.entrySet()) {
            if (map.getValue() >= 4) {
                countValue++;
            }
        }
        if (countValue >= 1) {
            // 龙七对（豪华七对）：玩家手牌为七对牌型，并且有四张牌是一样的，叫龙七对。不再计七对。
            return SCJYMJOpPoint.LongQiDuiHu;
        }
        return SCJYMJOpPoint.QiDuiHu;

    }
}
