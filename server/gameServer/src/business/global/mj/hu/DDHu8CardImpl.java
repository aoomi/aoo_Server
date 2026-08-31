package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuBDuiUtil;
import cenum.mj.MJOpPointEnum;

/**
 * 对对胡
 *
 * @author Huaxing
 */
public class DDHu8CardImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        //检查是否有碰杠吃
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        return HuBDuiUtil.getInstance().checkDuiHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }


    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return MJOpPointEnum.Not;
        }
        //检查是否有碰杠吃
        if (mSetPos.sizePublicCardList() > 0) {
            return MJOpPointEnum.Not;
        }
        int totalJin = mCardInit.sizeJin();
        // 检查是否8对胡
        if (!HuBDuiUtil.getInstance().checkHu(mCardInit)) {
            return MJOpPointEnum.Not;
        }
        // 没金
        if (totalJin <= 0) {
            return MJOpPointEnum.W_Hu;
        } else {
            // 金牌归本身
            MJCardInit mInit = this.mCaiGui(mCardInit.getAllCardInts(), mCardInit.getJins(), mCardInit.sizeJin());
            if (null != mInit) {
                if (!HuBDuiUtil.getInstance().checkHu(mInit)) {
                    return MJOpPointEnum.W_Hu;
                }
            }
            return MJOpPointEnum.J_Hu;
        }
    }

}
