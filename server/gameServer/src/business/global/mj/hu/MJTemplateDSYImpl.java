package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 大元：胡牌的时候手中有中刻子或杠+发刻子或杠+白刻子或杠；（参考游戏常州麻将）
 */
public class MJTemplateDSYImpl extends MJTemplateXSYImpl {
    /**
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        if (checkSanYuan(mSetPos, mCardInit, true)) {
            return OpPointEnum.DaSanYuan;
        }
        return OpPointEnum.Not;
    }


}
