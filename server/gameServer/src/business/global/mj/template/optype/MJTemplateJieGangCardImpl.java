package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.OpType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class MJTemplateJieGangCardImpl extends MJTemplateBaseGang {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        int type = cardID / 100;
        if (!checkFilter(mSetPos, type)) {
            return false;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        setPos.getJieGangList().clear();
        List<Integer> collect = setPos.allCardIDs().stream().filter(k -> k / 100 == type).collect(Collectors.toList());
        if (collect.size() >= 3 && checkGang(setPos, type)) {
            List<Integer> jieGangList = new ArrayList<>();
            jieGangList.add(cardID);
            jieGangList.addAll(collect);
            setPos.addJieGangList(jieGangList);
        }
        return CollectionUtils.isNotEmpty(setPos.getJieGangList());
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        int lastOutCard = setPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).getOwnnerPos();
        int type = lastOutCard / 100;
        if (!checkFilter(mSetPos, type)) {
            // 金牌或者花牌。
            return false;
        }
        List<Integer> publicCard = new ArrayList<>();
        //客户端显示需要
        publicCard.add(OpType.JieGang.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);
        publicCard.addAll(setPos.getOpCardList());
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(setPos.getOpCardList());
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.JieGang, new LastOpTypeItem(mSetPos.getPosID(), lastOutCard));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), lastOutCard, fromPos, OpType.JieGang);
        return true;
    }

}
