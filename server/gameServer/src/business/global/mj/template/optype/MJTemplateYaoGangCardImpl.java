package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class MJTemplateYaoGangCardImpl extends MJTemplateBaseGang {


    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        return false;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int opCard) {
        MJTemplateSetPos set_pos = (MJTemplateSetPos) mSetPos;
        int cardID = set_pos.getOpCardList().get(0);
        //手上有几张摇杠的牌
        List<Integer> cards = set_pos.allCardIDs().stream().filter(k -> k / 100 == cardID / 100).collect(Collectors.toList());
        //如果是4张是暗杠
        boolean result = false;
        if (cards.size() == 4) {
            result = doAnGangOpCard(mSetPos, cards);
        } else if (cards.size() == 1) {
            result = doBuGangOpCard(mSetPos, cards);
        } else if (cards.size() == 3) {
            result = doJieGangOpCard(mSetPos, cards);
        }
        set_pos.setYaoGang(result);
        return result;
    }

    /**
     * 摇杠的暗杠
     *
     * @param mSetPos
     * @param cards   手上要杠的牌
     * @return
     */
    protected boolean doAnGangOpCard(AbsMJSetPos mSetPos, List<Integer> cards) {
        Integer cardID = cards.get(0);
        List<Integer> publicCard = new ArrayList<>();
        int fromPos = mSetPos.getPosID();
        //客户端显示需要
        publicCard.add(OpType.AnGang.value());
        publicCard.add(fromPos);
        publicCard.add(cardID);
        // 增加亮牌
        publicCard.addAll(cards);
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(cards);
        if (!java.util.Objects.equals(mSetPos.getHandCard().getCardID(), cardID)) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.YaoGang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, fromPos, OpType.YaoGang);
        return true;
    }

    /**
     * 摇杠的接杠
     *
     * @param mSetPos
     * @param cards
     * @return
     */
    protected boolean doJieGangOpCard(AbsMJSetPos mSetPos, List<Integer> cards) {
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).getOwnnerPos();
        int type = lastOutCard / 100;
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            // 金牌或者花牌。
            return false;
        }
        List<Integer> publicCard = new ArrayList<>();
        //客户端显示需要
        publicCard.add(OpType.JieGang.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);
        publicCard.add(lastOutCard);
        publicCard.addAll(cards);
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(cards);
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.YaoGang, new LastOpTypeItem(mSetPos.getPosID(), lastOutCard));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), lastOutCard, fromPos, OpType.YaoGang);
        return true;
    }

    /**
     * 摇杠的补杠
     *
     * @param mSetPos
     * @param cards
     * @return
     */
    protected boolean doBuGangOpCard(AbsMJSetPos mSetPos, List<Integer> cards) {
        MJTemplateSetPos set_pos = (MJTemplateSetPos) mSetPos;
        List<Integer> opCardList = set_pos.getOpCardList();
        Integer cardID = opCardList.get(0);
        List<Integer> prePublicCard = set_pos.getPublicCardList().stream().filter(k -> k.get(2) / 100 == cardID / 100).findFirst().orElse(null);
        if (prePublicCard == null) {
            return false;
        }
        //客户端显示需要
        prePublicCard.set(0, OpType.Gang.value());
        prePublicCard.addAll(cards);
        // 增加亮牌
        mSetPos.removeAllPrivateCards(cards);
        if (!java.util.Objects.equals(mSetPos.getHandCard().getCardID(), cardID)) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.YaoGang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, prePublicCard.get(1), OpType.YaoGang);
        return true;
    }


}
