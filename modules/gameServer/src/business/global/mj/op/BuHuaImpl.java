package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import cenum.mj.FlowerEnum;
import cenum.mj.MJSpecialEnum;

import java.util.stream.Collectors;

public class BuHuaImpl implements OpCard {


    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        if (FlowerEnum.PRIVATE.ordinal() == cardID) {
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
        // 检查是否存花
        boolean checkExistFlower = mSetPos.allCards().stream().filter(k -> k.getType() >= MJSpecialEnum.NOT_HUA.value()).findAny().isPresent();
        if (!checkExistFlower) {
            return false;
        }
        // 设置私有牌
        mSetPos.setPrivateCard(mSetPos.getPrivateCard().stream().map(k -> hua(mSetPos, k)).filter(k -> null != k).collect(Collectors.toList()));
        mSetPos.sortCards();
        MJCard handCard = mSetPos.getHandCard();
        if (null != handCard && handCard.getType() >= MJSpecialEnum.NOT_HUA.value()) {
            // 记录花
            this.addHua(mSetPos, handCard);
            mSetPos.setHandCard(mSetPos.getMJSetCard().pop(false));
        }
        // 麻将补花
        mSetPos.getSet().MJApplique(mSetPos.getPosID());
        return true;
    }


    /**
     * 花
     *
     * @param mSetPos 玩家信息
     * @param mCard   牌
     * @return
     */
    private MJCard hua(AbsMJSetPos mSetPos, MJCard mCard) {
        if (mCard.getType() >= MJSpecialEnum.NOT_HUA.value()) {
            // 记录花
            this.addHua(mSetPos, mCard);
            MJCard hCard = mSetPos.getMJSetCard().pop(false);
            hCard.setOwnnerPos(mSetPos.getPosID());
            return hCard;
        }
        return mCard;
    }

    /**
     * 首牌补花
     *
     * @param mSetPos
     */
    public void handCardApplique(AbsMJSetPos mSetPos) {
        MJCard mHCard = mSetPos.getHandCard();
        if (null == mHCard || mHCard.getType() < MJSpecialEnum.NOT_HUA.value()) {
            // 没有首牌 或者 首牌不是花牌
            return;
        }
        // 补一张牌
        MJCard mCard = mSetPos.getMJSetCard().pop(false);
        // 补到牌
        if (mCard == null) {
            mSetPos.cleanHandCard();
        } else {
            mSetPos.setHandCard(mCard);
        }
        // 添加花牌记录。
        addHua(mSetPos, mHCard);
        // 通知补花
        mSetPos.getSet().MJApplique(mSetPos.getPosID());
        if (null == mCard) {
            // 没补到 臭庄
            mSetPos.getSet().getMHuInfo().setHuangPos(mSetPos.getPosID());
            mSetPos.getSet().endSet();
            return;
        }
        // 再次检查首牌补花
        handCardApplique(mSetPos);

    }

    private void addHua(AbsMJSetPos mSetPos, MJCard mCard) {
        mSetPos.getPosOpRecord().addHua(mCard.cardID);
        mSetPos.addOutCardIDs(mCard.cardID);
    }


}
