package business.global.mj.extbussiness.optype;

import business.global.mj.MJCard;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.extbussiness.StandardMJSetPos;

import java.util.*;
import java.util.stream.Collectors;

public class StandardMJUtil {

    private static class SingleCase {
        public static final StandardMJUtil INSTANCE = new StandardMJUtil();

        private SingleCase() {
        }
    }

    public static StandardMJUtil get() {
        return StandardMJUtil.SingleCase.INSTANCE;
    }


    //换三张的逻辑，模板麻将过来的
    public class ChangeCardData {
        public List<Integer> cards = new ArrayList<>();
        public int maxSameCardNum;//相同牌最多的
        public boolean containsShunZi;//牌是否包含顺子
    }

    /**
     * 默认选出要换的几张牌
     */
    public void initSanZhang(StandardMJRoomSet set) {
        if (((StandardMJRoom)set.getRoom()).getHuanZhangNum()<=0) {
            return ;
        }
        // 玩家初始发牌
        set.getPosDict().values().forEach(k -> initFirstChangeCard((StandardMJSetPos) k));
    }

    /**
     * 找出系统默认要换的牌，机器人托管要换的牌
     */
    public void initFirstChangeCard(StandardMJSetPos setPos) {
        StandardMJRoomSet set = (StandardMJRoomSet)setPos.getSet();
        StandardMJRoom room = (StandardMJRoom)setPos.getRoom();
        List<Integer> jins = new ArrayList<>();
        List<Integer> allCardIDs = setPos.allCards().stream().map(MJCard::getCardID).collect(Collectors.toList());
        if (set.getmJinCardInfo().sizeJin() > 0) {
            jins = allCardIDs.stream().filter(k -> set.getmJinCardInfo().checkJinExist(k)).collect(Collectors.toList());
        }
        allCardIDs.removeAll(jins);
        List<Integer> changCardList;
        if (!room.isHuanTongSe()) {
            changCardList = findChangeCardDifferentColor(allCardIDs,setPos).cards;
        } else {
            changCardList = findChangeCardSameColor(allCardIDs,setPos);
        }
        int changeCardNum = room.getHuanZhangNum();
        if (changCardList.size() < changeCardNum) {
            changCardList.addAll(jins.subList(0, changeCardNum - changCardList.size()));
        }
        setPos.setFirstChangeCardList(changCardList);
    }

    /**
     * 换张（不同色）：可以是不同花色的三张牌，也可以是同花色的三张牌
     *
     * @param value
     */
    protected ChangeCardData findChangeCardDifferentColor(List<Integer> value, StandardMJSetPos setPos) {
        ChangeCardData data = new ChangeCardData();
        int changeCardNum = ((StandardMJRoom)setPos.getRoom()).getHuanZhangNum();
        if (value.size() < changeCardNum) {
            return data;
        }
        Map<Integer, List<Integer>> collect = value.stream().collect(Collectors.groupingBy(k -> k / 100));
        Set<Integer> allShunZiTypes = findAllShunZiTypes(setPos);
        for (int i = 1; i <= 4; i++) {
            int finalI = i;
            data.maxSameCardNum = finalI;
            List<Integer> curList = new ArrayList<>(changeCardNum);
            //系统需自动弹起三张：优先弹起相同张数少的牌；
            collect.values().stream().forEach(k -> {
                if (k.size() == finalI) {
                    curList.addAll(k);
                }
            });
            if (curList.size() + data.cards.size() == changeCardNum) {
                data.cards.addAll(curList);
                return data;
            } else if (curList.size() + data.cards.size() > changeCardNum) {
                //相同张数少的有多张，优先弹起没办法组成顺子的牌；
                List<Integer> removeList = curList.stream().filter(k -> allShunZiTypes.contains(k / 100)).collect(Collectors.toList());
                curList.removeAll(removeList);
                //如果还是多张，从中随机弹起三张；
                if (curList.size() + data.cards.size() >= changeCardNum) {
                    data.cards.addAll(curList.subList(0, changeCardNum - data.cards.size()));
                    return data;
                } else {
                    data.cards.addAll(curList);
                    data.cards.addAll(removeList.subList(0, changeCardNum - data.cards.size()));
                    data.containsShunZi = true;
                    return data;
                }
            } else {
                data.cards.addAll(curList);
            }

        }
        data.cards.addAll(value.subList(0, changeCardNum));
        return data;
    }

    /**
     * 换张（同色）：只能是同花色的几张；
     *
     * @param checkList
     */
    protected List<Integer> findChangeCardSameColor(List<Integer> checkList, StandardMJSetPos setPos) {
        StandardMJRoom room = (StandardMJRoom)setPos.getRoom();
        int changeCardNum = room.getHuanZhangNum();
        if (checkList.size() < changeCardNum) {
            return checkList;
        }

        Map<Integer, List<Integer>> tongSeMap = checkList.stream().collect(Collectors.groupingBy(k -> k / 1000));
        List<ChangeCardData> dataList = new ArrayList<>();
        tongSeMap.values().forEach(k -> {
            ChangeCardData data = findChangeCardDifferentColor(k,setPos);
            if (data.cards.size() == changeCardNum) {
                dataList.add(data);
            }
        });
        if (dataList.size() == 1) {
            return dataList.get(0).cards;
        } else if (dataList.size() > 1) {
            int minNum = dataList.stream().mapToInt(k -> k.maxSameCardNum).min().getAsInt();
            List<ChangeCardData> collect = dataList.stream().filter(k -> k.maxSameCardNum == minNum).collect(Collectors.toList());
            //牌数一样
            if (collect.size() == 1) {
                return collect.get(0).cards;
            }
            List<ChangeCardData> collect1 = dataList.stream().filter(k -> k.containsShunZi == false).collect(Collectors.toList());
            //不拆顺子
            if (collect1.size() == 1) {
                return collect1.get(0).cards;
            }
            return dataList.get(0).cards;
        }
        return checkList.subList(0, changeCardNum);
    }

    /**
     * 找出所有的顺子 对应的cardType
     * @param setPos
     */
    protected Set<Integer> findAllShunZiTypes(StandardMJSetPos setPos) {
        Set<Integer> typeSet = new HashSet<>();
        List<Integer> cardList = setPos.getPrivateCard().stream().map(card -> card.type).collect(Collectors.toList());

        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 7; j++) {
                if (cardList.contains(i * 10 + j) && cardList.contains(i * 10 + j + 1) && cardList.contains(i * 10 + j + 2)) {
                    typeSet.add(i * 10 + j);
                    typeSet.add(i * 10 + j + 1);
                    typeSet.add(i * 10 + j + 2);
                }
            }
        }
        return typeSet;
    }
}
