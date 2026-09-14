package business.global.mj.scjymj.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.manage.OpCard;
import business.global.mj.scjymj.SCJYMJRoomSet;
import business.global.mj.scjymj.SCJYMJSetPos;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SCJYMJJieGangCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        SCJYMJSetPos jPos = (SCJYMJSetPos) mSetPos;
        return this.jieGang(jPos, cardID);
    }


    public boolean jieGang(SCJYMJSetPos mSetPos, int cardID) {
        MJCardInit mInit = mSetPos.mjCardInit(true);
        if (null == mInit) {
            return false;
        }
        // 获取牌的类型
        int type = cardID / 100;
        // 检查是否有金
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            return false;
        }

        Map<Integer, Long> map = mInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map) {
            return false;
        }
        if (map.containsKey(type)) {
            if (map.get(type) >= 3) {
                if (mSetPos.isTing()) {
                    List<Integer> huTypes = mSetPos.getHuCardTypes();
                    List<MJCard> mjCards = new ArrayList<>(mSetPos.allCards());
                    List<MJCard> removeCards = new ArrayList<>();
                    for (MJCard mjCard : mjCards) {
                        if (mjCard.getType() == type) {
                            removeCards.add(mjCard);
                            if (3 == removeCards.size()) {
                                break;
                            }
                        }
                    }
                    mjCards.removeAll(removeCards);
                    List<Integer> newHuTypes = MJFactory.getTingCard(mSetPos.getmActMrg()).checkTingCard(mSetPos, mjCards);
                    if (new HashSet<>(newHuTypes).containsAll(huTypes) && new HashSet<>(huTypes).containsAll(newHuTypes)) {
                        return true;
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).ownnerPos;
        int type = lastOutCard / 100;

        SCJYMJSetPos jPos = (SCJYMJSetPos) mSetPos;
        if (jPos.isTing()) {
            List<MJCard> allCards = mSetPos.allCards();
            boolean allow = false;
            List<MJCard> tempCards = new ArrayList<>(allCards);
            List<MJCard> gangCards = new ArrayList<>();
            for (MJCard cardTemp : tempCards) {
                if (cardTemp.getType() == type) {
                    gangCards.add(cardTemp);
                }
            }
            tempCards.removeAll(gangCards);
            List<Integer> tingCards = MJFactory.getTingCard(mSetPos.getmActMrg()).checkTingCard(mSetPos, tempCards);
            if (tingCards.size() > 0) {
                allow = true;
            }
            if (!allow) {
                return false;
            }

        }
        boolean ret = false;
        List<Integer> publicCard = new ArrayList<>();
        publicCard.add(OpType.JieGang.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);

        List<MJCard> tmp = new ArrayList<>();
        for (int i = 0; i < mSetPos.sizePrivateCard(); i++) {
            if (type == mSetPos.getPrivateCard().get(i).type) {
                tmp.add(mSetPos.getPrivateCard().get(i));
                if (tmp.size() >= 3) {
                    ret = true;
                    break;
                }
            }
        }

        if (ret) {
            publicCard.add(tmp.get(0).cardID);
            publicCard.add(lastOutCard);
            publicCard.add(tmp.get(1).cardID);
            publicCard.add(tmp.get(2).cardID);
            // 杠的炮，记录杠上炮
            ((SCJYMJSetPos) mSetPos).setGangCardId(lastOutCard);
            // 记录杠牌
            ((SCJYMJRoomSet) mSetPos.getSet()).addGangMap(lastOutCard, jPos.getPosID(), fromPos);
            mSetPos.addPublicCard(publicCard);
            mSetPos.removeAllPrivateCard(tmp);

        }
        return ret;

    }

}
