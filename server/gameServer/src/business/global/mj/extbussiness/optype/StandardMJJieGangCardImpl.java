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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模板麻将接杠
 */
public class StandardMJJieGangCardImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        StandardMJSetPos jPos = (StandardMJSetPos) mSetPos;
        jPos.clearJieGangList();
        StandardMJRoomSet set = (StandardMJRoomSet)mSetPos.getSet();
        int sizeCard = set.getSetCard().getRandomCard().getSize();
        if (sizeCard <= set.getLeftSizeByOpType(OpType.JieGang)) {
            return false;
        }
        return this.jieGang(jPos, cardID);
    }


    public boolean jieGang(StandardMJSetPos mSetPos, int cardID) {
        MJCardInit mInit = mSetPos.mjCardInit(false);
        if (null == mInit) {
            return false;
        }
        // 获取牌的类型
        int type = cardID / 100;
        Map<Integer, Long> map = mInit.getAllCardInts().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map) {
            return false;
        }

        if (map.containsKey(type)) {
            Long aLong = map.get(type);
            int fourSize = 3;
            if (aLong >= fourSize) {
                if (mSetPos.isTing()) {
                    if (mSetPos.checkBaotingGang(cardID / 100,OpType.JieGang)) {
                        List<Integer> list = newGangListCarId(new ArrayList<>(Collections.nCopies(fourSize, type)), mSetPos.allCards());
                        list.add(cardID);
                        mSetPos.getJieGangList().add(list);
                        return true;
                    }
                } else {
                    //能接杠
                    List<Integer> list = newGangListCarId(new ArrayList<>(Collections.nCopies(fourSize, type)), mSetPos.allCards());
                    list.add(cardID);
                    mSetPos.getJieGangList().add(list);
                    return true;
                }
            }
        }
        return false;
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
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).ownnerPos;

        List<Integer> publicCard = new ArrayList<>();
        publicCard.add(OpType.JieGang.value());
        publicCard.add(fromPos);
        publicCard.add(lastOutCard);

        List<Integer> jieGangCardList = ((StandardMJSetPos)mSetPos).getChiCardList();
        if (jieGangCardList == null || jieGangCardList.size() < 3) {
            return false;
        }

        List<Integer> jieGangList = jieGangCardList.stream().filter(y -> y != lastOutCard).collect(Collectors.toList());
        publicCard.add(jieGangList.get(0));
        publicCard.add(lastOutCard);
        publicCard.add(jieGangList.get(1));
        if(jieGangCardList.size()==4){
            publicCard.add(jieGangList.get(2));
        }
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(jieGangCardList);
        if(mSetPos.getHandCard()!=null){
            if (!jieGangCardList.contains(mSetPos.getHandCard().getCardID())) {
                mSetPos.addPrivateCard(mSetPos.getHandCard());
                mSetPos.sortCards();
                mSetPos.cleanHandCard();
            }else{
                mSetPos.cleanHandCard();
            }
        }
        if(((StandardMJRoom)mSetPos.getRoom()).isSSSG()){
            ((StandardMJSetPos)mSetPos).resolveSSSGJieGang(publicCard.get(1));
        }
        return true;
    }

}
