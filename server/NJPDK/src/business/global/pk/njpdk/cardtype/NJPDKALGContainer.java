package business.global.pk.njpdk.cardtype;

import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NJPDKALGContainer extends PKCardTypeALGContainer {

    /**
     * 2的牌值
     */
    private final int twoValue = 15;
    /**
     * 飞机最小长度2
     */
    private final int minPlaneLength = 2;
    /**
     * 二列表
     */
    private final List<Integer> twoList = new ArrayList<>(Arrays.asList(0x0F, 0x1F, 0x2F, 0x3F));

    /**
     * 一列表
     */
    private List<Integer> ACECard = Arrays.asList(0x1E, 0x2E, 0x3E);//3A


    private static class Singleton {
        private final static NJPDKALGContainer instance = new NJPDKALGContainer();
    }

    public static NJPDKALGContainer getInstance() {
        return NJPDKALGContainer.Singleton.instance;
    }

    /**
     * 获取3A1炸（智能按规则获取）
     *
     * @return
     */
    public List<Integer> getBomb3AWith1ByCardList(ArrayList<Integer> cardList) {
        boolean is3A = cardList.containsAll(ACECard);
        if (is3A && cardList.size() >= 3) {
            cardList.removeAll(ACECard);
            List<Integer> tailList = getTailList(cardList, 1, 1, null, 0, 0);
            tailList.addAll(ACECard);
            return tailList;
        }
        return new ArrayList<>();
    }

    /**
     * 获取3带1（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getThreeZoneWithAByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 3, 1, 0);
    }

    /**
     * 获取3带（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getThreeZoneByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 3, 0, 0);
    }

    /**
     * 获取3带2（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getThreeZoneWithTwoByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 3, 2, 0);
    }

    /**
     * 获取3带1对（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getThreeZoneWithPairsByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 3, 0, 1);
    }

    /**
     * 获取一对（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getAPairsByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 2, 0, 0);
    }

    /**
     * 获取4带1（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getBombWith1ByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 4, 1, 0);
    }

    /**
     * 获取4炸（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getBombByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return generateNormalTypeCC(cardList, lastMaxCard, 4, 0, 0);
    }

    /**
     * 获取单张（智能按规则获取）
     *
     * @return
     */
    public List<Integer> getSingleByCardList(ArrayList<Integer> cardList, int lastMaxCard) {
        return getTailList(cardList, 1, 1, null, lastMaxCard, 0);
    }

    /**
     * 获取飞机带一对（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getPlaneWithPairsByCardList(ArrayList<Integer> cardList, int planeCompareValue, int privateListSize, int lastPlaneLength) {
        List<TargetCardContainer> ccList = generatePlaneCC(cardList, 2, true, lastPlaneLength, planeCompareValue);
        return getLastMaxPlane(2, ccList, privateListSize);
    }

    /**
     * 获取飞机带一张（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getPlaneWithAByCardList(ArrayList<Integer> cardList, int planeCompareValue, int privateListSize, int lastPlaneLength) {
        List<TargetCardContainer> ccList = generatePlaneCC(cardList, 1, false, lastPlaneLength, planeCompareValue);
        return getLastMaxPlane(1, ccList, privateListSize);
    }

    /**
     * 获取飞机带二张（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getPlaneWithTwoByCardList(ArrayList<Integer> cardList, int planeCompareValue, int privateListSize, int lastPlaneLength) {
        List<TargetCardContainer> ccList = generatePlaneCC(cardList, 2, false, lastPlaneLength, planeCompareValue);
        return getLastMaxPlane(2, ccList, privateListSize);
    }

    /**
     * 获取飞机（智能按规则获取）
     *
     * @return
     */
    public TargetCardContainer getPlaneByCardList(ArrayList<Integer> cardList, int planeCompareValue, int privateListSize, int lastPlaneLength) {
        List<TargetCardContainer> ccList = generatePlaneCC(cardList, 0, false, lastPlaneLength, planeCompareValue);
        return getLastMaxPlane(0, ccList, privateListSize);
    }

    /**
     * 获取联队（智能按规则获取）
     *
     * @param cardList
     * @param bodyLength
     * @param compareValue
     * @return
     */
    public List getMultiPairsByCardList(ArrayList<Integer> cardList, int bodyLength, int compareValue) {
        return generateStraightTypeCC(cardList, bodyLength, 2, compareValue);
    }

    /**
     * 获取顺子（智能按规则获取）
     *
     * @param cardList
     * @param bodyLength
     * @param compareValue
     * @return
     */
    public List getStraightByCardList(ArrayList<Integer> cardList, int bodyLength, int compareValue) {
        return generateStraightTypeCC(cardList, bodyLength, 1, compareValue);
    }

    /**
     * 获取最大的飞机（规则有变时候，重写该方法）
     */
    public TargetCardContainer getLastMaxPlane(int tailNum, List<TargetCardContainer> ccList, int privateListSize) {
        TargetCardContainer cc = new TargetCardContainer();
        for (TargetCardContainer cl : ccList) {
            int totalTailNum = cl.planeLength * tailNum;
            int totalSize = tailNum * cl.planeLength + cl.planeLength * 3;
            if (cl.planeLength > 0 && privateListSize >= cl.planeLength * 3) {
                if ((cl.tailList.size() == totalTailNum) || (privateListSize < totalSize && cl.tailList.size() < totalTailNum)) {
                    if (cl.planeLength > cc.planeLength || (cl.planeLength == cc.planeLength && cl.planeCompareValue > cc.planeCompareValue)) {
                        cc = cl;
                    }
                }
            }
        }
        return cc;
    }

    /**
     * 获取比cardValue值大的组合
     * 支持牌型，单张，对子，3带1，3带一对，3带二，4带1，4炸，3不带
     *
     * @param cardList     [0x04,0x14,0x24,0x05,0x03]
     * @param compareValue 3
     * @param bodyNum      3
     * @return [0x04, 0x14, 0x24]
     */
    private TargetCardContainer generateNormalTypeCC(ArrayList<Integer> cardList, int compareValue, int bodyNum, int tailPartNumber, int duiNum) {
        TargetCardContainer cc = new TargetCardContainer();
        if (cardList == null || cardList.size() < bodyNum) {
            return cc;
        }
        generateBodyListByCompareValue(cardList, compareValue, bodyNum, cc);
        int filterKey = getFilterZoneCardValue(cc.bodyList, bodyNum, tailPartNumber);
        //去除主牌
        cardList.removeAll(cc.bodyList);
        //加上对子带牌
        cardList.removeAll(getTailList(cardList, duiNum, 2, cc, 0, filterKey));
        //加上单张带牌
        cardList.removeAll(getTailList(cardList, tailPartNumber, 1, cc, 0, filterKey));
        return cc;
    }

    /**
     * 获取不能带的牌(安岳3带1，不能带同主体的牌，不然变成炸弹)
     */
    private int getFilterZoneCardValue(List<Integer> bodyList, int bodyNum, int daiNum) {
        int filterDaiNum = (bodyNum == 3 && daiNum == 1) ? (bodyList.size() > 0 ? getCardValue(bodyList.get(0)) : 0) : 0;
        return filterDaiNum;
    }

    /**
     * 对比牌值和张数生成所需的牌
     *
     * @param cardList
     * @param compareValue
     * @param bodyNum
     */
    private void generateBodyListByCompareValue(ArrayList<Integer> cardList, int compareValue, int bodyNum, TargetCardContainer cc) {
        notNull(cc, "generateBodyListByCompareValue_cc");
        Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
        valueListMap.entrySet().stream().forEach(m -> {
            if (m.getKey() > compareValue && m.getValue().size() >= bodyNum) {
                if (cc.bodyList.size() == 0) {//设置默认
                    cc.setCard(m.getKey(), m.getValue());
                } else {
                    // 优先选择n张，n+1张，n+2张，再优先根据牌值从小到大排序 ,n=bodyNum
                    if ((cc.bodyList.size() == m.getValue().size() && cc.cardValue > m.getKey()) || m.getValue().size() < cc.bodyList.size()) {
                        cc.setCard(m.getKey(), m.getValue());
                    }
                }
            }
        });
        // 保留实际需要的张数
        if (cc.bodyList.size() > bodyNum) {
            cc.bodyList = new ArrayList<>(cc.bodyList.subList(0, bodyNum));
        }
    }

    /**
     * 获取带牌 支持带的是对子，带的是单张
     *
     * @param cardList     牌列表
     * @param partNumber   份数 2对就是2份，2张就是2份
     * @param cardNumber   张数 对子=2，单张=1
     * @param cc           容器
     * @param compareValue
     * @param filterKey
     * @return
     */
    public List<Integer> getTailList(ArrayList<Integer> cardList, int partNumber, int cardNumber, TargetCardContainer cc, int compareValue, int filterKey) {
        if (partNumber <= 0) {
            return new ArrayList<>();
        }
        cc = cc == null ? new TargetCardContainer() : cc;
        Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
        generateTailKeyListByCompareValue(partNumber, cardNumber, cc, compareValue, filterKey, valueListMap);
        generateTailListByTailKeyList(partNumber, cardNumber, cc, valueListMap);
        cc.tailKeyList.clear();
        return cc.tailList;
    }

    /**
     * 对比牌值和张数生成所需的带牌的key
     *
     * @param partNumber
     * @param cardNumber
     * @param cc
     * @param compareValue
     * @param filterKey
     * @param valueListMap
     */
    private void generateTailKeyListByCompareValue(int partNumber, int cardNumber, TargetCardContainer cc, int compareValue, int filterKey, Map<Integer, List<Integer>> valueListMap) {
        for (Map.Entry<Integer, List<Integer>> m : valueListMap.entrySet()) {
            if (m.getValue().size() >= cardNumber && m.getKey() > compareValue) {
                if (filterKey != m.getKey()) {
                    if (cc.tailKeyList.size() < partNumber) {
                        cc.tailKeyList.add(m.getKey());//先填满所需的key
                    } else {
                        for (int i = 0; i < cc.tailKeyList.size(); i++) {
                            //带牌，按牌值长度大小排序，短的优先选择，然后按牌值大小排序，小的优先选择
                            if ((valueListMap.get(cc.tailKeyList.get(i)).size() == m.getValue().size() && cc.tailKeyList.get(i) > m.getKey()) || valueListMap.get(cc.tailKeyList.get(i)).size() > m.getValue().size()) {
                                cc.tailKeyList.set(i, m.getKey());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 通过key生成带牌
     *
     * @param partNumber
     * @param cardNumber
     * @param cc
     * @param valueListMap
     */
    private void generateTailListByTailKeyList(int partNumber, int cardNumber, TargetCardContainer cc, Map<Integer, List<Integer>> valueListMap) {
        //缺少的带的份数，一对就是一份，一张也是一份
        int needDaiNum = partNumber - cc.tailKeyList.size();
        for (Integer card : cc.tailKeyList) {
            if (valueListMap.get(card).size() >= cardNumber) {
                cc.tailList.addAll(valueListMap.get(card).subList(0, cardNumber));
                valueListMap.get(card).removeAll(valueListMap.get(card).subList(0, cardNumber));
            }
            //缺少的牌，优先重tailKey寻找，如果规则有变可重写
            if (needDaiNum > 0) {
                while (valueListMap.get(card).size() >= cardNumber) {
                    if (needDaiNum <= 0) {
                        break;
                    }
                    cc.tailList.addAll(valueListMap.get(card).subList(0, cardNumber));
                    valueListMap.get(card).removeAll(valueListMap.get(card).subList(0, cardNumber));
                    needDaiNum--;
                }
            }
        }
    }

    /**
     * 生成飞机
     * 3带一的长度是(3+1)*m
     * 3带二的长度是(3+2)*m
     * 3带的长度是3m+n*2
     * m最大是2，最小是0
     *
     * @param cardList
     * @param tailNum               3带1传1 ，3带2传2，3带传0
     * @param tailIsDui             3带一对:true，其他:false
     * @param lastPlaneLength       比较的飞机长度，不需要比较的传-1
     * @param lastPlaneCompareValue 比较的飞机值，不需要比较的传0
     * @return
     */
    private List<TargetCardContainer> generatePlaneCC(ArrayList<Integer> cardList, int tailNum, boolean tailIsDui, int lastPlaneLength, int lastPlaneCompareValue) {
        if (cardList.size() < 6) {
            return new ArrayList<>();
        }
        List<TargetCardContainer> ccList = new ArrayList<>();
        List<Integer> keyList = new ArrayList<>();
        List<Integer> bodyList = new ArrayList<>();
        Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
        List<Integer> value3List = valueListMap.entrySet().stream().filter(n -> n.getValue().size() >= 3).map(k -> k.getKey()).collect(Collectors.toList());//选出3张组合
        //List<Integer> value3List = valueListMap.entrySet().stream().filter(n -> n.getValue().size() >= 3).map(k -> k.getKey()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());//选出3张组合
        if (value3List.size() < 2) {
            return new ArrayList<>();
        }
        for (int j = 0; j < value3List.size(); j++) {
            if (value3List.get(j) == twoValue) {
                continue;
            }
            keyList.clear();
            keyList.add(value3List.get(j));
            for (int i = j + 1; i < value3List.size(); i++) {
                bodyList.clear();
                if (value3List.get(i) - value3List.get(i - 1) == 1 && value3List.get(i) != twoValue) {
                    keyList.add(value3List.get(i));
                    if (keyList.size() >= minPlaneLength) {
                        for (Integer card : keyList) {
                            bodyList.addAll(valueListMap.get(card).subList(0, 3));
                        }
                        int planeLength = keyList.size();
                        if (lastPlaneLength == keyList.size() || lastPlaneLength == -1) {
                            TargetCardContainer cc = new TargetCardContainer();
                            cc.setPlane(planeLength, bodyList, value3List.get(i));
                            ArrayList<Integer> tailList = (ArrayList<Integer>) cardList.clone();
                            tailList.removeAll(bodyList);
                            if (!tailIsDui) {
                                cc.tailList.addAll(getTailList(tailList, tailNum * planeLength, 1, null, 0, 0));
                            } else {
                                cc.tailList.addAll(getTailList(tailList, tailNum * planeLength / 2, 2, null, 0, 0));
                            }
                            ccList.add(cc);
                        }
                    }
                    continue;
                }
                break;
            }
        }
        return ccList.stream().filter(cc -> cc.planeCompareValue > lastPlaneCompareValue).collect(Collectors.toList());
    }

    /**
     * 生成顺子
     *
     * @param cardList
     * @param bodyLength
     * @param bodyNum
     * @param compareValue
     * @return
     */
    private List generateStraightTypeCC(ArrayList<Integer> cardList, int bodyLength, int bodyNum, int compareValue) {
        Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
        Integer[] keys = valueListMap.keySet().toArray(new Integer[valueListMap.size()]);
//        Arrays.sort(keys);//升序key
        for (int i = 0; i <= keys.length - bodyLength; i++) {
            List<Integer> straightList = new ArrayList<>(bodyLength * bodyNum);
            if (valueListMap.get(keys[i]).size() < bodyNum) {
                continue;
            }
            if (valueListMap.get(keys[i]).size() >= bodyNum) {
                straightList.addAll(valueListMap.get(keys[i]).subList(0, bodyNum));
            }
            for (int j = 1; j < bodyLength; j++) {
                if (Math.abs(keys[i + j] - keys[i + j - 1]) != 1) {
                    break;
                }
                if (valueListMap.get(keys[i + j]).size() < bodyNum) {
                    break;
                }
                straightList.addAll(valueListMap.get(keys[i + j]).subList(0, bodyNum));
            }
            if (straightList.size() != bodyLength * bodyNum) {
                continue;
            }
            if (straightList.stream().anyMatch(n -> twoList.contains(n)))
                continue;
            if (getCardValue(straightList.get(straightList.size() - 1)) > compareValue) {
                return straightList;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 检测是否满足牌型（不支持连带，联队，顺子）
     * 支持的牌型3带1，3带2，3不带，对子,3带1对,4带1，4带2，4带一对，4炸
     *
     * @param cardList
     * @param bodyNum
     * @param tailNum
     * @param pairsNum
     * @param partNum
     * @return true
     * @type 3带1（[],3,1,1,0）
     * @type 3带2（[],3,1,2,0）
     * @type 3带2对（[],3,1,2,1）
     * @type 3不带（[],3,1,0,0）
     * @type 对子([], 2, 1, 0, 0)
     * ...........
     */
    public boolean checkNormalType(ArrayList<Integer> cardList, int bodyNum, int partNum, int tailNum, int pairsNum) {
        if (cardList == null || cardList.size() != partNum * bodyNum + tailNum || tailNum < pairsNum * 2) {
            return false;
        } else {
            Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
            List<Integer> cards = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> n : valueListMap.entrySet()) {
                if (n.getValue().size() >= bodyNum) {
                    cards.addAll(valueListMap.get(n.getKey()).subList(0, bodyNum));
                }
            }
            cardList.removeAll(cards);
            valueListMap = getValueListMapByList(cardList);
            long pairsCount = valueListMap.entrySet().stream().filter(m -> m.getValue().size() >= 2).count();
            return cards.size() == partNum * bodyNum && pairsCount >= pairsNum;
        }
    }

    /**
     * 检测是否顺子
     * 支持牌型，单顺（牌数大于5），双顺（连队），三顺（飞机不带牌），四顺（连炸不带牌）
     *
     * @return 是否是顺子
     * @type 单顺（[],1）
     * @type 双顺（[],2）
     * @type 飞机（[],3）
     * .....
     */
    public boolean checkStraight(ArrayList<Integer> cardList, int bodyNum) {
        //单顺子必须大于或者5张
        if (bodyNum == 1 && cardList.size() < 5) {
            return false;
        }
        Map<Integer, List<Integer>> valueListMap = getValueListMapByList(cardList);
        List<Integer> straightList = valueListMap.entrySet().stream().filter(n -> n.getValue().size() == bodyNum).map(m -> m.getKey()).collect(Collectors.toList());
        if (straightList.stream().anyMatch(n -> twoList.contains(n))) {
            return false;
        }
        return (straightList.size() == cardList.size() / bodyNum) && straightList.stream().reduce(Integer::max).get() - straightList.stream().reduce(Integer::min).get() == (cardList.size() / bodyNum) - 1;
    }

    /**
     * 是否是3A炸
     *
     * @param lastOpCardType
     * @param lastCardList
     * @return
     */
    public boolean is3A(int lastOpCardType, ArrayList<Integer> lastCardList) {
        //四带一可以压所有，除了3A炸
        List<Integer> aCeCard = Arrays.asList(0x1E, 0x2E, 0x3E);//3A
        return lastOpCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value() && lastCardList.containsAll(aCeCard);
    }

    public static void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalStateException(name + " is null");
        }
    }

    public static void main(String args[]) {
//        0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,  0x0E,	0x0F, //方块3~2
//                0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D,  0x1E,	0x1F, //梅花3~2
//                0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D,  0x2E,	0x2F, //红桃3~2
//                0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D,  0x3E,	0x3F, //黑桃3~2
        ArrayList<Integer> cards = new ArrayList<>(Arrays.asList(0x03, 0x13, 0x23, 0x04, 0x14, 0x24, 0x05));
        TargetCardContainer xx = NJPDKALGContainer.getInstance().getPlaneWithAByCardList(cards, 0, 10, -1);
        System.out.println(xx.planeLength);
        System.out.println(xx.planeCompareValue);
    }
}
