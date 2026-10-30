package business.global.pk.alg.util;

import business.global.pk.alg.params.BaseOpCard;
import business.global.pk.alg.params.BasePKParameter;
import jsproto.c2s.cclass.pk.BasePocker;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhujianming
 * @date 2020-07-14 09:28
 * 抽象扑克牌型算法工具
 */
public abstract class BasePKALGUtil {
    /**
     * 二列表
     */
    public final List<Integer> twoValueList = new ArrayList<>(Arrays.asList(0x0F, 0x1F, 0x2F, 0x3F));
    /**
     * 火箭
     */
    private List<Integer> rocketCard = Arrays.asList(0x41, 0x42);

    /**
     * 获取牌值
     *
     * @param card 扑克牌索引
     * @return 牌值
     */
    public int getCardValue(int card) {
        return BasePocker.getCardValueEx(card);
    }

    /**
     * 获取牌的牌值总数信息
     *
     * @param cardList 扑克牌索引
     * @return 分组完毕的扑克牌值数
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, Long> groupingCountByCardValue(ArrayList<Integer> cardList) {
        if (CollectionUtils.isNotEmpty(cardList)) {
            return cardList.stream().collect(Collectors.groupingBy(this::getCardValue, Collectors.counting()));
        }
        return new HashMap();
    }

    /**
     * 获取牌的牌值列表信息
     *
     * @param cardList 扑克牌索引
     * @return 分组完毕的扑克牌列表
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, List<Integer>> groupingListByCardValue(ArrayList<Integer> cardList) {
        if (CollectionUtils.isNotEmpty(cardList)) {
            return cardList.stream().collect(Collectors.groupingBy(this::getCardValue));
        }
        return new HashMap();
    }

    /**
     * 筛选长度为{@code length}的牌索引列表<br>
     * 并选取出最大的牌值数
     *
     * @param cardList 扑克牌索引
     * @param length   长度
     * @return 最大的牌值数
     */
    public int filterMaxCard(ArrayList<Integer> cardList, int length) {
        if (CollectionUtils.isNotEmpty(cardList)) {
            Map<Integer, List<Integer>> groupingList = groupingListByCardValue(cardList);
            return groupingList.entrySet().stream().filter(m -> m.getValue().size() >= length).map(Map.Entry::getKey).max(Integer::compareTo).orElse(0);
        }
        return 0;
    }

    /**
     * 获取同牌值列表
     *
     * @param cardList    牌列表
     * @param lastMaxCard 比较大小的牌值
     * @return 3带
     */
    public List<Integer> getSameValueList(ArrayList<Integer> cardList, int lastMaxCard, int bodyNum, boolean needEqual) {
        Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
        Function<Map.Entry<Integer, List<Integer>>, Integer> sizeCollator = p -> p.getValue().size();
        Function<Map.Entry<Integer, List<Integer>>, Integer> keyCollator = Map.Entry::getKey;
        Optional<List<Integer>> firstGroup = valueListMap.entrySet().stream().filter(n -> n.getKey() > lastMaxCard && ((needEqual && n.getValue().size() == bodyNum) || (!needEqual && n.getValue().size() >= bodyNum))).sorted(Comparator.comparing(sizeCollator).thenComparing(keyCollator)).map(Map.Entry::getValue).findFirst();
        return firstGroup.map(integer -> new ArrayList(integer.subList(0, bodyNum))).orElseGet(ArrayList::new);
    }

    /**
     * 获取飞机
     *
     * @param cardList         牌列表
     * @param tailNum          每个带带的牌张数（带对子或者带2张传2,带1传1，不带传0）
     * @param tailIsDui        带牌是否是对子
     * @param lastTripleNum    上次3带个数（飞机数）
     * @param lastCompareValue 上次飞机比较牌值
     * @return 符合的飞机牌型
     */
    @SuppressWarnings("unchecked")
    public List<BaseOpCard> getPlane(ArrayList<Integer> cardList, int tailNum, boolean tailIsDui, int lastTripleNum, int lastCompareValue, boolean isEqual) {
        int minTripleNum = 2;
        int minBodyLength = 6;
        int twoValue = 15;
        if (cardList.size() >= minBodyLength) {
            List<BaseOpCard> baseOpCardList = new ArrayList<>();
            Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
            List<Integer> tripleList = valueListMap.entrySet().stream().filter(n -> n.getValue().size() >= 3).map(Map.Entry::getKey).sorted(Comparator.naturalOrder()).collect(Collectors.toList());//选出3张组合
            List<Integer> keyList = new ArrayList<>();
            List<Integer> bodyList = new ArrayList<>();
            if (tripleList.size() >= minTripleNum) {
                for (int j = 0; j < tripleList.size(); j++) {
                    if (tripleList.get(j) == twoValue) {
                        continue;
                    }
                    keyList.clear();
                    keyList.add(tripleList.get(j));
                    for (int i = j + 1; i < tripleList.size(); i++) {
                        bodyList.clear();
                        if (tripleList.get(i) - tripleList.get(i - 1) == 1 && tripleList.get(i) != twoValue) {
                            keyList.add(tripleList.get(i));
                            if (keyList.size() >= minTripleNum) {
                                for (Integer card : keyList) {
                                    bodyList.addAll(valueListMap.get(card).subList(0, 3));
                                }
                                int tripleNum = keyList.size();
                                if (lastTripleNum == keyList.size() || lastTripleNum == -1) {
                                    BaseOpCard baseOpCard = new BaseOpCard();
                                    baseOpCard.cardList.addAll(bodyList);
                                    baseOpCard.tripleNum = tripleNum;
                                    baseOpCard.compareValue = tripleList.get(i);
                                    ArrayList<Integer> cardCloneList = (ArrayList<Integer>) cardList.clone();
                                    cardCloneList.removeAll(bodyList);
                                    int size = tailIsDui ? (tailNum * tripleNum) / 2 : tailNum * tripleNum;
                                    int tailType = tailIsDui ? 2 : 1;
                                    for (int k = 0; k < size; k++) {//两对
                                        List<Integer> sameValueList = getSameValueList(cardCloneList, 0, tailType, isEqual);
                                        cardCloneList.removeAll(sameValueList);
                                        baseOpCard.cardList.addAll(sameValueList);
                                    }
                                    baseOpCardList.add(baseOpCard);
                                }
                            }
                            continue;
                        }
                        break;
                    }
                }
                return baseOpCardList.stream().filter(cc -> cc.compareValue > lastCompareValue).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 生成顺子
     *
     * @param cardList     牌列表
     * @param bodyLength   顺子长度
     * @param bodyNum      顺子个数,连队2，顺子1
     * @param compareValue 比较值
     * @return 顺子
     */
    public List<Integer> getStraight(ArrayList<Integer> cardList, int bodyLength, int bodyNum, int compareValue) {
        Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
        Integer[] keys = valueListMap.keySet().toArray(new Integer[0]);
        Arrays.sort(keys);
        for (int i = 0; i <= keys.length - bodyLength; i++) {
            List<Integer> straightList = new ArrayList<>(bodyLength * bodyNum);
            if (valueListMap.get(keys[i]).size() >= bodyNum && valueListMap.get(keys[i]).size() != 4) {
                straightList.addAll(valueListMap.get(keys[i]).subList(0, bodyNum));
                for (int j = 1; j < bodyLength; j++) {
                    int before = i + j - 1;
                    int current = i + j;
                    if (Math.abs(keys[current] - keys[before]) != 1) {
                        break;
                    }
                    if (valueListMap.get(keys[current]).size() < bodyNum) {
                        break;
                    }
                    straightList.addAll(valueListMap.get(keys[current]).subList(0, bodyNum));
                }
                if (straightList.stream().anyMatch(twoValueList::contains))
                    continue;
                if (straightList.size() != bodyLength * bodyNum) {
                    continue;
                }
                if (getCardValue(straightList.get(0)) > compareValue) {
                    return straightList;
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 获取单张
     *
     * @param basePKParameter 输入参数
     * @return 单牌
     */
    public BaseOpCard getSingle(BasePKParameter basePKParameter) {
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        int compareValue = basePKParameter.compareValue;
        Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
        Function<Map.Entry<Integer, List<Integer>>, Integer> sizeCollator = p -> p.getValue().size();
        Function<Map.Entry<Integer, List<Integer>>, Integer> keyCollator = Map.Entry::getKey;
        Optional<List<Integer>> singleCardKey = valueListMap.entrySet().stream().filter(n -> n.getKey() > compareValue).sorted(Comparator.comparing(sizeCollator).thenComparing(keyCollator)).map(n -> n.getValue().subList(0, 1)).findFirst();
        return singleCardKey.map(cards -> BaseOpCard.make(basePKParameter.targetType, 0,
                new ArrayList<>(cards), getCardValue(cards.get(0)))).orElse(null);
    }

    /**
     * 获取一对
     *
     * @param basePKParameter 牌列表
     * @return 对子
     */
    public BaseOpCard getPairs(BasePKParameter basePKParameter) {
        List<Integer> sameValueList = getSameValueList(basePKParameter.getCloneOpTypeCardList(), basePKParameter.compareValue, 2, false);
        if (CollectionUtils.isNotEmpty(sameValueList)) {
            return BaseOpCard.make(basePKParameter.targetType, 0, sameValueList, getCardValue(sameValueList.get(0)));
        }
        return null;
    }

    /**
     * 获取3不带
     *
     * @param basePKParameter 牌列表
     * @return 3不带
     */
    public BaseOpCard getThreeZone(BasePKParameter basePKParameter) {
        final int bodyNum = 3;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> threeZone = getSameValueList(cardList, lastMaxCard, bodyNum, true);
            if (CollectionUtils.isNotEmpty(threeZone) && threeZone.size() >= bodyNum) {
                return BaseOpCard.make(basePKParameter.targetType, 0, threeZone, getCardValue(threeZone.get(0)));
            }
        }
        return null;
    }

    /**
     * 获取3带1
     *
     * @param basePKParameter 牌列表
     * @return 牌
     */
    public BaseOpCard getThreeZoneWithA(BasePKParameter basePKParameter) {
        final int bodyNum = 3;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> threeZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(threeZone)) {
                cardList.removeAll(threeZone);
                if (cardList.size() == 1) {
                    threeZone.addAll(getSingle(cardList, getCardValue(threeZone.get(0))));
                } else {
                    List<Integer> filterCardList = new ArrayList<>();
                    filterCardList.add(getCardValue(threeZone.get(0)));
                    filterCardList.add(getCardValue(0x41));
                    filterCardList.add(getCardValue(0x42));
                    filterCardList.add(15);
                    threeZone.addAll(getFilterDai(cardList, filterCardList));
                }
                if (threeZone.size() == 4) {
                    return BaseOpCard.make(basePKParameter.targetType, 1, threeZone, getCardValue(threeZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取3带1对
     *
     * @param basePKParameter 牌列表
     * @return 3带1对
     */
    public BaseOpCard getThreeZoneWithPairs(BasePKParameter basePKParameter) {
        final int bodyNum = 3;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> threeZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(threeZone)) {
                cardList.removeAll(threeZone);
                threeZone.addAll(getSameValueList(cardList, 0, 2, false));
                if (threeZone.size() == 5) {
                    return BaseOpCard.make(basePKParameter.targetType, 2, threeZone, getCardValue(threeZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取4炸
     *
     * @param basePKParameter 牌列表
     * @return 4炸
     */
    public BaseOpCard getBomb(BasePKParameter basePKParameter) {
        List<Integer> sameValueList = getSameValueList(basePKParameter.getCloneOpTypeCardList(), basePKParameter.compareValue, 4, true);
        if (CollectionUtils.isNotEmpty(sameValueList)) {
            return BaseOpCard.make(basePKParameter.targetType, 0, sameValueList, getCardValue(sameValueList.get(0)));
        }
        return null;
    }

    /**
     * 获取4带1
     *
     * @param basePKParameter 牌列表
     * @return 4带1
     */
    public BaseOpCard getFourWithA(BasePKParameter basePKParameter) {
        final int bodyNum = 4;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> fourZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(fourZone)) {
                cardList.removeAll(fourZone);
                fourZone.addAll(getSameValueList(cardList, 0, 1, false));
                if (fourZone.size() == 5) {
                    return BaseOpCard.make(basePKParameter.targetType, 1, fourZone, getCardValue(fourZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取4带1对
     *
     * @param basePKParameter 牌列表
     * @return 4带一对
     */
    public BaseOpCard getFourWithPairs(BasePKParameter basePKParameter) {
        final int bodyNum = 4;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> fourZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(fourZone)) {
                cardList.removeAll(fourZone);
                fourZone.addAll(getSameValueList(cardList, 0, 2, false));
                if (fourZone.size() == 6) {
                    return BaseOpCard.make(basePKParameter.targetType, 2, fourZone, getCardValue(fourZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取4带2
     *
     * @param basePKParameter 牌列表
     * @return 4带2
     */
    public BaseOpCard getFourWithTwo(BasePKParameter basePKParameter) {
        final int bodyNum = 4;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> fourZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(fourZone)) {
                cardList.removeAll(fourZone);
                for (int i = 0; i < 2; i++) {//两张单牌
                    List<Integer> sameValueList = getSameValueList(cardList, 0, 1, false);
                    cardList.removeAll(sameValueList);
                    fourZone.addAll(sameValueList);
                }
                if (fourZone.size() == 6) {
                    return BaseOpCard.make(basePKParameter.targetType, 2, fourZone, getCardValue(fourZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取4带2对
     *
     * @param basePKParameter 牌列表
     * @return 4带2对
     */
    public BaseOpCard getFourWithTwoPairs(BasePKParameter basePKParameter) {
        final int bodyNum = 4;
        int lastMaxCard = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        if (CollectionUtils.isNotEmpty(cardList) && cardList.size() >= bodyNum) {
            List<Integer> fourZone = new ArrayList<>(getSameValueList(cardList, lastMaxCard, bodyNum, true));
            if (CollectionUtils.isNotEmpty(fourZone)) {
                cardList.removeAll(fourZone);
                for (int i = 0; i < 2; i++) {//两对
                    List<Integer> sameValueList = getSameValueList(cardList, 0, 2, false);
                    cardList.removeAll(sameValueList);
                    fourZone.addAll(sameValueList);
                }
                if (fourZone.size() == 8) {
                    return BaseOpCard.make(basePKParameter.targetType, 4, fourZone, getCardValue(fourZone.get(0)));
                }
            }
        }
        return null;
    }

    /**
     * 获取飞机
     *
     * @param tailNum     带的牌
     * @param baseOpCards 满足的飞机
     * @param sort        升序
     * @return 飞机
     */
    public BaseOpCard getFirstPlane(int tailNum, List<BaseOpCard> baseOpCards, int sort) {
        Function<BaseOpCard, Integer> sizeCollator = p -> p.tripleNum;
        Function<BaseOpCard, Integer> keyCollator = p -> p.compareValue;
        Comparator<BaseOpCard> sortEd;
        if (sort == 0) {
            sortEd = Comparator.comparing(sizeCollator).reversed().thenComparing(keyCollator);
        } else {
            sortEd = Comparator.comparing(sizeCollator).reversed().thenComparing(keyCollator).reversed();
        }
        Optional<BaseOpCard> firstGroup = baseOpCards.stream().filter(n -> (n.tripleNum > 0) && (n.tripleNum * (tailNum + 3) == n.cardList.size())).sorted(sortEd).findFirst();
        return firstGroup.orElse(null);
    }

    /**
     * 获取飞机不带
     *
     * @param basePKParameter 牌列表
     * @return 飞机
     */
    public BaseOpCard getPlaneBuDai(BasePKParameter basePKParameter) {
        int compareValue = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        int lastTripleNum = basePKParameter.lastTripleNum == 0 ? -1 : basePKParameter.lastTripleNum;
        List<BaseOpCard> baseOpCards = getPlane(cardList, 0, false, lastTripleNum, compareValue, false);
        BaseOpCard firstPlane = getFirstPlane(0, baseOpCards, 0);
        if (firstPlane != null && firstPlane.cardList.size() >= 6) {
            firstPlane.opCardType = basePKParameter.targetType;
            firstPlane.daiNum = 0;
            return firstPlane;
        }
        return null;
    }

    /**
     * 获取飞机带一张
     *
     * @param basePKParameter 牌列表
     * @return 飞机
     */
    public BaseOpCard getPlaneWithA(BasePKParameter basePKParameter, int sort) {
        int compareValue = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        ;
        int lastTripleNum = basePKParameter.lastTripleNum == 0 ? -1 : basePKParameter.lastTripleNum;
        List<BaseOpCard> baseOpCards = getPlane(cardList, 1, false, lastTripleNum, compareValue, false);
        BaseOpCard firstPlane = getFirstPlane(1, baseOpCards, sort);
        if (firstPlane != null && firstPlane.cardList.size() >= 8) {
            firstPlane.opCardType = basePKParameter.targetType;
            firstPlane.daiNum = 0;
            return firstPlane;
        }
        return null;
    }

    /**
     * 获取飞机带一对
     *
     * @param basePKParameter 牌列表
     * @return 飞机
     */
    public BaseOpCard getPlaneWithPairs(BasePKParameter basePKParameter) {
        int compareValue = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        int lastTripleNum = basePKParameter.lastTripleNum == 0 ? -1 : basePKParameter.lastTripleNum;
        List<BaseOpCard> baseOpCards = getPlane(cardList, 2, true, lastTripleNum, compareValue, false);
        BaseOpCard firstPlane = getFirstPlane(2, baseOpCards, 0);
        if (firstPlane != null && firstPlane.cardList.size() >= 10) {
            firstPlane.opCardType = basePKParameter.targetType;
            firstPlane.daiNum = 0;
            return firstPlane;
        }
        return null;
    }

    /**
     * 获取顺子
     *
     * @param basePKParameter 牌列表
     * @return 顺子
     */
    public BaseOpCard getStraight(BasePKParameter basePKParameter) {
        int compareValue = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        List<Integer> straight = getStraight(cardList, basePKParameter.lastTripleNum, 1, compareValue);
        if (straight.size() >= 5) {
            return BaseOpCard.make(basePKParameter.targetType, 0, straight, getCardValue(straight.get(0)), straight.size());
        }
        return null;
    }

    /**
     * 获取连队
     *
     * @param basePKParameter 牌列表
     * @return 顺子
     */
    public BaseOpCard getMultiPairs(BasePKParameter basePKParameter) {
        int compareValue = basePKParameter.compareValue;
        ArrayList<Integer> cardList = basePKParameter.getCloneOpTypeCardList();
        List<Integer> straight = getStraight(cardList, basePKParameter.lastTripleNum, 2, compareValue);
        if (straight.size() >= 6) {
            return BaseOpCard.make(basePKParameter.targetType, 0, straight, getCardValue(straight.get(0)), straight.size() / 2);
        }
        return null;
    }

    /**
     * 获取火箭
     *
     * @return 火箭牌值
     */
    public List<Integer> getRocketCard() {
        return rocketCard;
    }


    /**
     * 获取单张
     *
     * @param cardList  牌列表
     * @param filterKey 过滤的牌值
     * @return 单牌
     */
    public List<Integer> getSingle(ArrayList<Integer> cardList, int filterKey) {
        Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
        Function<Map.Entry<Integer, List<Integer>>, Integer> sizeCollator = p -> p.getValue().size();
        Function<Map.Entry<Integer, List<Integer>>, Integer> keyCollator = Map.Entry::getKey;
        Optional<Integer> singleCardKey = valueListMap.entrySet().stream().filter(n -> n.getKey() != filterKey).sorted(Comparator.comparing(sizeCollator).thenComparing(keyCollator)).map(Map.Entry::getKey).findFirst();
        return singleCardKey.map(key -> valueListMap.get(key).subList(0, 1)).orElseGet(ArrayList::new);
    }

    /**
     * 获取带牌(在带牌时，除非是只剩两手牌，否则不能带王或2。 )
     *
     * @param cardList  牌列表
     * @param filterKey 过滤的牌值
     * @return 单牌
     */
    public List<Integer> getFilterDai(ArrayList<Integer> cardList, List<Integer> filterKey) {
        Map<Integer, List<Integer>> valueListMap = groupingListByCardValue(cardList);
        Function<Map.Entry<Integer, List<Integer>>, Integer> sizeCollator = p -> p.getValue().size();
        Function<Map.Entry<Integer, List<Integer>>, Integer> keyCollator = Map.Entry::getKey;
        Optional<Integer> singleCardKey = valueListMap.entrySet().stream().filter(n -> !filterKey.contains(n.getKey()) && n.getValue().size() != 4).sorted(Comparator.comparing(sizeCollator).thenComparing(keyCollator)).map(Map.Entry::getKey).findFirst();
        return singleCardKey.map(key -> valueListMap.get(key).subList(0, 1)).orElseGet(ArrayList::new);
    }

}
