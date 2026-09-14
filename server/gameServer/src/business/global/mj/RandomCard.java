package business.global.mj;

import cenum.mj.MJCardCfg;
import cenum.mj.MJSpecialEnum;
import com.ddm.server.common.utils.CommMath;
import lombok.Data;

import java.util.*;

/**
 * 随机发牌
 *
 * @author Administrator
 */
@Data
public class RandomCard {
    /**
     * 麻将牌编号
     */
    public List<MJCard> leftCards = new LinkedList<>();
    /**
     * 正常摸牌数量
     */
    private int normalMoCnt = 0;
    /**
     * 杠牌后，倒序摸牌数量
     */
    private int gangMoCnt = 0;
    /**
     * 牌编号
     */
    private Hashtable<Integer, MJCard> id2Cards = new Hashtable<>();
    /**
     * 筛子点数
     */
    public List<Integer> Saizi = new ArrayList<>();
    /**
     * 开始拿牌的位置
     */
    private int startPaiPos = 0;
    /**
     * 开始拿牌的蹲位
     */
    private int startPaiDun = 0;
    /**
     * 人数
     */
    private int playerNum;
    /**
     * 随机
     */
    private Random random;
    /**
     * 洗牌次数
     */
    private int xiPaiNum = 0;

    public RandomCard(List<MJCardCfg> mCfgs, int playerNum, int xiPaiNum) {
        this.playerNum = playerNum;
        this.xiPaiNum = xiPaiNum;
        this.randomSaizi();
        this.init(mCfgs);
    }

    public RandomCard() {

    }

    /**
     * 清空
     */
    public void clear() {
        if (null != this.leftCards) {
            this.leftCards.clear();
            this.leftCards = null;
        }
        if (null != this.id2Cards) {
            this.id2Cards.clear();
            this.id2Cards = null;
        }
        this.Saizi = null;
    }

    /**
     * 初始其他动作
     */
    public void initOtherActions() {

    }

    /**
     * 剩余的所有牌
     *
     * @return
     */
    public List<MJCard> getLeftCards() {
        return this.leftCards;
    }

    /**
     * 移除指定位置的一张牌
     *
     * @param index 牌位置
     * @return
     */
    public MJCard removeLeftCards(int index) {
        return this.leftCards.remove(index);
    }

    /**
     * 获取指定的牌
     *
     * @param id 牌ID
     * @return
     */
    public MJCard getCardByID(int id) {
        return this.id2Cards.get(id);
    }

    /**
     * 移除指定类型的牌
     *
     * @param cardType 牌类型
     * @return
     */
    public MJCard removeLeftCardType(int cardType) {
        MJCard mCard = null;
        // 创建迭代器
        Iterator<MJCard> it = this.leftCards.iterator();
        // 循环遍历迭代器
        while (it.hasNext()) {
            mCard = it.next();
            if (mCard.type == cardType) {
                it.remove();
                return mCard;
            }
        }
        return null;
    }


    /**
     * 获取指定类型的牌
     *
     * @param cardType 牌类型
     * @return
     */
    public MJCard getLeftCardType(int cardType) {
        return this.leftCards.stream().filter(k -> k.getType() == cardType).findAny().orElse(null);
    }

    /**
     * 移除指定ID的牌
     *
     * @param cardId 牌ID
     * @return
     */
    public MJCard removeLeftCardId(int cardId) {
        MJCard mCard = null;
        // 创建迭代器
        Iterator<MJCard> it = this.leftCards.iterator();
        // 循环遍历迭代器
        while (it.hasNext()) {
            mCard = it.next();
            if (mCard.cardID == cardId) {
                it.remove();
                return mCard;
            }
        }
        return null;
    }

    /**
     * 正常摸牌数
     *
     * @return
     */
    public int getNormalMoCnt() {
        return this.normalMoCnt;
    }

    /**
     * 杠摸牌数
     *
     * @return
     */
    public int getGangMoCnt() {
        return gangMoCnt;
    }

    /**
     * 设置杠摸牌数
     *
     * @param gangMoCnt 数
     */
    public void setGangMoCnt(int gangMoCnt) {
        if (this.gangMoCnt == gangMoCnt) {
            return;
        }
        this.gangMoCnt = gangMoCnt;
    }

    /**
     * 设置正常摸牌数
     *
     * @param normalMoCnt 数
     */
    public void setNormalMoCnt(int normalMoCnt) {
        if (this.normalMoCnt == normalMoCnt) {
            return;
        }
        this.normalMoCnt = normalMoCnt;
    }

    /**
     * 获取牌字典
     *
     * @return
     */
    public Hashtable<Integer, MJCard> getId2Cards() {
        return this.id2Cards;
    }

    /**
     * 随机筛子
     */
    public void randomSaizi() {
        int totalPoint = 0;
        int totalMin = 6;
        for (int i = 0; i < MJSpecialEnum.DICE.value(); i++) {
            int tmp = CommMath.randomInt(1, 6);
            this.Saizi.add(tmp);
            totalPoint += tmp;
            totalMin = Math.min(totalMin, tmp);
        }
        this.startPaiDun = totalMin;
        this.startPaiPos = (totalPoint - 1) % this.playerNum;
    }

    /**
     * 开始发牌的位置
     *
     * @return
     */
    public int getStartPaiPos() {
        return this.startPaiPos;
    }

    /**
     * 开始拿牌墩位置
     *
     * @return
     */
    public int getStartPaiDun() {
        return this.startPaiDun;
    }

    /**
     * 初始牌
     *
     * @param mCfgs
     */
    protected void init(List<MJCardCfg> mCfgs) {
        random = new Random();
        this.leftCards = new ArrayList<>();
        // 万
        this.wang(mCfgs);
        // 条
        this.tiao(mCfgs);
        // 筒
        this.tong(mCfgs);
        // 风头
        this.fengTou(mCfgs);
        // 箭牌
        this.jianPai(mCfgs);
        // 红中
        this.hongZhong(mCfgs);
        // 发财
        this.faCai(mCfgs);
        // 白板
        this.baiBan(mCfgs);
        // 花
        this.hua(mCfgs);
        // 正常摸牌数 = 总牌数 - 可使用的牌数
        this.normalMoCnt = MJSpecialEnum.TOTAL_CARD.value() - this.leftCards.size();
        for (int i = 0; i < this.leftCards.size(); i++) {
            MJCard card = this.leftCards.get(i);
            this.id2Cards.put(card.cardID, card);
        }
        // 先随机打散牌队列一次
        this.leftCards = CommMath.getRandomList((ArrayList<MJCard>) this.leftCards);
        // 获取洗牌次数 = 洗牌次数 <= 0?按在摸牌位置ID洗牌:洗牌数
        this.xiPaiNum = this.xiPaiNum <= 0 ? this.getStartPaiPos() : this.xiPaiNum;
        // 洗牌次数
        for (int i = 0; i < this.xiPaiNum; i++) {
            // 随机打散
            this.leftCards = CommMath.getRandomList((ArrayList<MJCard>) this.leftCards);
        }
    }

    /**
     * 增加摸牌数
     *
     * @param cnt
     */
    public void addNormalMoCnt(int cnt) {
        this.normalMoCnt += cnt;
    }

    /**
     * 增加杠摸数
     *
     * @param cnt
     */
    public void addGangMoCnt(int cnt) {
        this.gangMoCnt += cnt;
    }

    /**
     * 获取可使用牌数
     *
     * @return
     */
    public int getSize() {
        return this.leftCards.size();
    }

    /**
     * 随机取牌
     *
     * @return
     */
    public MJCard dispatch() {
        return this.leftCards.remove(random.nextInt(getSize()));
    }

    /**
     * 随机取牌
     *
     * @return
     */
    public MJCard randomCard() {
        return this.leftCards.get((random.nextInt(getSize())));
    }


    /**
     * 玩家牌
     *
     * @param cnt      发牌数
     * @param isNormal T:正常摸牌,F:杠摸
     * @param posId    位置Id
     * @return
     */
    public List<MJCard> popList(int cnt, boolean isNormal, int posId) {
        return popList(cnt, isNormal);
    }

    /**
     * 玩家牌
     *
     * @param cnt      发牌数
     * @param isNormal T:正常摸牌,F:杠摸
     * @return
     */
    public List<MJCard> popList(int cnt, boolean isNormal) {
        List<MJCard> popList = new ArrayList<MJCard>();
        for (int i = 0; i < cnt; i++) {
            popList.add(dispatch());
        }
        if (isNormal) {
            this.normalMoCnt += cnt;
        } else {
            this.gangMoCnt += cnt;
        }
        return popList;
    }

    /**
     * 万牌
     *
     * @param mCfgs 配置
     * @return
     */
    public int wang(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.WANG)) {
            initCard(11, 19, MJCardCfg.WANG);
            return MJSpecialEnum.NOT.value();
        } else {
            return 36;
        }
    }

    /**
     * 条牌
     *
     * @param mCfgs 配置
     * @return
     */
    public int tiao(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.TIAO)) {
            this.initCard(21, 29, MJCardCfg.TIAO);
            return MJSpecialEnum.NOT.value();
        } else {
            return 36;
        }
    }

    /**
     * 筒牌
     *
     * @param mCfgs 配置
     * @return
     */
    public int tong(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.TONG)) {
            this.initCard(31, 39, MJCardCfg.TONG);
            return MJSpecialEnum.NOT.value();
        } else {
            return 36;
        }
    }

    /**
     * 风头（东南西北）
     *
     * @param mCfgs 配置
     * @return
     */
    public int fengTou(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.FENG)) {
            this.initCard(41, 44, MJCardCfg.FENG);
            return MJSpecialEnum.NOT.value();
        } else {
            return 16;
        }
    }

    /**
     * 箭牌（中发）
     *
     * @param mCfgs 配置
     * @return
     */
    public int jianPai(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.JIAN)) {
            this.initCard(45, 46, MJCardCfg.JIAN);
            return MJSpecialEnum.NOT.value();
        } else {
            return 8;
        }
    }

    /**
     * 红中
     *
     * @param mCfgs 配置
     * @return
     */
    public int hongZhong(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.ZHONG)) {
            this.initCard(45, 45, MJCardCfg.ZHONG);
            return MJSpecialEnum.NOT.value();
        } else {
            return 4;
        }
    }

    /**
     * 发财
     *
     * @param mCfgs 配置
     * @return
     */
    public int faCai(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.FA)) {
            this.initCard(46, 46, MJCardCfg.FA);
            return MJSpecialEnum.NOT.value();
        } else {
            return 4;
        }
    }

    /**
     * 白板
     *
     * @param mCfgs 配置
     * @return
     */
    public int baiBan(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.BAI)) {
            this.initCard(47, 47, MJCardCfg.BAI);
            return MJSpecialEnum.NOT.value();
        } else {
            return 4;
        }
    }

    /**
     * 花牌
     *
     * @param mCfgs 配置
     * @return
     */
    public int hua(List<MJCardCfg> mCfgs) {
        if (mCfgs.contains(MJCardCfg.HUA)) {
            for (int i = 51; i <= 58; i++) {
                this.leftCards.add(new MJCard(i * 100 + 1));
            }
            return MJSpecialEnum.NOT.value();
        } else {
            return 8;
        }
    }

    /**
     * 初始牌
     *
     * @param start 起始
     * @param end   结束
     * @param cfg   类型
     */
    public void initCard(int start, int end, MJCardCfg cfg) {
        List<Integer> tmp = new ArrayList<Integer>();
        for (int i = start; i <= end; i++) {
            int cardId1 = i * 100 + 1;
            int cardId2 = i * 100 + 2;
            int cardId3 = i * 100 + 3;
            int cardId4 = i * 100 + 4;
            tmp.add(cardId1 / 100);
            tmp.add(cardId2 / 100);
            tmp.add(cardId3 / 100);
            tmp.add(cardId4 / 100);
            this.leftCards.add(new MJCard(cardId1));
            this.leftCards.add(new MJCard(cardId2));
            this.leftCards.add(new MJCard(cardId3));
            this.leftCards.add(new MJCard(cardId4));
        }
    }

    /**
     * 随机筛子
     *
     * @return
     */
    public int RandomSaizi() {
        int totalPoint = 0;
        for (int i = 0; i < MJSpecialEnum.DICE.value(); i++) {
            int tmp = CommMath.randomInt(1, 6);
            totalPoint += tmp;
        }
        return (totalPoint - 1) % this.playerNum;
    }

    /**
     * 强制发牌（测试用）
     *
     * @param forcePop 强制牌列表
     * @return
     */
    public List<MJCard> forcePopList(List<Integer> forcePop) {
        if (null == forcePop) {
            return null;
        }
        List<MJCard> ret = new ArrayList<>();
        for (int type : forcePop) {
            MJCard mCard = this.removeLeftCardType(type);
            if (null != mCard) {
                ret.add(mCard);
                this.setNormalMoCnt(this.getNormalMoCnt() + 1);
            }
        }

        return ret;
    }

    /**
     * 添加万条筒
     *
     * @param cfg
     */
    public void addWTT(List<MJCardCfg> cfg) {
        // 万
        this.wang(cfg);
        // 条
        this.tiao(cfg);
        // 筒
        this.tong(cfg);
    }

    /**
     * 添加风箭白
     *
     * @param cfg
     */
    public void addFJB(List<MJCardCfg> cfg) {
        // 风头
        this.fengTou(cfg);
        // 箭牌
        this.jianPai(cfg);
        // 白板
        this.baiBan(cfg);
    }

    /**
     * 洗牌
     */
    public void shuffleCard() {
        // 先随机打散牌队列一次
        this.leftCards = CommMath.getRandomList((ArrayList<MJCard>) this.leftCards);
        // 获取洗牌次数 = 洗牌次数 <= 0?按在摸牌位置ID洗牌:洗牌数
        this.setXiPaiNum(this.getXiPaiNum() <= 0 ? this.getStartPaiPos() : this.getXiPaiNum());
        // 洗牌次数
        for (int i = 0; i < this.getXiPaiNum(); i++) {
            // 随机打散
            this.leftCards = CommMath.getRandomList((ArrayList<MJCard>) this.leftCards);
        }
    }
}
