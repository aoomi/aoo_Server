package business.global.mj.template;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.AbsTing;

/**
 * 模板麻将
 *
 * @author Huaxing
 */
public class MJTemplateTingImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(mSetPos, mCardInit);
    }

}
