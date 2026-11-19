package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.AbsTing;
import cenum.mj.OpPointEnum;

import java.util.List;
import java.util.Objects;

/**
 * 捉五魁：胡顺子中的5，可以是5筒或5条或5万；例：手中有4万和6万，听4万5万6万顺子中的5万（参考任丘麻将）
 *
 * @author leo_wi
 */
public class MJTemplateHuShun5Impl extends BaseHuCard {

    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        if (Objects.isNull(mSetPos.getHandCard())) {
            return OpPointEnum.Not;
        }
        if (matchShunMid(mSetPos, mCardInit, mSetPos.getHandCard().type)) {
            return OpPointEnum.ZhuoWuKui;
        }
        return OpPointEnum.Not;
    }

    /**
     * 检查左顺子
     *
     * @param mSetPos
     * @param mCardInit 牌
     * @param cardType  类型
     * @return
     */
    public boolean matchShunMid(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType) {
        if (cardType > 40 || cardType % 10 != 5) {
            return false;
        }
        MJCardInit newAllInit = new MJCardInit(mCardInit.getAllCardInts(), mCardInit.getJins(), 0);
        Integer right1 = cardType + 1;
        Integer right2 = cardType - 1;
        return checkHu(mSetPos, newAllInit, right1, right2, cardType);
    }

    /**
     * 检查右顺子
     *
     * @param mSetPos
     * @param mCardInit 牌
     * @param cardType  类型
     * @return
     */
    public boolean matchShunRight(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType) {
        if (cardType % 10 != 3) {
            return false;
        }
        MJCardInit newAllInit = new MJCardInit(mCardInit.getAllCardInts(), mCardInit.getJins(), 0);
        Integer right1 = cardType - 1;
        Integer right2 = cardType - 2;
        return checkHu(mSetPos, newAllInit, right1, right2, cardType);
    }

    /**
     * @param mSetPos
     * @param newAllInit
     * @param type1
     * @param type2
     * @param type
     * @return
     */
    private boolean checkHu(AbsMJSetPos mSetPos, MJCardInit newAllInit, Integer type1, Integer type2, Integer type) {
        //将对应的顺子移除掉,再检查能不能胡
        List<Integer> cardInts = newAllInit.getAllCardInts();
        if (cardInts.remove(type) && cardInts.remove(type1) && cardInts.remove(type2)) {
            return ((AbsTing) MJFactory.getTingCard(mSetPos.getmActMrg())).tingHu(mSetPos, newAllInit);
        }
        return false;
    }
}
