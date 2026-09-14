package business.global.mj.extbussiness.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.MJSetPos;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.extbussiness.StandardMJRoomEnum;
import business.global.mj.manage.OpCard;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 麻将开金
 *
 * @author Huaxing
 */
@SuppressWarnings("Duplicates")
public class StandardMJKaiJinImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        return false;
    }


    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardType) {
        // 开金
        boolean result = this.kaiJin(mSetPos.getSet());
        if (result) {
            mSetPos.getSet().getPosDict().values().forEach(MJSetPos::sortCards);
        }
        return result;
    }

    /**
     * kai金
     *
     * @param set 集
     */
    public boolean kaiJin(AbsMJSetRoom set) {
        StandardMJRoomEnum.KaiJin jinType = ((StandardMJRoom) set.getRoom()).getJinType();
        if(jinType.value<=0){
            return false;
        }
        Integer jin = ((StandardMJRoomSet) set).popJin();
        List<MJCard> leftCards = set.getMJSetCard().getRandomCard().getLeftCards();
        MJCard card;
        Optional<MJCard> first;
        if (jin != 0 && (first = leftCards.stream().filter(n -> n.type == jin).findFirst()).isPresent()) {
            card = first.orElseThrow();
            if(jinType.deductedThisCard){
                //扣出来
                leftCards.remove(card);
            }
        }else{
            //没神牌那张，随机来一张
            card = leftCards.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(leftCards.size()));
            if(jinType.deductedThisCard){
                //扣出来
                leftCards.remove(card);
            }
        }
        if (null == card) {
            return false;
        }
        if(jinType.deductedThisCard){
            //扣出来
            set.getSetCard().getRandomCard().addNormalMoCnt(-1);
        }
        //进金
        MJCard jinJin = this.jinJin(card);
        if(jinType.value==3){
            //进金
            ((StandardMJRoomSet) set).setOpenCard(card.getCardID());
            set.getmJinCardInfo().addJinCard(jinJin);
        }else if(jinType.value==2){
            //双金
            set.getmJinCardInfo().addJinCard(card);
            set.getmJinCardInfo().addJinCard(jinJin);
        }else if(jinType.value==1){
            //单金
            set.getmJinCardInfo().addJinCard(card);
        }
        //通知开金
        set.kaiJinNotify(card, jinJin);
        return true;
    }


    /**
     * 进金
     *
     * @param card
     * @return
     */
    public MJCard jinJin(MJCard card) {
        //牌类型
        int type = card.type / 10;
        //牌大小
        int size = (card.cardID % 1000) / 100;
        int tem = size;
        //如果是箭牌类型
        if (card.type > 44) {
            //中发白一循环
            if (size >= 7) {
                size = 5;
            }
        } else if (card.type > 40) {
            //东南西北一循环
            if (size >= 4) {
                size = 1;
            }
        } else {
            //如果是 万条筒
            //牌 >= 9 就是 九 万条筒
            if (size >= 9) {
                // 进金为 一 万条筒
                size = 1;
            }
        }
        if (tem == size) {
            size++;
        }
        int cardId = (type * 10 + size) * 100 + 1;
        return new MJCard(cardId);
    }

}
