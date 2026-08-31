package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuKeUtil;

/**
 * 碰碰胡
 *
 * @author Huaxing
 */
public class PPHuCardImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        // 检查是否有吃
        if (!this.checkChi(mSetPos)) {
            return false;
        }
        return HuKeUtil.getInstance().checkKeHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }

}
