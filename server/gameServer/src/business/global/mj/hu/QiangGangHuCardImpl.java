package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import cenum.mj.OpType;

import java.util.List;

/**
 * 抢杠胡
 *
 * @author Huaxing
 */
public class QiangGangHuCardImpl extends BaseHuCard {

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
        AbsMJSetPos mSetPos = aSetPos.getSet().getMJSetPos(pos);
        if (null == mSetPos) {
            return;
        }
        int type = mSetPos.getSet().getLastOpInfo().getLastOpCard() / 100;
        Integer cardID = mSetPos.getSet().getLastOpInfo().getLastOpCard();
        List<Integer> prePublicCard = null;
        for (int i = 0; i < mSetPos.sizePublicCardList(); i++) {
            prePublicCard = mSetPos.getPublicCardList().get(i);
            if (prePublicCard.get(0) == OpType.Gang.value()) {
                if (prePublicCard.get(2) / 100 == type) {
                    break;
                }
            }
        }
        // peng <- gang
        if (null != prePublicCard) {
            prePublicCard.set(0, OpType.Peng.value());
            prePublicCard.remove(cardID);
        }
    }

}
