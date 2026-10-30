package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuUtil;
import cenum.mj.OpType;

/**
 * 如果赢家胡牌，一张花牌都没有抓到、也没有开杠的话，即为无花无杠（平胡）。
 */
public class WHuWGangImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (mSetPos.getPosOpRecord().sizeHua() > 0) {
            // 有花
            return false;
        }
        if (mSetPos.getPublicCardList().stream().filter(k -> {
            return k.get(0) == OpType.Gang.value() ||
                    k.get(0) == OpType.JieGang.value() ||
                    k.get(0) == OpType.AnGang.value();
        }).findAny().isPresent()) {
            // 存在开杠
            return false;
        }
        return HuUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }


}
