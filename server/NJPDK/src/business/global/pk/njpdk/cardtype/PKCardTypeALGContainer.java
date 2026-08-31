package business.global.pk.njpdk.cardtype;

import jsproto.c2s.cclass.pk.BasePocker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 扑克牌型算法容器
 */
public abstract class PKCardTypeALGContainer {

    /**
     * 获取牌值
     *
     * @param card
     * @return
     */
    public int getCardValue(int card) {
        return BasePocker.getCardValue(card);
    }

    /**
     * 获取牌的牌值总数信息
     *
     * @param cardList
     * @return
     */
    public Map<Integer, Long> getValueCountMapByList(ArrayList<Integer> cardList) {
        notNull(cardList, "getValueCountMapByList_cardList");
        Map<Integer, Long> valueCountMap = cardList.stream().collect(Collectors.groupingBy(p -> getCardValue(p), Collectors.counting()));
        return valueCountMap;
    }

    /**
     * 获取牌的牌值列表信息
     *
     * @param cardList
     * @return
     */
    public Map<Integer, List<Integer>> getValueListMapByList(ArrayList<Integer> cardList) {
        notNull(cardList, "getValueListMapByList_cardList");
        Map<Integer, List<Integer>> valueListMap = cardList.stream().collect(Collectors.groupingBy(p -> getCardValue(p)));
        return valueListMap;
    }

    /**
     * 获取牌堆里最大的牌
     *
     * @param cardList
     * @param length
     * @return
     */
    public int getMaxCard(ArrayList<Integer> cardList, int length) {
        notNull(cardList, "getMaxCard_cardList");
        Map<Integer, List<Integer>> ccList = getValueListMapByList(cardList);
        return ccList.entrySet().stream().filter(m -> m.getValue().size() >= length).map(n -> n.getKey()).max((a, b) -> a > b ? 1 : -1).orElse(0);
    }

    public static void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalStateException(name + " is null");
        }
    }
}
