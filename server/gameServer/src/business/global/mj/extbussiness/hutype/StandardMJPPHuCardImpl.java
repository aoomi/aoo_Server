package business.global.mj.extbussiness.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.util.HuKeUtil;

/**
 * 麻将碰碰胡：胡牌的时候手中全是碰牌刻子或杠，加一组对子；（即碰碰胡）
 *
 * @author Huaxing
 */
public class StandardMJPPHuCardImpl extends BaseHuCard {

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

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (checkHuCard(mSetPos, mCardInit)) {
            StandardMJPointItem item = new StandardMJPointItem();
//            item.addOpPointEnum(OpPoint.PPHu.name(), OpPoint.PPHu.value());
            return item;
        }
        return null;
    }
}
