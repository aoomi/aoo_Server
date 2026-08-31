package business.global.mj.template;

import business.global.mj.AbsMJSetCard;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.RandomCard;
import cenum.mj.MJCardCfg;
import cenum.mj.MJSpecialEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 保定易县麻将
 * 每一局麻将底牌信息
 * 抓牌人是逆时针出手
 * 牌是顺时针被抓
 *
 * @author Huaxing
 */
public abstract class MJTemplateSetCard extends AbsMJSetCard {
    /**
     * 当局信息
     */
    public MJTemplateRoomSet set;
    /**
     * 留牌
     */
    public int liuPai;
    public List<Integer> checkHuCards = new ArrayList<>();

    public MJTemplateSetCard(MJTemplateRoomSet set) {
        this.set = set;
        this.room = set.getRoom();
        this.randomCard();
        initLiuPai();
        initCheckHuCards();
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
        this.checkHuCards = null;
        this.set = null;
    }

    public void initCheckHuCards() {
        for (int cardID : getRandomCard().getId2Cards().keySet()) {
            if (!checkHuCards.contains(cardID / 100) && cardID / 100 < MJSpecialEnum.NOT_HUA.value()) {
                checkHuCards.add(cardID / 100);
            }
        }
        Collections.sort(checkHuCards);
    }

    /**
     * 初始化留牌
     */
    protected void initLiuPai() {
        this.liuPai = getRoom().wanFa_LiuPaiNum();
        if (getRoom().wanFa_LiuPaiExtrasOptions().equals(MJTemplateRoomEnum.LiuPaiExtrasOptions.MAI_MA_LIU_PAI)) {
            liuPai += getRoom().wanFa_MaiMa().value;
        }
    }

    /**
     * 洗牌
     */
    @Override
    public void randomCard() {
        List<MJCardCfg> mCfgs = new ArrayList<MJCardCfg>();
        mCfgs.add(MJCardCfg.WANG);
        mCfgs.add(MJCardCfg.TIAO);
        mCfgs.add(MJCardCfg.TONG);
        mCfgs.add(MJCardCfg.FENG);
        mCfgs.add(MJCardCfg.JIAN);
        mCfgs.add(MJCardCfg.BAI);
        mCfgs.add(MJCardCfg.HUA);
        this.setRandomCard(new RandomCard(mCfgs, this.room.getPlayerNum(), this.room.getXiPaiList().size()));
        this.initDPos(this.set);
    }

    /**
     * 首次庄家设置
     *
     * @param set
     */
    @Override
    public void initDPos(AbsMJSetRoom set) {
        if (room.getCurSetID() == 1) {
            int dPos = this.firstRandomDPos() ? this.randomCard.getStartPaiPos() : set.getDPos();
            set.setDPos(dPos);
            room.setDPos(dPos);
        }
        // 清空洗牌列表
        this.room.getXiPaiList().clear();
    }

    @Override
    public MJCard pop(boolean isNormalMo, int cardType) {
        // 无牌
        if (isPopCardNull()) {
            return null;
        }
        MJCard ret = this.getGodCard(cardType);
        ret = Objects.nonNull(ret) ? ret : this.randomCard.removeLeftCards(0);
        if (isNormalMo) {
            this.randomCard.setNormalMoCnt(this.randomCard.getNormalMoCnt() + 1);
        } else {
            this.randomCard.setGangMoCnt(this.randomCard.getGangMoCnt() + 1);
        }
        return ret;
    }

    /**
     * 买码的牌
     *
     * @return
     */
    public List<Integer> calcMaList() {
        return getRandomCard().getLeftCards().stream().limit(getRoom().wanFa_MaiMa().value).map(k -> k.getCardID()).collect(Collectors.toList());
    }

    /**
     * 是否可以海底捞月
     *
     * @param needCount 需要的张数
     * @return
     */
    public boolean isStartHaiDiLaoYue(int needCount) {
        if (needCount <= 0) {
            return false;
        }
        return (liuPai + needCount) == getRandomCard().getSize();
    }

    /**
     * 是否已经不能再摸牌了
     *
     * @return
     */
    public boolean isPopCardNull() {
        return liuPai >= getRandomCard().getSize();
    }

    public MJTemplateRoom getRoom() {
        return (MJTemplateRoom) super.room;
    }

    /**
     * 增加留牌数
     */
    public void addLiuPai() {
        if (MJTemplateRoomEnum.LiuPaiExtrasOptions.GANG_CARD_LIU_PAI.equals(getRoom().wanFa_LiuPaiExtrasOptions())) {
            liuPai += getRoom().wanFa_GangCardLiuPaiNum().value;
        }
    }
}

