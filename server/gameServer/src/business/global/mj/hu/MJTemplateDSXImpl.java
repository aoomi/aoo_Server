package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

/**
 * 小四喜 东西南北均 其中一个为将对  两外三个3张以上
 */
public class MJTemplateDSXImpl extends MJTemplateXSXImpl {
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
        if (checkSiXi(mSetPos, mCardInit, true)) {
            return OpPointEnum.DaSiXi;
        }
        return OpPointEnum.Not;
    }

}
