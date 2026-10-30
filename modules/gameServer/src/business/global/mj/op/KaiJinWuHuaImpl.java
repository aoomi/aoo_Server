package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import cenum.mj.MJSpecialEnum;

public class KaiJinWuHuaImpl implements OpCard {

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
     * 开金
     *
     * @param mSetPos 玩家信息
     */
    public void kaiJinCard(AbsMJSetPos mSetPos) {
        // 摸牌开金
        MJCard card = mSetPos.getSet().getMJSetCard().pop(false, mSetPos.getSet().getGodInfo().godHandCard(mSetPos));
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
            // 重新开金
            kaiJinCard(mSetPos);
        }
    }


}
