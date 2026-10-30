package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;
import cenum.mj.OpType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leo_wi
 * 门清
 * 门清：胡牌玩家没有吃牌，没有碰牌，没有碰杠，可以有直杠、暗杠；
 * 只检查牌型 不检测胡牌
 */
public class MJTemplateMenQingImpl extends BaseHuCard {
    /**
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        //大门清：没花牌、没杠（暗杠也不行）、没字牌暗刻、没吃、没碰，自摸胡牌的；（花数为0，且没吃）；（参考游戏 海安麻将）
        if (checkDaMenQing(mSetPos, mCardInit)) {
            return OpPointEnum.MenQianQing;
        }
        // 门清（门子）：没吃、没碰、没杠即胡牌的（可以暗杠）；
        if (mSetPos.getPublicCardList().stream().noneMatch(k -> k.get(0) != OpType.AnGang.value()) || mSetPos.sizePublicCardList() == 0) {
            return OpPointEnum.MenQing;
        }
        return OpPointEnum.Not;
    }

    /**
     * 大门清：没花牌、没杠（暗杠也不行）、没字牌暗刻、没吃、没碰，自摸胡牌的；（花数为0，且没吃）；（参考游戏 海安麻将）
     *
     * @param mSetPos
     * @return
     */
    public boolean checkDaMenQing(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.sizePublicCardList() > 0 || mSetPos.getPosOpRecord().sizeHua() > 0) {
            return false;
        }
        Map<Integer, List<Integer>> cardMap = mCardInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p));
        for (Map.Entry<Integer, List<Integer>> entry : cardMap.entrySet()) {
            if (entry.getValue().size() >= 3 && entry.getKey() > 40) {
                return false;
            }
        }
        return true;
    }
}


