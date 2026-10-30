package business.global.mj.ting;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.BTJHuCardImpl;
import business.global.mj.manage.MJFactory;

public class TingBanJinImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (MJFactory.getHuCard(BTJHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }
}
