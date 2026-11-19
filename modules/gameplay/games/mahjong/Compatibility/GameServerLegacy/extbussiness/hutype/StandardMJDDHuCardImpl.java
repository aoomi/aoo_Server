package business.global.mj.extbussiness.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.util.HuDuiUtil;
import org.apache.commons.collections4.MapUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 麻将七对胡
 *
 * @author Huaxing
 */
public class StandardMJDDHuCardImpl extends BaseHuCard {
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
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardId) {
        if(!checkHuCard(mSetPos,mCardInit)){
            return null;
        }
        // 分组统计手上的相同类型的牌
        Map<Integer, Long> groupingByMap = mCardInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (MapUtils.isEmpty(groupingByMap)) {
            return null;
        }
        StandardMJPointItem item;
        //这个方法可以判断豪华七小对还是超豪华七小对
        int duiNum = getDuiNum(mCardInit,groupingByMap);
        item = new StandardMJPointItem();
        item.setDuiNum(duiNum);
//        item.addOpPointEnum(OpPoint.QD.name(), OpPoint.QD.value());
        return item;

    }

    /**
     * 用来统计
     *
     * @param mCardInit     米卡初始化
     * @param groupingByMap 分组的地图
     * @return int
     */
    private int getDuiNum(MJCardInit mCardInit, Map<Integer, Long> groupingByMap) {
        int countValue = 0;
        int laizi = mCardInit.getJins().size();
        Comparator<Map.Entry<Integer, Long>> longComparator = Comparator.comparing(Map.Entry::getValue);
        List<Map.Entry<Integer, Long>> sortList = groupingByMap.entrySet().stream().sorted(longComparator.reversed()).collect(Collectors.toList());

        // 遍历相同的数>=4
        for (Map.Entry<Integer, Long> map : sortList) {
            if (map.getValue() == 4) {
                countValue++;
            } else if (map.getValue() == 3) {
                if (laizi >= 1) {
                    laizi--;
                    countValue++;
                }
            } else if (map.getValue() == 2) {
                if (laizi >= 2) {
                    laizi = laizi - 2;
                    countValue++;
                }
            } else if (map.getValue() == 1) {
                if (laizi >= 3) {
                    laizi = laizi - 3;
                    countValue++;
                }
            }
        }

        if (laizi >= 4) {
            countValue++;
        }
        return countValue;
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return this.checkHuCardReturn(mSetPos, mCardInit, 0);
    }
}
