package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.AbsTing;
import cenum.mj.OpPointEnum;

import java.util.List;

/**
 * 边张：听一种牌面时，单胡123的3及789的7或1233的3、 7789胡7都为边张；
 * 手中有12345胡3、 56789胡7不算边张；
 * 坎张：听一种牌面时，胡顺子中间的牌；
 * 4556胡5也为坎张，手中有45567胡6不算坎张；
 * 单吊将：听一种牌面时，胡的牌为将牌的另外一张；
 *
 * @author leo_wi
 */
public class MJTemplateBianKanDiaoImpl extends DDHuCardImpl {


    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType) {
        if (cardType == 0) {
            return OpPointEnum.Not;
        }
        //单吊：胡牌的时候，手中的牌都碰杠了，只剩一对将；
        if (matcJiang(mSetPos, mCardInit, cardType)) {
            return OpPointEnum.DD;
        }
        if (cardType > 40) {
            return OpPointEnum.Not;
        }
        //89胡7；
        if (matchShunLeft(mSetPos, mCardInit, cardType)) {
            return OpPointEnum.BianZhang;
        }
        //12胡3
        if (matchShunRight(mSetPos, mCardInit, cardType)) {
            return OpPointEnum.BianZhang;
        }
        //胡的那张牌是顺子中间的那张；
        if (matchShunMid(mSetPos, mCardInit, cardType)) {
            return OpPointEnum.KanZhang;
        }
        return OpPointEnum.Not;

    }

    /**
     * 单吊：胡牌的时候，手中的牌都碰杠了，只剩一对将；
     *
     * @param cardType
     * @return
     */
    public boolean matcJiang(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType) {
        return mSetPos.allCards().size() == 2;
    }

    /**
     * 检查左顺子
     *
     * @param mSetPos
     * @param mCardInit 牌
     * @param cardType  类型
     * @return
     */
    public boolean matchShunLeft(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType) {
        if (cardType % 10 != 7) {
            return false;
        }
        MJCardInit newAllInit = new MJCardInit(mCardInit.getAllCardInts(), mCardInit.getJins(), 0);
        Integer right1 = cardType + 1;
        Integer right2 = cardType + 2;
        return checkHu(mSetPos, newAllInit, right1, right2, cardType);
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
        if (cardType % 10 + 1 > 9 || cardType % 10 - 1 < 1) {
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