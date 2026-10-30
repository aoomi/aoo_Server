package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseAnGang implements OpCard {
    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int specialCard) {
        Map<Integer, Long> map = mSetPos.allCards().stream()
                // 筛选出所有的牌类型
                .map(k -> k.getType())
                // 检查等于金牌 或者 不是花牌
                .filter(k -> this.checkFilter(mSetPos, k))
                // 按牌类型分组
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map || map.size() <= 0) {
            return false;
        }
        // 遍历出相同类型 >= 4.
        for (Long value : map.values()) {
            if (value.intValue() >= 4) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        int type = cardID / 100;
        if (!this.checkFilter(mSetPos, type)) {
            // 金牌或者花牌。
            return false;
        }
        List<Integer> publicCard = new ArrayList<>();
        int fromPos = mSetPos.getPosID();
        publicCard.add(OpType.AnGang.value());
        publicCard.add(fromPos);
        publicCard.add(cardID);
        // 搜集牌
        List<Integer> tmp = mSetPos.allCards().stream().map(k -> k.getCardID()).filter(k -> k / 100 == type).collect(Collectors.toList());
        // 牌 < 4.
        if (tmp.size() < 4) {
            return false;
        }
        // 增加亮牌
        publicCard.addAll(tmp);
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(tmp);
        if (mSetPos.getHandCard().getType() != type) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.AnGang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, fromPos, OpType.AnGang);
        return true;
    }

    /**
     * 检查过滤器
     *
     * @param type 牌类型
     * @return
     */
    protected abstract boolean checkFilter(AbsMJSetPos mSetPos, int type);

}
