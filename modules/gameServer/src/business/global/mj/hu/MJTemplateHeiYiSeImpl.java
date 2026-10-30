package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 黑一色：由东、南、西、北、8筒中的牌组成的胡牌牌型
 *
 * @author leo_wi
 */
public class MJTemplateHeiYiSeImpl extends BaseHuCard {

    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        OpPointEnum oEnum = checkHeiYS(mSetPos, mCardInit);
        if (OpPointEnum.Not.equals(oEnum)) {
            return oEnum;
        }
        return OpPointEnum.HeiYiSe;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.HeiYiSe.equals(checkHuCardReturn(mSetPos, mCardInit));
    }


    /**
     * 检查黑一色
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected OpPointEnum checkHeiYS(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.publicCardTypeList().stream().anyMatch(k -> !checkHei(k))) {
            return OpPointEnum.Not;
        }
        if (mCardInit.getAllCardInts().stream().anyMatch(k -> !checkHei(k))) {
            return OpPointEnum.Not;
        }
        return OpPointEnum.HeiYiSe;
    }

    /**
     * 由东、南、西、北、8筒中的牌组成的胡牌牌型；
     *
     * @param card
     * @return
     */
    protected boolean checkHei(Integer card) {
        if (card > 100) {
            card = card / 100;
        }
        return (card >= 41 && card <= 44) || card == 38;
    }
}
