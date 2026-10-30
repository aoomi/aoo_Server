package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.OpType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class MJTemplateGangCardImpl extends MJTemplateBaseGang {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        setPos.getBuGangList().clear();
        checkBuGang(setPos, mSetPos.getPublicCardList(),
                mSetPos.getHandCard().type, mSetPos.getHandCard().cardID);
        for (int i = 0, size = mSetPos.sizePrivateCard(); i < size; i++) {
            checkBuGang(setPos, mSetPos.getPublicCardList(), mSetPos
                    .getPrivateCard().get(i).type, mSetPos.getPrivateCard()
                    .get(i).cardID);
        }
        return CollectionUtils.isNotEmpty(((MJTemplateSetPos) mSetPos).getBuGangList());
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int card) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;

        List<Integer> opCardList = setPos.getOpCardList();
        int cardID = opCardList.get(0);
        List<Integer> prePublicCard = setPos.getPublicCardList().stream().filter(k -> k.get(2) / 100 == cardID / 100).findFirst().get();
        if (prePublicCard == null) {
            return false;
        }
        //客户端显示需要
        prePublicCard.set(0, OpType.Gang.value());
        prePublicCard.add(cardID);
        // 增加亮牌
        mSetPos.removeAllPrivateCards(opCardList);
        if (!setPos.getOpCardList().contains(mSetPos.getHandCard().getCardID())) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.Gang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, prePublicCard.get(1), OpType.YaoGang);
        return true;
    }

    /**
     * 补杠
     *
     * @param publicCardList 已经亮出的牌
     * @param type           牌类型
     * @param cardID         牌号
     * @return
     */
    public boolean checkBuGang(MJTemplateSetPos mSetPos, List<List<Integer>> publicCardList, int type,
                               int cardID) {

        List<Integer> prePublicCard = null;
        List<Integer> buGangList;
        for (int i = 0; i < publicCardList.size(); i++) {
            prePublicCard = publicCardList.get(i);
            if (prePublicCard.get(0) == OpType.Peng.value()) {
                if (prePublicCard.get(2) / 100 == type && checkGang(mSetPos, type)) {
                    buGangList = new ArrayList<>();
                    buGangList.add(cardID);
                    buGangList.addAll(prePublicCard.subList(3, prePublicCard.size()));
                    mSetPos.addBuGangList(buGangList);
                    mSetPos.setOpCardId(cardID);
                }
            }
        }
        return false;
    }

}
