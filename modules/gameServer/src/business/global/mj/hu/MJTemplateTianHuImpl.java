package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 天胡：庄家起手没出一张牌就胡
 */
public class MJTemplateTianHuImpl extends BaseHuCard {


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
        OpPointEnum opPointEnum = OpPointEnum.Not;
        //没出过牌 没吃碰杠
        if (mSetPos.sizePublicCardList() > 0) {
            return opPointEnum;
        }
        if (mSetPos.sizeOutCardIDs() > 0) {
            return opPointEnum;
        }
        if (mSetPos.getPosID() != mSetPos.getSet().getDPos()) {
            return opPointEnum;
        }
        if (mSetPos.getSet().getPosDict().values().stream().anyMatch(k -> k.sizePublicCardList() > 0 || k.sizeOutCardIDs() > 0)) {
            return opPointEnum;
        }
        return OpPointEnum.TianHu;
    }
}

