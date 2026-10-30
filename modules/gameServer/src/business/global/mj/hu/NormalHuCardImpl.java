package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuUtil;

public class NormalHuCardImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        return HuUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }
}
