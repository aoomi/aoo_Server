package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.HuCard;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胡
 *
 * @author Huaxing
 */
public abstract class BaseHuCard implements HuCard {

    /**
     * @param allCardInts
     * @param jins
     * @param jinNum
     * @return
     */
    public MJCardInit mCaiGui(List<Integer> allCardInts, List<Integer> jins, int jinNum) {
        MJCardInit mCardInit = new MJCardInit();
        mCardInit.addAllCardInts(allCardInts);
        mCardInit.addAllJins(jins);
        // 检查手上是否有金
        if (mCardInit.sizeJin() == 0) {
            return null;
        }
        // 检查金的数量 大于等于 财归数量。
        if (mCardInit.sizeJin() >= jinNum) {
            for (int i = 0; i < jinNum; i++) {
                if (mCardInit.sizeJin() > 0) {
                    mCardInit.addCardInts(mCardInit.getJins().remove(0));
                }
            }
            return mCardInit;
        }
        return null;
    }

    /**
     * 替金牌
     *
     * @param allCardInts
     * @param jins
     * @param jinNum
     * @return
     */
    public MJCardInit mTiJin(List<Integer> allCardInts, List<Integer> jins, int jinNum) {
        MJCardInit mCardInit = new MJCardInit();
        mCardInit.addAllCardInts(allCardInts);
        mCardInit.addAllJins(jins);
        // 检查手上是否有金
        if (mCardInit.sizeJin() == 0) {
            return null;
        }
        // 检查金的数量 大于等于 替金数量。
        if (mCardInit.sizeJin() >= jinNum) {
            for (int i = 0; i < jinNum; i++) {
                if (mCardInit.sizeJin() > 0) {
                    mCardInit.getJins().remove(0);
                    mCardInit.addCardInts(MJSpecialEnum.NOT_CARD.value());
                }
            }
            return mCardInit;
        }
        return null;
    }

    /**
     * 4相同类型的牌
     *
     * @param allCards
     * @return
     */
    public int fourSameType(List<Integer> allCards) {
        int sameInt = 0;
        // 检查数据是否存在 或 小于等于 0
        if (null == allCards || allCards.size() <= 0) {
            return 0;
        }
        // 分组牌列表
        Map<Integer, Long> map = allCards.stream().collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map) {
            return 0;
        }
        // 遍历检查列表
        for (Long four : map.values()) {
            // 检查是否有4个相同类型的牌
            if (four == 4) {
                sameInt++;
            }
        }
        return sameInt;
    }

    /**
     * 检查是否有吃
     *
     * @param mSetPos
     */
    public boolean checkChi(AbsMJSetPos mSetPos) {
        List<List<Integer>> publicCardList = new ArrayList<List<Integer>>(mSetPos.getPublicCardList());
        for (List<Integer> publicCards : publicCardList) {
            int type = publicCards.get(0);
            if (type == OpType.Chi.value()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取打出公共牌列表
     *
     * @param mSetPos
     * @return
     */
    public List<Integer> publicCardList(AbsMJSetPos mSetPos) {
        // 已经亮出的牌
        List<List<Integer>> publicCardList = new ArrayList<List<Integer>>();
        publicCardList.addAll(mSetPos.getPublicCardList());
        // 获取公共牌的类型
        List<Integer> list = new ArrayList<Integer>();
        for (List<Integer> publicCards : publicCardList) {
            int cardType = publicCards.get(2) / 100;
            if (cardType <= 0) {
                continue;
            }
            // 牌类型
            list.add(cardType);
        }
        return list;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType) {
        return false;
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType) {
        return null;
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return null;
    }

    @Override
    public <T> boolean checkHuCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType, Object opHu) {
        return false;
    }

    @Override
    public <T> boolean checkHuCard(AbsMJSetPos mSetPos, int cardType, Object opHu) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, int jin) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos aSetPos) {
        return false;
    }

    @Override
    public boolean tingYouJin(AbsMJSetPos setPos) {
        return false;
    }

    @Override
    public List<MJCard> qiangJinHuCard(AbsMJSetPos setPos) {
        return null;
    }

    @Override
    public boolean doQiangJin(AbsMJSetPos mSetPos, List<MJCard> qiangJinList) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return false;
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType) {
        return null;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos hbSetPos, List<Integer> allCardList, String[] cardTypes, List<Integer> cardList, int lastOutCard) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, Object opHu) {
        return false;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType, Object opHu, Integer posID) {
        return false;
    }
}
