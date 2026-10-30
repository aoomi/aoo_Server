package business.global.mj.scjymj.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
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

public class SCJYMJAnGangCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int specialCard) {
        SCJYMJSetPos jPos = (SCJYMJSetPos) mSetPos;
        return this.anGang(jPos);
    }

    public boolean anGang(SCJYMJSetPos mSetPos) {
        // 获取牌的类型
        Map<Integer, Long> map = mSetPos.allCards().stream().map(k -> k.getType()).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map) {
            return false;
        }
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue() >= 4) {
                if (mSetPos.isTing()) {
                    List<Integer> huTypes = mSetPos.getHuCardTypes();
                    List<MJCard> mjCards = new ArrayList<>(mSetPos.allCards());
                    List<MJCard> removeCards = new ArrayList<>();
                    for (MJCard mjCard : mjCards) {
                        if (mjCard.getType() == entry.getKey()) {
                            removeCards.add(mjCard);
                            if (4 == removeCards.size()) {
                                break;
                            }
                        }
                    }
                    mjCards.removeAll(removeCards);
                    List<Integer> newHuTypes = MJFactory.getTingCard(mSetPos.getmActMrg()).checkTingCard(mSetPos, mjCards);
                    if (huTypes.size() == 0 && newHuTypes.size() > 0) {
                        return true;
                    }
                    if (huTypes.size() > 0 && new HashSet<>(newHuTypes).containsAll(huTypes) && new HashSet<>(huTypes).containsAll(newHuTypes)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        int type = cardID / 100;
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
        int fromPos = mSetPos.getPosID();
        publicCard.add(OpType.AnGang.value());
        publicCard.add(fromPos);
        publicCard.add(cardID);

        // 搜集牌
        List<MJCard> tmp = new ArrayList<>();
        if (mSetPos.getHandCard().type == type) {
            tmp.add(mSetPos.getHandCard());
        }
        for (int i = 0; i < mSetPos.sizePrivateCard(); i++) {
            if (type == mSetPos.getPrivateCard().get(i).type) {
                tmp.add(mSetPos.getPrivateCard().get(i));
                if (tmp.size() >= 4) {
                    ret = true;
                    break;
                }
            }
        }

        if (ret) {
            // 增加亮牌
            publicCard.add(tmp.get(0).cardID);
            publicCard.add(tmp.get(1).cardID);
            publicCard.add(tmp.get(2).cardID);
            publicCard.add(tmp.get(3).cardID);
            // 杠的炮，记录杠上炮
            ((SCJYMJSetPos) mSetPos).setGangCardId(tmp.get(0).cardID);
            ((SCJYMJRoomSet) mSetPos.getSet()).addGangMap(tmp.get(0).cardID, jPos.getPosID(), -1);
            mSetPos.addPublicCard(publicCard);
            mSetPos.removeAllPrivateCard(tmp);
            if (mSetPos.getHandCard().type != type) {
                mSetPos.addPrivateCard(mSetPos.getHandCard());
                mSetPos.sortCards();
            }
            mSetPos.cleanHandCard();
        }

        return ret;

    }

}
