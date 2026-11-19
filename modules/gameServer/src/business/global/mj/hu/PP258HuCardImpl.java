package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.Hu258JiangKeUtil;

/**
 * 碰碰258胡
 *
 * @author Huaxing
 */
public class PP258HuCardImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        // 检查是否有吃
        if (!this.checkChi(mSetPos)) {
            return false;
        }
        return Hu258JiangKeUtil.getInstance().checkHu(mCardInit);
    }

}
