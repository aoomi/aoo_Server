package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import business.global.mj.set.GodInfo;
import cenum.mj.MJSpecialEnum;

/**
 * @author leo_wi
 * <p>
 * 开金 随机抽取一张,开出来后放回去，总共有四张金
 */
public class KaiJinBenJin4ZhangImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        // 操作开金
        this.opKaiJin(mSetPos);
        return false;
    }


    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        return false;
    }

    /**
     * 开金
     *
     * @param isNormalMo
     * @return
     */
    public void opKaiJin(AbsMJSetPos mSetPos) {
        // 开金的次数 >= 指定的开金数，开金完毕，通知开金成功。
        if (mSetPos.getSet().getmJinCardInfo().sizeJin() >= mSetPos.getSet().getmJinCardInfo().getKaiJinNum()) {
            // 开金通知。
            mSetPos.getSet().kaiJinNotify(mSetPos.getSet().getmJinCardInfo().getJin(1), mSetPos.getSet().getmJinCardInfo().getJin(2));
            return;
        }
        // 开金
        this.kaiJinCard(mSetPos);
        // 再操作开金
        this.opKaiJin(mSetPos);
    }


    /**
     * 开金补花通知
     *
     * @param mSetPos 玩家信息
     * @param mCard   开出的牌
     */
    private void kaiJinApplique(AbsMJSetPos mSetPos, MJCard mCard) {
        // 添加打出的牌
        mSetPos.addOutCardIDs(mCard.getCardID());
        // 添加花
        mSetPos.getPosOpRecord().addHua(mCard.getCardID());
        // 通知补花,补花位置。
        mSetPos.getSet().MJApplique(mSetPos.getPosID());
    }


    /**
     * 开金
     *
     * @param mSetPos 玩家信息
     */
    public void kaiJinCard(AbsMJSetPos mSetPos) {
        // 摸牌开金
        // 摸牌开金
        MJCard card;
        GodInfo godInfo = mSetPos.getSet().getGodInfo();
        if (godInfo.isGodCardMode() && godInfo.getJin(0) > 0) {
            int jin = godInfo.getJin(0);
            card = mSetPos.getSet().getSetCard().getCardByID(jin * 100 + 1);
        } else {
            card = mSetPos.getSet().getMJSetCard().getRandomCard().randomCard();
        }
        if (null == card) {
            // 没有牌
            return;
        }
        // 检查摸到牌的类型是否 < 50 , > 50 花
        if (card.getType() < MJSpecialEnum.NOT_HUA.value()) {
            // 添加金牌
            if (mSetPos.getSet().getmJinCardInfo().addJinCard(card)) {
                // 金牌添加成功
                return;
            } else {
                // 添加金失败，重新开金
                kaiJinCard(mSetPos);
            }
        } else {
            // 开金开到花牌，通知补花，并且重新开金
            kaiJinApplique(mSetPos, card);
            // 重新开金
            kaiJinCard(mSetPos);
        }
    }
}
