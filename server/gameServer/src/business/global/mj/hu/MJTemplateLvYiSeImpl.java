package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 绿一色：由2、3、4、6、8条、发财中的牌组成的胡牌牌型；
 *
 * @author leo_wi
 */
public class MJTemplateLvYiSeImpl extends BaseHuCard {

    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        OpPointEnum oEnum = checkLYS(mSetPos, mCardInit);
        if (OpPointEnum.Not.equals(oEnum)) {
            return oEnum;
        }
        return OpPointEnum.LvYiSe;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.LvYiSe.equals(checkHuCardReturn(mSetPos, mCardInit));
    }


    /**
     * 检查绿一色 由2、3、4、6、8条、发财中的牌组成的胡牌牌型；
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected OpPointEnum checkLYS(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.publicCardTypeList().stream().anyMatch(k -> !checkLv(k))) {
            return OpPointEnum.Not;
        }
        if (mCardInit.getAllCardInts().stream().anyMatch(k -> !checkLv(k))) {
            return OpPointEnum.Not;
        }
        return OpPointEnum.LvYiSe;
    }

    protected boolean checkLv(Integer card) {
        if (card > 100) {
            card = card / 100;
        }
        return card == 22 || card == 23 || card == 24 || card == 26 || card == 28 || card == 46;
    }
}
