package business.global.mj.ting;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.TingCard;
import business.global.mj.util.HuUtil;
import cenum.mj.MJSpecialEnum;

import java.util.ArrayList;
import java.util.List;

public abstract class AbsTing implements TingCard {
    /**
     * 听所有胡
     *
     * @param mSetPos   用户
     * @param mCardInit 所有牌
     * @return
     */
    public abstract boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit);

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public List<Integer> absCheckTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        List<Integer> ret = new ArrayList<>();
        // 麻将牌的初始信息
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        boolean isHu = true;
        // 添加一张任意牌，进行测试是否能胡
        isHu = tingHu(mSetPos,
                this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value()));
        if (!isHu) {
            // 任意牌都不能出，其他牌相应的也不能胡。
            return ret;
        }
        // 检查是否有金牌
        if (mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            // 添加金牌列表
            ret.addAll(mSetPos.getSet().getmJinCardInfo().getJinKeys());
        }

        // 遍历其他牌
        for (int type : HuUtil.CheckTypes) {
            isHu = tingHu(mSetPos, this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), type));
            if (isHu) {
                if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
                    continue;
                } else {
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }

            }

        }
        mInit = null;
        allCardList = null;
        return ret;
    }

    public MJCardInit newMJCardInit(List<Integer> allCardInts, List<Integer> jins, int cardType) {
        MJCardInit mInit = new MJCardInit();
        mInit.addAllCardInts(allCardInts);
        mInit.addAllJins(jins);
        if (MJSpecialEnum.NOT_JIN.value() == cardType) {
            mInit.addJins(cardType);
        } else {
            mInit.addCardInts(cardType);
        }
        return mInit;

    }

    /**
     * 检查听到的“金”牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public List<Integer> absTingJinCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        List<Integer> tings = absCheckTingCard(mSetPos, allCardList);
        if (tings.size() == mSetPos.getSet().getmJinCardInfo().getJinKeys().size()) {
            if (mSetPos.getSet().getmJinCardInfo().getJinKeys().containsAll(tings)) {
                tings.clear();
                return tings;
            }
        }
        return tings;
    }

    /**
     * 检查是否有听到牌
     *
     * @param mSetPos
     * @return
     */
    public boolean absCheckTingList(AbsMJSetPos mSetPos) {
        mSetPos.getPosOpNotice().clearTingCardMap();
        List<Integer> tings = new ArrayList<Integer>();
        // 听列表
        tings = tingList(mSetPos, tings, 0);
        if (tings.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * 听列表
     *
     * @param lists 列表
     * @param idx   下标
     * @return
     */
    public List<Integer> tingList(AbsMJSetPos mSetPos, List<Integer> lists, int idx) {
        // 获取所有牌
        List<MJCard> allCards = mSetPos.allCards();
        // 如果牌的下标 == 所有牌 -1
        if (allCards.size() == idx) {
            return lists;
        }
        // 获取牌ID
        int cardId = allCards.get(idx).cardID;
        // 移除一张牌
        allCards.remove(idx);
        // 听牌
        List<Integer> tingList = absCheckTingCard(mSetPos, allCards);
        idx++;
        // 判断听牌数
        if (tingList.size() > 0) {
            mSetPos.getPosOpNotice().addTingCardList(cardId / 100, tingList);
            lists.add(cardId);
            return tingList(mSetPos, lists, idx);
        }
        return tingList(mSetPos, lists, idx);
    }

    /**
     * 听列表
     *
     * @param lists 列表
     * @param idx   下标
     * @return
     */
    public List<Integer> tingList(AbsMJSetPos mSetPos, List<Integer> lists) {
        // 获取所有牌
        List<MJCard> filterCardList = new ArrayList<>();
        mSetPos.allCards().forEach(n -> {
            if (filterCardList.stream().allMatch(m -> m.getType() != n.getType())) {
                filterCardList.add(n);
            }
        });
        filterCardList.parallelStream().forEach(n -> {
            List<MJCard> allCards = mSetPos.allCards();
            allCards.removeIf(m -> m.getCardID().intValue() == n.getCardID());
            List<Integer> tingList = absCheckTingCard(mSetPos, allCards);
            // 判断听牌数
            if (tingList.size() > 0) {
                mSetPos.getPosOpNotice().addTingCardList(n.getType(), tingList);
                lists.add(n.getCardID());
            }
        });
        return lists;
    }

    /**
     * 遍历检查是否存在胡牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public boolean checkErgodicExistHuCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        // 麻将牌的初始信息
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return false;
        }
        // 添加一张任意牌，进行测试是否能胡
        return tingHu(mSetPos,
                this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value()));
    }


    /**
     * 枪金的听牌列表
     *
     * @param mSetPos 列表
     * @param idx     下标
     * @param isDPos
     * @return
     */
    @Override
    public List<MJCard> qangJinTingList(AbsMJSetPos mSetPos, int idx, boolean isDPos) {
        // 获取所有牌
        List<MJCard> allCards = mSetPos.allCards();
        // 如果牌的下标 == 所有牌 -1
        if (allCards.size() == idx) {
            return null;
        }
        // 庄家多一张牌，所有需移除一张做检查
        if (isDPos) {
            // 移除一张牌
            allCards.remove(idx);
        }
        // 听牌
        boolean checkErgodicExistHuCard = checkErgodicExistHuCard(mSetPos, allCards);
        idx++;
        // 判断听牌数
        if (checkErgodicExistHuCard) {
            return allCards;
        } else {
            if (isDPos) {
                // 庄家可以尝试将所有牌都打出一遍，试试有没有听牌
                return qangJinTingList(mSetPos, idx, isDPos);
            } else {
                // 闲家只能检查一次。
                return null;
            }
        }
    }

    @Override
    public List<Integer> checkTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        return absCheckTingCard(mSetPos, allCardList);
    }

    @Override
    public List<Integer> tingJinCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        return absTingJinCard(mSetPos, allCardList);
    }

    @Override
    public boolean checkTingList(AbsMJSetPos mSetPos) {
        return absCheckTingList(mSetPos);
    }

}
