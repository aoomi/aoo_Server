package business.global.mj.extbussiness.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomEnum;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.extbussiness.StandardMJSetPos;
import business.global.mj.manage.OpCard;
import cenum.mj.OpType;
import jsproto.c2s.iclass.mj._Promptly;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板麻将暗杠
 */
public class StandardMJAnGangCardImpl implements OpCard {
    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int specialCard) {
        StandardMJSetPos jPos = (StandardMJSetPos) mSetPos;
        jPos.clearGangList();
        StandardMJRoomSet set = (StandardMJRoomSet) mSetPos.getSet();
        int sizeCard = set.getSetCard().getRandomCard().getSize();
        if (sizeCard <= set.getLeftSizeByOpType(OpType.AnGang)) {
            return false;
        }
        //扣牌后不能暗杠
        return this.baoTingAnGang(jPos);
    }

    /**
     * 报听杠检查
     *
     * @param mSetPos
     * @return
     */
    public boolean baoTingAnGang(StandardMJSetPos mSetPos) {
        MJCardInit mInit = mSetPos.mjCardInit(false);
        if (Objects.isNull(mInit)) {
            return false;
        }
        Map<Integer, Long> map = mSetPos.allCards().stream()
                // 筛选出所有的牌类型
                .map(MJCard::getType)
                // 按牌类型分组
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map || map.size() <= 0) {
            return false;
        }

        int fourSize = 4;
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue().intValue() >= fourSize) {
                if (mSetPos.isTing()) {
                    // 检查报听-杠
                    if (mSetPos.checkBaotingGang(entry.getKey(),OpType.AnGang)) {
                        // 添加可以杠的Key
                        mSetPos.getAnGangList().add(newGangListCarId(new ArrayList<>(Collections.nCopies(fourSize, entry.getKey())), mSetPos.allCards()));
                    }
                } else {
                    //能杠
                    mSetPos.getAnGangList().add(newGangListCarId(new ArrayList<>(Collections.nCopies(fourSize, entry.getKey())), mSetPos.allCards()));
                }
            }
        }
        return CollectionUtils.isNotEmpty(mSetPos.getAnGangList());
    }

    /**
     * 获取吃列表ID
     *
     * @param chis
     * @param privateCards
     * @return
     */
    public List<Integer> newGangListCarId(List<Integer> chis,
                                           List<MJCard> privateCards) {
        for (int i = 0, size = privateCards.size(); i < size; i++) {
            int index = chis.indexOf(privateCards.get(i).type);
            if (index >= 0) {
                chis.set(index, privateCards.get(i).cardID);
            }
        }
        return chis;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos cSetPos, int cardID) {
        StandardMJSetPos mSetPos = (StandardMJSetPos) cSetPos;
        List<Integer> chiCardList = mSetPos.getChiCardList();
        List<MJCard> liangCardList = mSetPos.allCards().stream().filter(i -> chiCardList.contains(i.getCardID())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(liangCardList) || liangCardList.size() != chiCardList.size()) {
            return false;
        }
        List<Integer> publicCard = new ArrayList<>();
        publicCard.add(OpType.AnGang.value());
        publicCard.add(mSetPos.getPosID());
        publicCard.add(cardID);
        publicCard.addAll(chiCardList);
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCard(liangCardList);
        if (mSetPos.getHandCard()!=null&&liangCardList.stream().noneMatch(y->y.cardID==mSetPos.getHandCard().cardID)) {
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
        }
        mSetPos.cleanHandCard();
        mSetPos.getSet().getSetPosMgr().clearChiList();
        if(((StandardMJRoom)mSetPos.getRoom()).isSSSG()){
            mSetPos.resolveSSSGAnGang();
        }
        return true;
    }

}
