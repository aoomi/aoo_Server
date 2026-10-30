package business.global.mj.ting;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.mj.manage.MJFactory;

public class TingNormalImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }


}
