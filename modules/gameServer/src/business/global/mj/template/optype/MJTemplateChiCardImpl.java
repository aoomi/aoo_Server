package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 阳新麻将，中发白
 *
 * @author Huaxing
 */
public class MJTemplateChiCardImpl implements OpCard {
    private List<Integer> fengPai = Arrays.asList(41, 42, 43, 44);
    private List<Integer> jianPai = Arrays.asList(45, 46, 47);

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
        int cardType = cardId / 100;
        MJCardInit mjCardInit = mSetPos.mjCardInit(false);
        if (null == mjCardInit)
            return false;
        List<List<Integer>> chiList = new ArrayList<List<Integer>>();

        Map<Boolean, List<Integer>> partitioned = mjCardInit.getAllCardInts().stream()
                .collect(Collectors.partitioningBy(e -> e >= 40));
        if (null == partitioned) {
            return false;
        }
        List<Integer> allCardList = new ArrayList<>();
        if (cardType < MJSpecialEnum.FENG.value()) {
            allCardList.addAll(partitioned.get(false));
            CommMath.getSort(allCardList, true);
            chiList = chiCard(allCardList, cardType, 0, chiList);
        } else {
            allCardList.addAll(partitioned.get(true));
            CommMath.getSort(allCardList, true);
            chiList = chiDaPaiCard(allCardList, cardType, chiList);
        }

        for (int i = 0; i < chiList.size(); i++) {
            List<Integer> chis = chiList.get(i);
            chis = chiListCarId(chis, mSetPos.getPrivateCard());
            if (chis.size() < 3)
                chis.add(cardId);
            chiSort(chis);
            chiList.set(i, chis);
        }

        if (chiList.size() <= 0)
            return false;
        mSetPos.getPosOpNotice().setChiList(chiList);
        return true;
    }

    /**
     * 获取吃列表ID
     *
     * @param chis
     * @param privateCards
     * @return
     */
    private List<Integer> chiListCarId(List<Integer> chis, List<MJCard> privateCards) {
        List<Integer> chiCardIds = new ArrayList<Integer>();
        List<Integer> chiCardCount = new ArrayList<Integer>();

        for (int i = 0, size = privateCards.size(); i < size; i++) {
            if (chis.contains(privateCards.get(i).type)) {
                int cardType = privateCards.get(i).type;
                if (!chiCardCount.contains(cardType)) {
                    chiCardCount.add(cardType);
                    chiCardIds.add(privateCards.get(i).cardID);
                }

            }
            if (chiCardCount.size() >= 3)
                break;
        }
        return chiCardIds;

    }

    /**
     * 排序
     *
     * @param chis
     */
    private void chiSort(List<Integer> chis) {
        Collections.sort(chis, (o1, o2) -> {
            int oType1 = o1 / 100;
            int oType2 = o2 / 100;
            return oType1 - oType2;
        });
    }

    /**
     * 吃操作
     */
    @Override
    public boolean doOpCard(AbsMJSetPos cSetPos, int cardID) {
        boolean ret = false;
        MJTemplateSetPos mSetPos = (MJTemplateSetPos) cSetPos;
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).ownnerPos;
        List<Integer> publicCard = new ArrayList<>();
        List<Integer> chiTmps = new ArrayList<Integer>();
        publicCard.add(OpType.Chi.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);
        List<Integer> chiCardList = mSetPos.getChiCardList();
        if (chiCardList != null && chiCardList.size() >= 3) {
            chiTmps.addAll(chiCardList);
            int lastOutCardType = lastOutCard / 100;
            for (int i = 0; i < chiTmps.size(); i++) {
                if (lastOutCardType == chiTmps.get(i) / 100)
                    chiTmps.set(i, lastOutCard);
            }
        } else {
            for (int i = 0; i < mSetPos.getPosOpNotice().getChiList().size(); i++) {
                List<Integer> chis = mSetPos.getPosOpNotice().getChiList().get(i);
                for (int j = 0; j < chis.size(); j++) {
                    if (chis.get(0) == cardID) {
                        chiTmps = chis;
                        break;
                    }
                }
            }

            int lastOutCardType = lastOutCard / 100;
            for (int i = 0; i < chiTmps.size(); i++) {
                if (lastOutCardType == chiTmps.get(i) / 100)
                    chiTmps.set(i, lastOutCard);
            }
        }
        // 搜集牌	
        List<MJCard> tmp = new ArrayList<>();
        for (int i = 0; i < mSetPos.getPrivateCard().size(); i++) {
            if (chiTmps.contains(mSetPos.getPrivateCard().get(i).cardID)) {
                if (!tmp.contains(mSetPos.getPrivateCard().get(i)))
                    tmp.add(mSetPos.getPrivateCard().get(i));
                if (tmp.size() >= 2) {
                    ret = true;
                    break;
                }
            }
        }
        if (ret) {
            publicCard.add(chiTmps.get(0));
            publicCard.add(chiTmps.get(1));
            publicCard.add(chiTmps.get(2));
            mSetPos.addPublicCard(publicCard);
            mSetPos.removeAllPrivateCard(tmp);
            mSetPos.getSet().getSetPosMgr().clearChiList();
            mSetPos.getSet().getLastOpInfo()
                    .addLastOpItem(OpType.Chi, new LastOpTypeItem(mSetPos.getPosID(), lastOutCard));
            mSetPos.privateMoveHandCard();
        }
        return ret;
    }

    /**
     * 获取吃牌
     * 万条筒
     *
     * @param privateCards 私有牌
     * @param cardType     牌类型
     * @param idx          位置
     * @param chiList      吃列表
     * @return
     */
    public List<List<Integer>> chiCard(List<Integer> privateCards, int cardType, int idx, List<List<Integer>> chiList) {
        List<Integer> cardInts = new ArrayList<Integer>();
        // 如果 下标 和 手牌长度一致	
        if (idx == privateCards.size())
            return chiList;
        // 从指定的下标开始，遍历出所有手牌	
        for (int i = idx, size = privateCards.size(); i < size; i++) {
            // 如果 手牌中的类型 == 牌的类型	
            if (privateCards.get(i) == cardType || privateCards.get(i) > 39)
                continue;
            // 如果 手牌中类型 不出现重复 并且 记录的牌数 < 2	
            if (!cardInts.contains(privateCards.get(i)) && cardInts.size() < 2) {
                // 添加不重复的牌	
                cardInts.add(privateCards.get(i));
                // 如果 记录牌数 == 2 结束循环	
            } else if (cardInts.size() == 2)
                break;
        }
        idx++;
        // 如果 记录牌数 == 2	
        if (cardInts.size() == 2) {
            // 添加牌	
            cardInts.add(cardType);
            // 判断是否顺子	
            if (CommMath.isContinuous(cardInts)) {
                // 如果是否有重复的顺子	
                if (!chiList.contains(cardInts))
                    chiList.add(cardInts);
                return chiCard(privateCards, cardType, idx, chiList);
            }
        }
        return chiCard(privateCards, cardType, idx, chiList);
    }

    /**
     * 吃箭牌
     *
     * @param privateCards
     * @param cardType
     */
    public List<List<Integer>> chiDaPaiCard(List<Integer> privateCards, int cardType, List<List<Integer>> chiLists) {
        Map<Boolean, List<Integer>> partitioned = privateCards.stream()
                .collect(Collectors.partitioningBy(e -> e < MJSpecialEnum.ZHONG.value()));
        if (null == partitioned)
            return chiLists;
        return addChiList(partitioned, cardType, cardType < MJSpecialEnum.ZHONG.value(), chiLists);
    }

    /**
     * 检查牌是否是顺子要的牌
     *
     * @return
     */
    public boolean checkChiList(List<Integer> privateCards, int curCard, List<Integer> shunzi) {
        if (shunzi.contains(curCard)) {
            return false;
        }
        if (!privateCards.contains(curCard)) {
            return false;
        }
        return true;
    }

    /**
     * 检查牌是否是顺子要的牌
     *
     * @return
     */
    public List<List<Integer>> addChiList(Map<Boolean, List<Integer>> partitioned, int cardType, boolean isFeng, List<List<Integer>> chiLists) {
        //风牌列表	
        List<Integer> cardList = partitioned.get(isFeng).stream().distinct().collect(Collectors.toList());
        List<Integer> curFengPai = isFeng ? fengPai : jianPai;
        for (int feng : curFengPai) {
            List<Integer> list = new ArrayList<>();
            list.add(cardType);
            if (!checkChiList(cardList, feng, list)) {
                continue;
            }
            list.add(feng);
            for (int feng1 : curFengPai) {
                if (checkChiList(cardList, feng1, list)) {
                    List<Integer> shunzi = Arrays.asList(feng, feng1, cardType);
                    Collections.sort(shunzi);
                    if (chiLists.contains(shunzi)) {
                        continue;
                    }
                    chiLists.add(shunzi);
                }
            }
        }

        return chiLists;
    }

}
