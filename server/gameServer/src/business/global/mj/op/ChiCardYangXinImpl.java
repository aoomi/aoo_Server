package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 阳新麻将，中发白
 *
 * @author Huaxing
 */
public class ChiCardYangXinImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
        int cardType = cardId / 100;
        MJCardInit mjCardInit = mSetPos.mjCardInit(true);
        if (null == mjCardInit) {
            return false;
        }
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(cardType)) {
            return false;
        }
        List<List<Integer>> chiList = new ArrayList<List<Integer>>();


        chiList = chiCard(new ArrayList<Integer>(mjCardInit.getAllCardInts()), cardType, 0, chiList);
        for (int i = 0; i < chiList.size(); i++) {
            List<Integer> chis = chiList.get(i);
            chis = chiListCarId(chis, mSetPos.getPrivateCard());
            if (chis.size() < 3) {
                chis.add(cardId);
            }
            chiSort(chis, mSetPos.getSet().getmJinCardInfo().getJin(0).getType());
            chiList.set(i, chis);
        }

        if (chiList.size() <= 0) {
            return false;
        }
        mSetPos.getPosOpNotice().setChiList(chiList);
        return true;
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
        }
        return ret;
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
            if (privateCards.get(i) < 45) {
                continue;
            }
            // 如果 手牌中类型 不出现重复 并且 记录的牌数 < 2
            if (!cardInts.contains(privateCards.get(i)) && cardInts.size() < 3) {
                // 添加不重复的牌
                cardInts.add(privateCards.get(i));
                // 如果 记录牌数 == 2 结束循环
            } else if (cardInts.size() == 3) {
                break;
            }
        }
        idx++;
        // 如果 记录牌数 == 2
        if (cardInts.size() == 3) {
            // 添加牌
            // 判断是否顺子
            if (CommMath.isContinuous(cardInts)) {
                // 如果是否有重复的顺子
//				if (!chiList.contains(cardInts))
                chiList.add(cardInts);
//				removeCard(privateCards,cardInts);
                return chiCard(privateCards, cardType, idx, chiList);
            }
        }
        return chiCard(privateCards, cardType, idx, chiList);
    }


    public int removeCard(List<Integer> privateCards, int idx) {

        List<Integer> cc = new ArrayList<Integer>();
        cc.add(45);
        cc.add(46);
        cc.add(47);
        if (privateCards.containsAll(cc)) {
            for (int i = 0; i < cc.size(); i++) {
                for (int j = 0; j < privateCards.size(); j++) {
                    if (privateCards.get(j).equals(cc.get(i))) {
                        privateCards.remove(j);
                        break;
                    }
                }
            }
            idx++;
        } else {
            return idx;
        }
        return removeCard(privateCards, idx);
    }


}
