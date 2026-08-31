package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import cenum.mj.MJSpecialEnum;

/**
 * @author leo_wi
 * <p>
 * 开金 随机抽取一张,没有放回去，减去1和本身 则为金 万，条，筒，中发白，东西南北一个轮回
 * 中发白算一起，如翻开白则发是金；
 * 	东南西北算一起，如翻开东则北是金；
 * 	翻开的那张牌不能抓；
 * 	例如第一张牌是二万，那么一万就是宝。
 * 	此时牌里就剩下3张一万，4张二万；
 */
public class KaiJinBenJinAddBackJin7ZhangImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        // 操作开金
        this.opKaiJin(mSetPos);
        return false;
    }


    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * 开金
     *
     * @return
     */
    public void opKaiJin(AbsMJSetPos mSetPos) {
        // 开金的次数 >= 指定的开金数，开金完毕，通知开金成功。
        if (mSetPos.getSet().getmJinCardInfo().sizeJin() >= mSetPos.getSet().getmJinCardInfo().getKaiJinNum()) {
            MJCard backJin = mSetPos.getSet().getmJinCardInfo().getJin(1);
            // 开金通知。
            mSetPos.getSet().kaiJinNotify(backJin, jinJin(backJin));
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
        MJCard card = mSetPos.getSet().getMJSetCard().pop(false, mSetPos.getSet().getGodInfo().godHandCard(mSetPos));
        if (null == card) {
            // 没有牌
            return;
        }
        // 检查摸到牌的类型是否 < 50 , > 50 花
        if (card.getType() < MJSpecialEnum.NOT_HUA.value()) {
            // 添加金牌
            if (mSetPos.getSet().getmJinCardInfo().addJinCard(backJinJin(card)) && mSetPos.getSet().getmJinCardInfo().addJinCard(card)) {
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
        if (card.type >= 45) {
            //牌 >= 7 就是白板
            if (size >= 7) {
                // 进金为 红中
                size = 5;
            }
        } else if (card.type > 40) {
            //如果是风牌
            //牌 >= 4 就是 北风
            if (size >= 4) {
                //进金为 东风
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

    /**
     * 退金
     *
     * @param card
     * @return
     */
    public MJCard backJinJin(MJCard card) {
        //牌类型
        int type = card.type / 10;
        //牌大小
        int size = (card.cardID % 1000) / 100;
        int tem = size;
        //如果是箭牌类型
        if (card.type >= 45) {
            //牌 >= 5 就是 红中
            if (size <= 5) {
                // 退金为 白板
                size = 7;
            }
        } else if (card.type > 40) {
            //如果是风牌
            //牌 >= 1 就是 东风
            if (size <= 1) {
                //退金为 北风
                size = 4;
            }
        } else {
            //如果是 万条筒
            //牌 >= 1 就是 一 万条筒
            if (size <= 1) {
                // 退金为 九 万条筒
                size = 9;
            }
        }
        if (tem == size) {
            size--;
        }


        int cardId = (type * 10 + size) * 100 + 1;
        return new MJCard(cardId);
    }
}
