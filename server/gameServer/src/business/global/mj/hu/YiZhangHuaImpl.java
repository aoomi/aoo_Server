package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuUtil;

/**
 * 一张花：如果赢家胡牌，只有一张花的情况下，即为一张花（平胡）
 */
public class YiZhangHuaImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (mSetPos.getPosOpRecord().sizeHua() != 1) {
            // 花数 != 1
            return false;
        }
        return HuUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }

}
