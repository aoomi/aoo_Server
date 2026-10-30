package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.ting.AbsTing;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class MJTemplateAnGangCardImpl extends MJTemplateBaseGang {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int specialCard) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        setPos.getAnGangList().clear();
        Map<Integer, List<Integer>> typeMap = setPos.allCardIDs().stream().collect(Collectors.groupingBy(k -> k / 100));
        for (Map.Entry<Integer, List<Integer>> entry : typeMap.entrySet()) {
            //检查失败
            if (!checkFilter(mSetPos, entry.getKey())) {
                continue;
            }
            if (entry.getValue().size() >= 4 && checkGang(setPos, entry.getKey())) {
                setPos.addAnGangList(new ArrayList<>(entry.getValue()));
            }
        }
        return CollectionUtils.isNotEmpty(setPos.getAnGangList());
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int card) {
        MJTemplateSetPos set_pos = (MJTemplateSetPos) mSetPos;
        Integer cardID = set_pos.getOpCardList().get(0);
        List<Integer> publicCard = new ArrayList<>();
        int fromPos = mSetPos.getPosID();
        //客户端显示需要
        publicCard.add(OpType.AnGang.value());
        publicCard.add(fromPos);
        publicCard.add(cardID);
        // 增加亮牌
        publicCard.addAll(set_pos.getOpCardList());
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(set_pos.getOpCardList());
        if (!set_pos.getOpCardList().contains(mSetPos.getHandCard().getCardID())) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.AnGang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, fromPos, OpType.AnGang);
        return true;
    }

    /**
     * 检测杠玩 是否还能听牌
     *
     * @return
     */
    public boolean checkGangHouTing(AbsMJSetPos mSetPos, MJCardInit mCardInit, int type) {
        List<Integer> cards = new ArrayList<>();
        for (int card : mCardInit.getAllCardInts()) {
            if (card == type) {
                cards.add(card);
            }
        }
        mCardInit.getAllCardInts().removeAll(cards);
        mCardInit.getJins().add(MJSpecialEnum.NOT_JIN.value());
        if (((AbsTing) MJFactory.getTingCard(mSetPos.getmActMrg())).tingHu(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }
}
