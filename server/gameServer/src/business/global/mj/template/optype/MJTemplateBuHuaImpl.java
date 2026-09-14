package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.op.BuHuaImpl;
import business.global.mj.template.MJTemplateRoom;
import cenum.mj.FlowerEnum;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 春、夏、秋、冬
 */
public class MJTemplateBuHuaImpl extends BuHuaImpl {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        Set<Integer> buHuaTypeSet = ((MJTemplateRoom) mSetPos.getRoom()).getBuHuaTypeSet();
        if (Objects.isNull(buHuaTypeSet) || buHuaTypeSet.isEmpty()) {
            return false;
        }

        if (FlowerEnum.PRIVATE.ordinal() == cardID) {
            return this.privateCardApplique(mSetPos);
        } else if (FlowerEnum.HAND_CARD.ordinal() == cardID) {
            this.handCardApplique(mSetPos);
        }
        return false;
    }

    /**
     * 私有牌补花
     */
    private boolean privateCardApplique(AbsMJSetPos mSetPos) {
        // 检查是否存花		
        if (mSetPos.allCards().stream().noneMatch(k -> checkHua(mSetPos, k))) {
            return false;
        }
        // 设置私有牌		
        mSetPos.setPrivateCard(mSetPos.getPrivateCard().stream().map(k -> hua(mSetPos, k)).filter(k -> null != k).collect(Collectors.toList()));
        mSetPos.sortCards();
        MJCard handCard = mSetPos.getHandCard();
        if (checkHua(mSetPos, handCard)) {
            // 记录花		
            this.addHua(mSetPos, handCard);
            MJCard mjCard = mSetPos.getMJSetCard().pop(false);
            mSetPos.getCard(mjCard);
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
        if (checkHua(mSetPos, mCard)) {
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
        if (!checkHua(mSetPos, mHCard)) {
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
        return;

    }

    private void addHua(AbsMJSetPos mSetPos, MJCard mCard) {
        mSetPos.getPosOpRecord().addHua(mCard.cardID, mCard.type);
        mSetPos.addOutCardIDs(mCard.cardID);
    }

    public boolean checkHua(AbsMJSetPos mSetPos, MJCard mjCard) {
        if (Objects.isNull(mjCard)) {
            return false;
        }
        return ((MJTemplateRoom) mSetPos.getRoom()).getBuHuaTypeSet().contains(mjCard.type);
    }


}
