package business.global.mj;

import business.global.room.mj.MahjongRoom;
import cenum.mj.MJCardCfg;

import java.util.List;

/**
 * 麻将牌
 *
 * @author Administrator
 */
public abstract class AbsMJSetCard {
    /**
     * 房间信息
     */
    protected MahjongRoom room;
    /**
     * 随机发牌
     */
    protected RandomCard randomCard;

    /**
     * 随机发牌
     *
     * @return
     */
    public RandomCard newRandomCard(List<MJCardCfg> mCfgs, int playerNum, int xiPaiNum) {
        return new RandomCard(mCfgs, playerNum, xiPaiNum);
    }

    /**
     * 随机发牌
     */
    public abstract void randomCard();

    /**
     * 摸牌
     *
     * @param isNormalMo T:正常摸牌，F:杠牌摸牌
     * @return
     */
    public MJCard pop(boolean isNormalMo) {
        return this.pop(isNormalMo, 0);
    }

    /**
     * 摸牌
     *
     * @param isNormalMo T:正常摸牌，F:杠牌摸牌
     * @param cardType   牌类型（默认：0）
     * @return
     */
    public abstract MJCard pop(boolean isNormalMo, int cardType);

    /**
     * 获取随机发牌
     *
     * @return
     */
    public RandomCard getRandomCard() {
        return this.randomCard;
    }

    /**
     * 获取指定的牌
     *
     * @param id 牌ID
     * @return
     */
    public MJCard getCardByID(int id) {
        return this.randomCard.getCardByID(id);
    }

    /**
     * 发牌
     *
     * @param cnt 数量
     * @param i   位置
     * @return
     */
    public List<MJCard> popList(int cnt, int i) {
        return this.randomCard.popList(cnt, true);
    }

    /**
     * 发牌
     *
     * @param cnt 数
     * @return
     */
    public List<MJCard> popList(int cnt) {
        return this.randomCard.popList(cnt, false);
    }

    /**
     * 强制发牌（测试用）
     *
     * @param forcePop 强制牌列表
     * @return
     */
    public List<MJCard> forcePopList(List<Integer> forcePop) {
        return this.randomCard.forcePopList(forcePop);
    }

    /**
     * 内部测试用。
     *
     * @param cardType 牌ID
     * @return
     */
    public MJCard getGodCard(int cardType) {
        if (cardType <= 0) {
            return null;
        }
        return this.randomCard.removeLeftCardType(cardType);
    }

    /**
     * 首次随机庄
     *
     * @return T:首次随机庄，F:房主庄
     */
    protected abstract boolean firstRandomDPos();

    /**
     * 首次庄家设置
     *
     * @param set
     */
    public void initDPos(AbsMJSetRoom set) {
        if (room.getCurSetID() == 1) {
            set.setDPos(this.firstRandomDPos() ? this.randomCard.getStartPaiPos() : set.getDPos());
        }
        // 清空洗牌列表
        this.room.getXiPaiList().clear();
    }

    /**
     * 清空随机发牌
     */
    public void randomCardClear() {
        if (null != this.randomCard) {
            this.randomCard.clear();
        }
    }

    /**
     * 清空
     */
    public void clear() {
        if (null != this.randomCard) {
            this.randomCard.clear();
            this.randomCard = null;
        }
        this.room = null;
    }

    /**
     * 设置随机牌
     *
     * @param randomCard
     */
    public void setRandomCard(RandomCard randomCard) {
        this.randomCard = randomCard;
    }


}
