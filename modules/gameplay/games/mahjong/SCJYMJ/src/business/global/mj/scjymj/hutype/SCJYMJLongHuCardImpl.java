package business.global.mj.scjymj.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.util.HuUtil;
import cenum.mj.MJCEnum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 	一条龙：同一门花色的123456789等连续牌叫一条龙
 *
 * @author Huaxing
 */
public class SCJYMJLongHuCardImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (mSetPos.sizePublicCardList() >= 2) {
            return false;
        }
        if (HuUtil.getInstance().checkHu(mCardInit)) {
            return checkQysLong(mCardInit.getAllCardInts());
        }
        return false;
    }

    // 检查清一色-扣牌麻将
    public boolean checkQysLong(List<Integer> allCards) {
        // 如果 碰杠的牌次数 >= 2，那么无法形成 一条龙
        boolean isTab = false;
        if (allCards.containsAll(MJCEnum.WA_LONG)) {
            isTab = checkLongHu(allCards, MJCEnum.WA_LONG);
        } else if (allCards.containsAll(MJCEnum.TI_LONG)) {
            isTab = checkLongHu(allCards, MJCEnum.TI_LONG);
        } else if (allCards.containsAll(MJCEnum.TO_LONG)) {
            isTab = checkLongHu(allCards, MJCEnum.TO_LONG);
        }
        return isTab;
    }

    // 去掉 一条龙，检查是否可以胡牌
    public boolean checkLongHu(List<Integer> allCards, List<Integer> cardLong) {
        List<Integer> aCards = new ArrayList<>(allCards);
        Integer mCard = null;
        Iterator<Integer> it = null;
        for (Integer cardType : cardLong) {
            it = aCards.iterator(); // 创建迭代器
            while (it.hasNext()) { // 循环遍历迭代器
                mCard = it.next();
                if (mCard.equals(cardType)) {
                    it.remove();
                    break;
                }
            }
        }
        if (HuUtil.getInstance().checkHu(aCards, 0)) {
            return true;
        }
        return false;
    }
}
