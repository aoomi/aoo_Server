package business.global.mj.scjymj;

import business.global.mj.AbsMJSetCard;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.RandomCard;
import cenum.mj.MJCardCfg;

import java.util.ArrayList;
import java.util.List;

/**
 * 江苏扬中麻将 每一局麻将底牌信息 抓牌人是逆时针出手 牌是顺时针被抓
 *
 * @author Huaxing
 */
public class SCJYMJSetCard extends AbsMJSetCard {
    public SCJYMJRoomSet set;

    public SCJYMJSetCard(SCJYMJRoomSet set) {
        this.set = set;
        this.room = set.getRoom();
        this.randomCard();
    }

    /**
     * 洗牌
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void randomCard() {
        List<MJCardCfg> mCfgs = new ArrayList<>();
        mCfgs.add(MJCardCfg.TIAO);
        mCfgs.add(MJCardCfg.TONG);
        set.getmJinCardInfo().addJinCard(new MJCard(6001));
        baseRandomCard(this.set, mCfgs);
        this.initDPos(this.set);
    }


    public void baseRandomCard(AbsMJSetRoom set, List<MJCardCfg> mCfgs) {
        this.randomCard = new RandomCard(mCfgs, this.room.getPlayerNum(), 0);
    }

    @Override
    public MJCard pop(boolean isNormalMo, int cardType) {
        int sizeCard = this.randomCard.getSize();
        // 留牌：不留牌；
        // 最后1张牌摸完后没有胡牌则为流局
        if (sizeCard <= 0) {
            this.set.setHuang(true);
            return null;
        }
        if (sizeCard <= 1) {
            // 海底捞月：抓到可以抓的最后一张牌时胡牌；
            this.set.setLastCard(true);
        }
        if (sizeCard <= 4) {
            // 最后4张牌必须胡牌
            this.set.setLastFourCard(true);
        }
        MJCard ret = this.getGodCard(cardType);
        ret = null != ret ? ret : this.randomCard.removeLeftCards(0);
        if (isNormalMo) {
            this.randomCard.setNormalMoCnt(this.randomCard.getNormalMoCnt() + 1);
        } else {
            this.randomCard.setGangMoCnt(this.randomCard.getGangMoCnt() + 1);
        }
        return ret;
    }


    @Override
    protected boolean firstRandomDPos() {
        return true;
    }
}
