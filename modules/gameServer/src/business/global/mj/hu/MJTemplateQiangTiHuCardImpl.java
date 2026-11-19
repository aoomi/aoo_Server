package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.HuType;
import cenum.mj.OpType;

import java.util.List;

/**
 * 抢杠胡
 *
 * @author Huaxing
 */
public class MJTemplateQiangTiHuCardImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos aSetPos) {
        recvQiangGang(aSetPos);
        aSetPos.setHandCard(new MJCard(aSetPos.getSet().getLastOpInfo().getLastOpCard()));
        return true;
    }

    public void recvQiangGang(AbsMJSetPos aSetPos) {
        int pos = aSetPos.getSet().getLastOpInfo().getLastOpPos();
        if (pos <= -1) {
            return;
        }
        MJTemplateSetPos mSetPos = (MJTemplateSetPos) aSetPos.getSet().getMJSetPos(pos);
        if (null == mSetPos) {
            return;
        }
        int type = mSetPos.getSet().getLastOpInfo().getLastOpCard() / 100;
        Integer cardID = mSetPos.getSet().getLastOpInfo().getLastOpCard();
        List<Integer> prePublicCard = null;
        for (int i = 0; i < mSetPos.sizePublicCardList(); i++) {
            prePublicCard = mSetPos.getPublicCardList().get(i);
            if (prePublicCard.get(2) / 100 == type) {
                break;
            }
        }
        if (null != prePublicCard) {

            if (prePublicCard.get(0) == OpType.Gang.value()) {
                prePublicCard.set(0, OpType.RuanGang.value());
            } else if (prePublicCard.get(0) == OpType.JieGang.value()) {
                prePublicCard.set(0, OpType.RuanJieGang.value());
            } else if (prePublicCard.get(0) == OpType.Peng.value()) {
                prePublicCard.set(0, OpType.RuanPeng.value());
            } else if (prePublicCard.get(0) == OpType.AnGang.value()) {
                prePublicCard.set(0, OpType.RuanAnGang.value());
            }
            if (mSetPos.getHandCard() != null) {
                mSetPos.addPrivateCard(mSetPos.getHandCard());
                mSetPos.cleanHandCard();
            }
            MJCard jinCard = mSetPos.allCards().stream().filter(k -> mSetPos.getSet().getmJinCardInfo().checkJinExist(k.cardID)).findFirst().get();
            prePublicCard.remove(cardID);
            prePublicCard.add(jinCard.cardID);
            mSetPos.setHuType(HuType.DianPao);
        }
    }

}
