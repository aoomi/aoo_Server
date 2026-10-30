package business.global.mj.scjymj.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import business.global.mj.scjymj.SCJYMJRoomSet;
import business.global.mj.scjymj.SCJYMJSetPos;
import cenum.mj.OpType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SCJYMJGangCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        SCJYMJSetPos jPos = (SCJYMJSetPos) mSetPos;
//        if (jPos.isTing()) {
//            return false;
//        }
        return buGang(jPos);
    }

    public boolean buGang(SCJYMJSetPos mSetPos) {
        final List<Integer> cardTypeList = mSetPos.allCards().stream().map(k -> k.getType()).distinct()
                .collect(Collectors.toList());
        if (null == cardTypeList || cardTypeList.size() <= 0) {
            return false;
        }
        int lastOpCard;
        if (null != mSetPos.getSet().getPreRound() && mSetPos.getSet().getPreRound().getOpType() == OpType.Peng) {
            lastOpCard = mSetPos.getPublicCardList().get(mSetPos.getPublicCardList().size() - 1).get(2) / 100;
        } else {
            lastOpCard = 0;
        }
        return mSetPos.getPublicCardList().stream()
                .filter(k -> k.get(0) == OpType.Peng.value() && lastOpCard != k.get(2) / 100 && cardTypeList.contains(k.get(2) / 100)).findAny()
                .isPresent();
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        if (doBuGang(cardID / 100, cardID, mSetPos.getPublicCardList(), mSetPos)) {
            return true;
        }
        return false;
    }

    // 点 补杠
    private boolean doBuGang(int type, int carID, List<List<Integer>> publicCardList, AbsMJSetPos mSetPos) {
        if (Objects.isNull(publicCardList)) {
            return false;
        }
        // 是否对应牌类型的碰
        List<Integer> prePublicCard = publicCardList.stream()
                .filter(k -> k.get(0) == OpType.Peng.value() && k.get(2) / 100 == type).map(k -> k).findAny()
                .orElse(null);
        if (Objects.isNull(prePublicCard)) {
            return false;
        }
        // 找到手中对应牌类型的Id
        int id = mSetPos.allCards().stream().filter(k -> k.type == type).map(k -> k.cardID).findAny().orElse(0);
        if (id <= 0) {
            return false;
        }

        prePublicCard.set(0, OpType.Gang.value());
        prePublicCard.add(id);
        // 杠的炮，记录杠上炮
        ((SCJYMJSetPos) mSetPos).setGangCardId(id);
        ((SCJYMJRoomSet) mSetPos.getSet()).addGangMap(id, mSetPos.getPosID(), -1);

        if (mSetPos.getHandCard().type == type) {
            // 清理手牌
            mSetPos.cleanHandCard();
        } else {
            mSetPos.removePrivateCard(new MJCard(id));
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
            mSetPos.cleanHandCard();
        }
        return true;
    }

}
