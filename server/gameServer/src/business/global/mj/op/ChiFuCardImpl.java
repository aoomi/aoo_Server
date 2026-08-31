package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 不能吃幅打幅（玩家手上只剩4张牌的情况除外，即吃完就剩两张的情况外）；
 *
 * @author Administrator
 */
public class ChiFuCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
        int cardType = cardId / 100;
        MJCardInit mjCardInit = mSetPos.mjCardInit(false);
        mSetPos.getPosOpNotice().clearBuNengChuList();
        if (null == mjCardInit) {
            return false;
        }
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(cardType)) {
            return false;
        }
        if (!checkCardShunZi(mSetPos, cardId)) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
            return false;
        }

        List<List<Integer>> chiList = new ArrayList<List<Integer>>();


        chiList = chiCard(new ArrayList<Integer>(mjCardInit.getAllCardInts()), cardType, 0, chiList);
        for (int i = 0; i < chiList.size(); ) {
            List<Integer> chis = chiList.get(i);
            if (!checkShunZi(mSetPos, chis, cardType)) {
                chiList.remove(i);
                continue;
            }

            chis = chiListCarId(chis, mSetPos.getPrivateCard());
            if (chis.size() < 3) {
                chis.add(cardId);
            }
            chiSort(chis, mSetPos.getSet().getmJinCardInfo().getJin(1).getType());


            chiList.set(i, chis);
            i++;
        }

        if (chiList.size() <= 0) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
            return false;
        }

        if (mSetPos.getPrivateCard().size() <= 4) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
//			return false;
        }

        if (chiList.size() == 1) {
            ArrayList<MJCard> shouCard = new ArrayList<>(mSetPos.getPrivateCard());
//			shouCard.removeAll(chiList.get(0));
            ArrayList<Integer> temp = new ArrayList<>();
            for (MJCard mjCard : shouCard) {
                if (chiList.get(0).contains(mjCard.cardID)) {
                    continue;
                }
                temp.add(mjCard.cardID);
            }
            Map<Integer, List<Integer>> valueMap = temp.stream().filter(p -> !mSetPos.getPosOpNotice().getBuNengChuList().contains(p / 100)).collect(Collectors.groupingBy(p -> p / 100));
            if (valueMap.size() <= 0) {
                mSetPos.getPosOpNotice().clearBuNengChuList();
                return false;
            }
        }

        mSetPos.getPosOpNotice().setChiList(chiList);
        return true;
    }

    /**
     * 判断是否可以组成顺子
     **/
    protected boolean checkShunZi(AbsMJSetPos mSetPos, List<Integer> chis, int cardType) {

        ArrayList<Integer> tempChiList = new ArrayList<>(chis);
        tempChiList.remove(Integer.valueOf(cardType));
        for (Integer integer : mSetPos.getPosOpNotice().getBuNengChuList()) {
            tempChiList.add(integer);
            if (CommMath.isContinuous(tempChiList)) {
                return true;
            } else {
                tempChiList.remove(integer);
            }
        }
        return false;
    }

    private List<Integer> chiListCarId(List<Integer> chis,
                                       List<MJCard> privateCards) {
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
            if (chiCardCount.size() >= 3) {
                break;
            }
        }
        return chiCardIds;

    }

    private void chiSort(List<Integer> chis, int jin) {
        Collections.sort(chis, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int oType1 = o1 / 100;
                int oType2 = o2 / 100;
                return oType1 - oType2;
            }
        });
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        boolean ret = false;
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).ownnerPos;

        List<Integer> publicCard = new ArrayList<>();
        publicCard.add(OpType.Chi.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);
        List<Integer> chiTmps = new ArrayList<Integer>();
        for (int i = 0; i < mSetPos.getPosOpNotice().getChiList().size(); i++) {
            List<Integer> chis = mSetPos.getPosOpNotice().getChiList().get(i);
            for (int j = 0; j < chis.size(); j++) {
                if (chis.get(0) == cardID) {
                    chiTmps = chis;
                    break;
                }
            }
        }

        clacBuNengChu(mSetPos, lastOutCard, cardID);

        int lastOutCardType = lastOutCard / 100;
        for (int i = 0; i < chiTmps.size(); i++) {
            if (lastOutCardType == chiTmps.get(i) / 100) {
                chiTmps.set(i, lastOutCard);
            }
        }

        // 搜集牌
        List<MJCard> tmp = new ArrayList<>();
        for (int i = 0; i < mSetPos.getPrivateCard().size(); i++) {
            if (chiTmps.contains(mSetPos.getPrivateCard().get(i).cardID)) {
                if (!tmp.contains(mSetPos.getPrivateCard().get(i))) {
                    tmp.add(mSetPos.getPrivateCard().get(i));
                }
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
            mSetPos.privateMoveHandCard();
            mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.Chi, new LastOpTypeItem(mSetPos.getPosID(), lastOutCard));
            this.checkOutCard(mSetPos);
        }
        return ret;
    }

    public void checkOutCard(AbsMJSetPos mSetPos) {
        int size = mSetPos.allCards().size();
        if (size >= 5) {
            return;
        }
        if (size <= 2) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
            return;
        }
        MJCardInit mInit = mSetPos.mjCardInit(false);
        if (null == mInit) {
            return;
        }
        Map<Boolean, List<Integer>> map = mInit.getAllCardInts().stream().collect(Collectors.partitioningBy(e -> !mSetPos.getPosOpNotice().getBuNengChuList().contains(e)));
        if (null == map || map.size() <= 0) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
            return;
        }
        if (map.containsKey(Boolean.TRUE) && map.get(Boolean.TRUE).size() <= 0) {
            mSetPos.getPosOpNotice().clearBuNengChuList();
            return;
        }
    }

    /**
     * 获取所有的吃牌
     *
     * @param privateCards 私有牌
     * @param cardType     牌类型
     * @param idx          位置
     * @param chiList      吃列表
     * @return
     */
    public List<List<Integer>> chiCard(List<Integer> privateCards,
                                       int cardType, int idx, List<List<Integer>> chiList) {
        List<Integer> cardInts = new ArrayList<Integer>();
        // 如果 下标 和 手牌长度一致
        if (idx == privateCards.size()) {
            return chiList;
        }
        // 从指定的下标开始，遍历出所有手牌
        for (int i = idx, size = privateCards.size(); i < size; i++) {
            // 如果 手牌中的类型 == 牌的类型
            if (privateCards.get(i) == cardType || privateCards.get(i) > 39) {
                continue;
            }
            // 如果 手牌中类型 不出现重复 并且 记录的牌数 < 2
            if (!cardInts.contains(privateCards.get(i)) && cardInts.size() < 2) {
                // 添加不重复的牌
                cardInts.add(privateCards.get(i));
                // 如果 记录牌数 == 2 结束循环
            } else if (cardInts.size() == 2) {
                break;
            }
        }
        idx++;
        // 如果 记录牌数 == 2
        if (cardInts.size() == 2) {
            // 添加牌
            cardInts.add(cardType);
            // 判断是否顺子
            if (CommMath.isContinuous(cardInts)) {
                // 如果是否有重复的顺子
                if (!chiList.contains(cardInts)) {
                    chiList.add(cardInts);
                }
                return chiCard(privateCards, cardType, idx, chiList);
            }
        }
        return chiCard(privateCards, cardType, idx, chiList);
    }

    /**
     * 超过三手看能否组成顺子
     */
    public boolean checkCardShunZi(AbsMJSetPos mSetPos, int cardId) {

        MJCard mjCard = new MJCard(cardId);

        //只能吃万条筒
        if (mjCard.type < 10 || mjCard.type > 39) {
            return false;
        }


        int typeAdd3 = (mjCard.type) % 10 > 6 ? mjCard.type : mjCard.type + 3;
        int typeSub3 = (mjCard.type) % 10 < 4 ? mjCard.type : mjCard.type - 3;
        int typeAdd2 = (mjCard.type) % 10 > 7 ? mjCard.type : mjCard.type + 2;
        int typeSub2 = (mjCard.type) % 10 < 3 ? mjCard.type : mjCard.type - 2;
        int typeAdd1 = (mjCard.type) % 10 > 8 ? mjCard.type : mjCard.type + 1;
        int typeSub1 = (mjCard.type) % 10 < 2 ? mjCard.type : mjCard.type - 1;
//		if (!checkHaveCard(mSetPos, typeAdd3 ) && !checkHaveCard(mSetPos, typeSub3 ) && !checkHaveCard(mSetPos, mjCard.type) ) {
//			return false;
//		}

        boolean falg = false;
        if (checkHaveCard(mSetPos, typeAdd1) && checkHaveCard(mSetPos, typeAdd2)) {
            mSetPos.getPosOpNotice().addBuNengChuList(mjCard.type);
            mSetPos.getPosOpNotice().addBuNengChuList(typeAdd3);
            falg = true;
        }

        if (checkHaveCard(mSetPos, typeSub1) && checkHaveCard(mSetPos, typeSub2)) {
            mSetPos.getPosOpNotice().addBuNengChuList(mjCard.type);
            mSetPos.getPosOpNotice().addBuNengChuList(typeSub3);
            falg = true;
        }

        if (checkHaveCard(mSetPos, typeAdd1) && checkHaveCard(mSetPos, typeSub1)) {
            mSetPos.getPosOpNotice().addBuNengChuList(mjCard.type);
            falg = true;
        }
        return falg;
    }


    /**
     * 判断是否有摸张牌
     */
    public boolean checkHaveCard(AbsMJSetPos mSetPos, int type) {
        for (MJCard mjCard : mSetPos.getPrivateCard()) {
            if (mjCard.type == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重新计算不能出的牌
     */
    public void clacBuNengChu(AbsMJSetPos mSetPos, int lastCard, int cardID) {
        mSetPos.getPosOpNotice().clearBuNengChuList();

        if (lastCard / 100 == cardID / 100) {
            if (checkHaveCard(mSetPos, lastCard / 100)) {
                mSetPos.getPosOpNotice().addBuNengChuList(lastCard / 100);
            }
            int type = (lastCard / 100 + 3) % 10 > 9 ? lastCard / 100 : lastCard / 100 + 3;
            if (checkHaveCard(mSetPos, type)) {
                mSetPos.getPosOpNotice().addBuNengChuList(type);
            }
        } else if (lastCard / 100 == cardID / 100 + 1) {
            if (checkHaveCard(mSetPos, lastCard / 100)) {
                mSetPos.getPosOpNotice().addBuNengChuList(lastCard / 100);
            }
        } else if (lastCard / 100 == cardID / 100 + 2) {
            if (checkHaveCard(mSetPos, lastCard / 100)) {
                mSetPos.getPosOpNotice().addBuNengChuList(lastCard / 100);
            }
            int type = (lastCard / 100 - 3) % 10 > 9 ? lastCard / 100 : lastCard / 100 - 3;
            if (checkHaveCard(mSetPos, type)) {
                mSetPos.getPosOpNotice().addBuNengChuList(type);
            }
        }
    }

}
