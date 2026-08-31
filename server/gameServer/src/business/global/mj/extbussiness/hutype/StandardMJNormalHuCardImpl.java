package business.global.mj.extbussiness.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.dto.StandardMJCardTypeItem;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.hu.BaseHuCard;
import com.ddm.server.common.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 麻将胡牌
 */
public class StandardMJNormalHuCardImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (((StandardMJRoom) mSetPos.getRoom()).isHuDaZi()) {
            return StandardMJHuZiUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
        } else {
            return StandardMJHuUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin());
        }
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return getPossibilityList(mCardInit, mSetPos);
    }

    /**
     * 获取胡牌的可能性
     *
     * @param mCardInit 米卡初始化
     * @param pxPos     px pos
     * @return {@link List<List<List<Integer>>>}
     */
    public StandardMJPointItem<StandardMJCardTypeItem> getPossibilityList(MJCardInit mCardInit, AbsMJSetPos pxPos) {
        if (null == mCardInit) {
            return null;
        }
        List<String> huTypeList1 = null;
        if (((StandardMJRoom) pxPos.getRoom()).isHuDaZi()) {
            huTypeList1 = StandardMJHuZiUtil.getInstance().findHuTypeList1(mCardInit.getAllCardInts(), mCardInit.sizeJin(), needJinHuanYuan());
        } else {
            huTypeList1 = StandardMJHuUtil.getInstance().findHuTypeList1(mCardInit.getAllCardInts(), mCardInit.sizeJin(), needJinHuanYuan());
        }
        if (CollectionUtils.isNotEmpty(huTypeList1)) {
            StandardMJPointItem<StandardMJCardTypeItem> item = new StandardMJPointItem<>();
            for (String type : huTypeList1) {
                String[] cardTypes = type.split(":");
                item.addCardTypeMap(type, new StandardMJCardTypeItem<>(getCardList(cardTypes)));
            }
//            item.addOpPointEnum(OpPoint.PingHu.name(), OpPoint.PingHu.value());
            return item;
        }
        return null;
    }

    /**
     * 获取牌型
     *
     * @param cardTypes 卡片类型
     */
    private static List<List<Integer>> getCardList(String[] cardTypes) {
        List<List<Integer>> cardList = new ArrayList<>();
        Arrays.stream(cardTypes).forEach(n -> {
            String[] cards = n.split(",");
            List<Integer> itemCardList = new ArrayList<>();
            if (cards.length == 2) {
                int card1 = Integer.parseInt(cards[0].trim());
                int card2 = Integer.parseInt(cards[1].trim());
                itemCardList.add(card1);
                itemCardList.add(card2);
                Optional<Integer> num = itemCardList.stream().filter(i -> i != 0).findFirst();
                num.ifPresent(u -> {
                    itemCardList.set(0, u);
                    itemCardList.set(1, u);
                });
            } else {
                if (cards.length == 3) {
                    int card1 = Integer.parseInt(cards[0].trim());
                    int card2 = Integer.parseInt(cards[1].trim());
                    int card3 = Integer.parseInt(cards[2].trim());
                    itemCardList.add(card1);
                    itemCardList.add(card2);
                    itemCardList.add(card3);
                    Optional<Integer> num = itemCardList.stream().filter(i -> i != 0).findFirst();
                    List<Integer> checkCardList = itemCardList.stream().filter(i -> i != 0).collect(Collectors.toList());
                    long zeroNum = itemCardList.stream().filter(i -> i == 0).count();
                    if (zeroNum == 1 && num.isPresent() && checkCardList.get(0).intValue() != checkCardList.get(1)) {
                        int present = num.orElseThrow();
                        int index = itemCardList.indexOf(present);
                        if (index == 0) {
                            itemCardList.set(1, present + 1);
                            itemCardList.set(2, present + 2);
                        } else if (index == 1) {
                            itemCardList.set(0, present - 1);
                            itemCardList.set(2, present + 1);
                        } else if (index == 2) {
                            itemCardList.set(0, present - 2);
                            itemCardList.set(1, present - 1);
                        }
                    }
                }
            }
            cardList.add(itemCardList);
        });
        return cardList;
    }


    /**
     * 是否需要把金牌（也就是0）还原出来成具体替代值
     *
     * @return
     */
    public boolean needJinHuanYuan() {
        return true;
    }

}
