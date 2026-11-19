package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuDuiUtil;
import cenum.mj.OpPointEnum;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 金可以替代的对对胡
 *
 * @author Huaxing
 */
public class MJTemplateDDHuJinCardImpl extends BaseHuCard {
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
            return OpPointEnum.Not;
        }
        //检查是否有碰杠吃
        if (mSetPos.sizePublicCardList() > 0) {
            return OpPointEnum.Not;
        }
        if (!HuDuiUtil.getInstance().checkDuiHu(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
            return OpPointEnum.Not;
        }

        // 分组统计手上的相同类型的牌
        Map<Integer, Long> groupingByMap = mCardInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == groupingByMap || groupingByMap.size() <= 0) {
            return OpPointEnum.QDHu;
        }
        int countValue = 0;
        int jin = mCardInit.sizeJin();
        // 遍历相同的数>=4
        for (Entry<Integer, Long> map : groupingByMap.entrySet()) {
            if (map.getValue() >= 4) {
                countValue++;
            } else if (map.getValue() == 3 && jin > 0) {
                countValue++;
                jin--;
            } else if (map.getValue() == 1 && jin > 0) {
                jin--;
            }
            if (jin <= 0) {
                break;
            }
        }
        if (countValue < 3) {
            countValue += jin / 2;
        }
        if (countValue == 1) {
            // 豪华对对胡
            return OpPointEnum.HDDHu;
        } else if (countValue == 2) {
            // 超级豪华对对胡
            return OpPointEnum.CHDDHu;
        } else if (countValue == 3) {
            // 超超级豪华对对胡
            return OpPointEnum.CCHDDHu;
        }
        return OpPointEnum.QDHu;

    }
}
