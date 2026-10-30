package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 地胡：定义1、2可复选
 * 定义2：庄家出第一张后，第一个闲家出第一张之前胡牌都是地胡
 */
public class MJTemplateDiHu2Impl extends BaseHuCard {


    /**
     * 定义2：庄家出第一张后，第一个闲家出第一张之前胡牌都是地胡
     *
     * @param mSetPos
     * @return
     */
    public boolean checkDiHu2(AbsMJSetPos mSetPos) {
        if (mSetPos.sizeOutCardIDs() > 0) {
            return false;
        }
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        return true;
    }

    /**
     * 本方法启动前提是已经胡了
     *
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (checkDiHu2(mSetPos)) {
            return OpPointEnum.DiHu;
        }
        return OpPointEnum.Not;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.DiHu.equals(checkHuCardReturn(mSetPos, mCardInit));
    }
}

