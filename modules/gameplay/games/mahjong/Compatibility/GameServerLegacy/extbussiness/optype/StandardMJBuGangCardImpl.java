package business.global.mj.extbussiness.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomEnum;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.extbussiness.StandardMJSetPos;
import business.global.mj.manage.OpCard;
import cenum.mj.OpType;
import jsproto.c2s.iclass.mj._Promptly;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模板麻将补杠
 */
public class StandardMJBuGangCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        StandardMJSetPos jPos = (StandardMJSetPos) mSetPos;
        jPos.clearBuGangList();
        StandardMJRoomSet set = (StandardMJRoomSet) mSetPos.getSet();
        int sizeCard = set.getSetCard().getRandomCard().getSize();
        if (sizeCard <= set.getLeftSizeByOpType(OpType.Gang)) {
            return false;
        }
        List<MJCard> cards = mSetPos.allCards();

        List<List<Integer>> pengList = mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Peng.value()).map(k -> k.subList(3, k.size())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(pengList)) {
            for (int i = 0;i < cards.size(); i++) {
                final int num = i;
                Optional<List<Integer>> first = pengList.stream().filter(z -> z.get(0) / 100 == cards.get(num).type).findFirst();
                if(first.isPresent()){
                    List<Integer> matchedPeng = first.orElseThrow();
                    if (jPos.isTing()) {
                        if (jPos.checkBaotingGang(cards.get(num).type,OpType.Gang)) {
                            List<Integer> newList = new ArrayList<>(matchedPeng);
                            newList.add(cards.get(num).getCardID());
                            jPos.getBuGangList().add(newList);
                        }
                    } else {
                        List<Integer> newList = new ArrayList<>(matchedPeng);
                        newList.add(cards.get(num).getCardID());
                        jPos.getBuGangList().add(newList);
                    }
                }
            }
        }
        return CollectionUtils.isNotEmpty(jPos.getBuGangList());
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        StandardMJSetPos jPos = (StandardMJSetPos) mSetPos;

        List<Integer> buGangCardList = ((StandardMJSetPos) mSetPos).getChiCardList();
        if (buGangCardList == null || buGangCardList.size() < 4) {
            return false;
        }

        List<Integer> prePublicCard = null;
        for (int i = 0; i < jPos.getPublicCardList().size(); i++) {
            if (jPos.getPublicCardList().get(i).get(0) == OpType.Peng.value()) {
                if (buGangCardList.contains(jPos.getPublicCardList().get(i).get(3))) {
                    prePublicCard = jPos.getPublicCardList().get(i);
                }
            }
        }
        if (null == prePublicCard) {
            return false;
        }

        List<Integer> list = prePublicCard.subList(3, prePublicCard.size());
        List<Integer> addCardList = buGangCardList.stream().filter(z -> !list.contains(z)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(addCardList)) {
            return false;
        }
        // peng -》gang
        if (prePublicCard.get(0) == OpType.Peng.value()) {
            prePublicCard.set(0, OpType.Gang.value());
        }
        prePublicCard.add(addCardList.get(0));
        if (mSetPos.getHandCard() != null) {
            if (buGangCardList.contains(mSetPos.getHandCard().cardID)) {
                // 清理手牌
                mSetPos.cleanHandCard();
            } else {
                mSetPos.removePrivateCard(new MJCard(addCardList.get(0)));
                mSetPos.addPrivateCard(mSetPos.getHandCard());
                mSetPos.sortCards();
                mSetPos.cleanHandCard();
            }
        }
        if(((StandardMJRoom)mSetPos.getRoom()).isSSSG()){
            //普通补杠后立马算分
            ((StandardMJSetPos)mSetPos).resolveSSSGBuGang(prePublicCard.get(1),prePublicCard.get(2));
        }
        return true;
    }
}
