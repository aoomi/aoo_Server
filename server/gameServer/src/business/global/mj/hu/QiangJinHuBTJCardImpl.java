package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.TingBanJinImpl;

import java.util.List;

/**
 * 抢金 普通的白板替金抢金
 *
 * @author Huaxing
 */
public class QiangJinHuBTJCardImpl extends BaseHuCard {

    @Override
    public List<MJCard> qiangJinHuCard(AbsMJSetPos setPos) {
        return MJFactory.getTingCard(TingBanJinImpl.class).qangJinTingList(setPos, 0, setPos.getSet().getDPos() == setPos.getPosID());
    }

    @Override
    public boolean doQiangJin(AbsMJSetPos mSetPos, List<MJCard> qiangJinList) {
        mSetPos.setPrivateCard(qiangJinList);
        mSetPos.cleanHandCard();
        mSetPos.getCard(mSetPos.getSet().getmJinCardInfo().getJin(1));
        return true;
    }
}
