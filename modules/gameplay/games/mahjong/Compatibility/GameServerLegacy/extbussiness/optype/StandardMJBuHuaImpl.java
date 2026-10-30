package business.global.mj.extbussiness.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.manage.OpCard;
import cenum.mj.FlowerEnum;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 麻将补花
 *
 * @author Administrator
 */
public class StandardMJBuHuaImpl implements OpCard {


    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        if (FlowerEnum.PRIVATE.ordinal() == cardID) {
            if(((StandardMJRoom)mSetPos.getRoom()).isOneTimeBuHua()){
                return this.privateCardAppliqueAll(mSetPos);
            }
            return this.privateCardApplique(mSetPos);
        } else if (FlowerEnum.HAND_CARD.ordinal() == cardID) {
            this.handCardApplique(mSetPos);
        }
        return false;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        return false;
    }


    /**
     * 私有牌补花
     */
    private boolean privateCardApplique(AbsMJSetPos mSetPos) {
        StandardMJRoomSet set = (StandardMJRoomSet) mSetPos.getSet();
        StandardMJRoom room = (StandardMJRoom) set.getRoom();
        // 检查是否存花
        boolean checkExistFlower = mSetPos.allCards().stream().anyMatch(k -> room.getHuaList().contains(k.getType()));
        if (!checkExistFlower) {
            return false;
        }
        int allSize = mSetPos.getPrivateCards().size();
        mSetPos.setPrivateCard(mSetPos.getPrivateCard().stream().map(k -> hua(mSetPos, set, k)).filter(Objects::nonNull).collect(Collectors.toList()));
        //没得补花结束游戏
        if (allSize != mSetPos.getPrivateCards().size()) {
            set.endSet();
            return false;
        }
        mSetPos.sortCards();
        MJCard handCard = mSetPos.getHandCard();
        if (null != handCard && room.getHuaList().contains(handCard.getType())) {
            // 记录花
            this.addHua(mSetPos, handCard);
            MJCard hCard = mSetPos.getMJSetCard().pop(false);
            if (Objects.isNull(hCard)) {
                set.endSet();
                return false;
            }
            mSetPos.setHandCard(hCard);
            hCard.setOwnnerPos(mSetPos.getPosID());
        }
        set.MJApplique(mSetPos.getPosID());
        return true;
    }

    /**
     * 私有牌补花
     */
    private boolean privateCardAppliqueAll(AbsMJSetPos mSetPos) {
        StandardMJRoomSet set = (StandardMJRoomSet) mSetPos.getSet();
        StandardMJRoom room = (StandardMJRoom) set.getRoom();
        // 检查是否存花
        boolean checkExistFlower = mSetPos.allCards().stream().anyMatch(k -> room.getHuaList().contains(k.getType()));
        if (!checkExistFlower) {
            return false;
        }
        int allSize = mSetPos.getPrivateCards().size();
        mSetPos.setPrivateCard(mSetPos.getPrivateCard().stream().map(k -> hua(mSetPos, set, k)).filter(Objects::nonNull).collect(Collectors.toList()));
        //没得补花结束游戏
        if (allSize != mSetPos.getPrivateCards().size()) {
            set.endSet();
            return false;
        }
        mSetPos.sortCards();
        MJCard handCard = mSetPos.getHandCard();
        if (null != handCard && room.getHuaList().contains(handCard.getType())) {
            // 记录花
            this.addHua(mSetPos, handCard);
            MJCard hCard = mSetPos.getMJSetCard().pop(false);
            if (Objects.isNull(hCard)) {
                set.endSet();
                return false;
            }
            mSetPos.setHandCard(hCard);
            hCard.setOwnnerPos(mSetPos.getPosID());
        }

        // 检查是否存花
        boolean haveFlower = mSetPos.allCards().stream().anyMatch(k -> room.getHuaList().contains(k.getType()));
        if(haveFlower){
            return privateCardAppliqueAll(mSetPos);
        }else{
            set.MJApplique(mSetPos.getPosID());
            return true;
        }
    }


    /**
     * 花
     *
     * @param mSetPos 玩家信息
     * @param set     当局
     * @param mCard   牌
     * @return
     */
    private MJCard hua(AbsMJSetPos mSetPos, StandardMJRoomSet set, MJCard mCard) {
        if (((StandardMJRoom) set.getRoom()).getHuaList().contains(mCard.getType())) {
            // 记录花
            this.addHua(mSetPos, mCard);
            MJCard hCard = mSetPos.getMJSetCard().pop(false);
            hCard.setOwnnerPos(mSetPos.getPosID());
            return hCard;
        }
        return mCard;
    }

    /**
     * 添加花
     *
     * @param mSetPos
     * @param mCard   牌
     */
    private void addHua(AbsMJSetPos mSetPos, MJCard mCard) {
        if (mCard != null && ((StandardMJRoom) mSetPos.getRoom()).getHuaList().contains(mCard.getType())) {
            mSetPos.getPosOpRecord().getHuaList().add(mCard.getCardID());
            mSetPos.addOutCardIDs(mCard.getCardID());
        }
    }

    /**
     * 首牌补花
     *
     * @param mSetPos
     * @return
     */
    private void handCardApplique(AbsMJSetPos mSetPos) {
        StandardMJRoomSet set = (StandardMJRoomSet) mSetPos.getSet();
        MJCard handCard = mSetPos.getHandCard();
        if (null == handCard) {
            return;
        }
        if(!((StandardMJRoom) mSetPos.getRoom()).getHuaList().contains(handCard.getType())){
            return;
        }
        MJCard mCard = mSetPos.getMJSetCard().pop(false);
        if (null == mCard) {
            mSetPos.getSet().setHuangPos(mSetPos.getPosID());
            mSetPos.getSet().endSet();
            return;
        }
        this.addHua(mSetPos, handCard);
        mSetPos.setHandCard(mCard);
        set.MJApplique(mSetPos.getPosID());
        handCardApplique(mSetPos);
    }

}
