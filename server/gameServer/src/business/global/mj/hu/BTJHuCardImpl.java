package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.util.HuUtil;

import java.util.List;

public class BTJHuCardImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        return HuUtil.getInstance().checkBTJHu(mCardInit.getAllCardInts(), mCardInit.sizeJin(), (List<Integer>) mSetPos.getSet().getmJinCardInfo().getJinKeys());
    }

}
