package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 吃牌动作，包含白板替金
 *
 * @author Administrator
 */
public class ChiCardBanJinImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
        int cardType = cardId / 100;
        if (!mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            return false;
        }
        MJCardInit mjCardInit = mSetPos.mjCardInit(true);
        if (null == mjCardInit) {
            return false;
        }
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(cardType)) {
            return false;
        }
        List<List<Integer>> chiList = new ArrayList<List<Integer>>();
        List<Integer> baiBanList = checkDanBaiBan(mjCardInit.getAllCardInts(), mSetPos.getSet().getmJinCardInfo().getJinKeys());

        if (null == baiBanList) {
            baiBanList = mjCardInit.getAllCardInts();
        }
        int jinType = mSetPos.getSet().getmJinCardInfo().getJin(0).getType();
        if (!mSetPos.getSet().getmJinCardInfo().checkJinExist(MJSpecialEnum.BAIBAN.value()) && cardType == MJSpecialEnum.BAIBAN.value()) {
            cardType = jinType;
        }
        chiList = chiCard(baiBanList, cardType, 0, chiList);
        // 对结果进行排序

        for (int i = 0; i < chiList.size(); i++) {
            List<Integer> chis = chiList.get(i);

            for (int j = 0; j < chis.size(); j++) {
                if (chis.get(j) == jinType) {
                    chis.set(j, MJSpecialEnum.BAIBAN.value());
                }
            }
            chis = chiListCarId(chis, mSetPos.getPrivateCard());
            if (chis.size() < 3) {
                chis.add(cardId);
            }
            chiSort(chis, jinType);
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
                if (oType1 == MJSpecialEnum.BAIBAN.value()) {
                    oType1 = jin;
                }
                if (oType2 == MJSpecialEnum.BAIBAN.value()) {
                    oType2 = jin;
                }
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
     * 检查白板
     *
     * @return
     */
    public List<Integer> checkDanBaiBan(List<Integer> allCardInts,
                                        List<Integer> Jins) {
        if (Jins.size() <= 0) {
            return null;
        }
        // 判断金的大小，是否属于1\2\3中
        List<Integer> baiBans = new ArrayList<Integer>();
        List<Integer> baList = new ArrayList<Integer>();
        baList.addAll(allCardInts);
        // 如果金大于40就没必要进行接下去的判断了。
        if (Jins.get(0) > 40) {
            return null;
        }
        // 检查是否有白板
        for (int i = 0; i < baList.size(); i++) {
            if (baList.get(i) == MJSpecialEnum.BAIBAN.value()) {
                baiBans.add(baList.get(i));
            }
        }
        // 检查白板的数量
        if (baiBans.size() <= 0) {
            return null;
        }
        // 去除所有的白板
        baList.removeAll(baiBans);
        // 将数组中的白板去掉，换成金的数字
        for (int i = 0; i < baiBans.size(); i++) {
            baList.add(Jins.get(0));
        }
        // 对结果进行排序
        Collections.sort(baList, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return baList;
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

}
