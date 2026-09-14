package business.global.mj;

import business.global.mj.manage.BaiBan;
import business.global.mj.pos.PosOpJinInfo;
import business.global.mj.pos.PosOpNotice;
import business.global.mj.pos.PosOpRecord;
import business.global.mj.ting.TingNormalImpl;
import business.global.room.mj.MJRoomPos;
import business.global.room.mj.MahjongRoom;
import cenum.PrizeType;
import cenum.mj.HuType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import com.ddm.server.common.utils.Random;
import jsproto.c2s.cclass.room.AbsBaseResults;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 玩家信息位置操作
 *
 * @author Administrator
 */
@Data
public abstract class MJSetPos {
    /**
     * 位置ID
     */
    private int posID = 0;
    /**
     * 玩家房间信息
     */
    private MJRoomPos roomPos = null;
    /**
     * 房间当局信息
     */
    private AbsMJSetRoom set = null;
    /**
     * 当前抓到的牌
     */
    private MJCard handCard = null;
    /**
     * 手牌
     */
    private List<MJCard> privateCards = new ArrayList<>();
    /**
     * 打出的牌
     */
    private List<Integer> outCardIDs = new ArrayList<>();
    /**
     * 已经亮出的牌
     */
    private List<List<Integer>> publicCardList = new ArrayList<>();
    /**
     * 可胡牌番薯
     */
    private List<Integer> huCardTypes = new ArrayList<>();
    /**
     * 最终胡的类型
     */
    private HuType huType = HuType.NotHu;
    /**
     * 麻将胡的动作类型，自摸和平胡
     */
    private MJHuOpType mHuOpType;
    /**
     * 标记用户明牌
     */
    private boolean isRevealCard = false;
    /**
     * 用户结算分数
     */
    private int endPoint = 0;
    /**
     * 扣分
     */
    private int deductPoint = 0;
    /**
     * 动作的牌
     */
    private int opCardId = 0;
    /**
     * 记录特殊胡牌动作
     */
    private Object opHuType = OpType.Not;
    /**
     * 默认听牌类
     */
    private Class<?> mActMrg = TingNormalImpl.class;
    /**
     * 麻将牌的初始信息
     */
    private MJCardInit mCardInitInfo = null;
    /**
     * 玩家动作信息
     */
    private AbsMJSetOp mSetOp;
    /**
     * 玩家位置结算
     */
    private AbsCalcPosEnd calcPosEnd;
    /**
     * 打金信息
     */
    private PosOpJinInfo outJinInfo;
    /**
     * 位置内部操作通知 记录内容： 吃列表、不能出列表、打出每张牌可听的牌列表。 该类记录的内容，会随着摸牌、打牌、回合开始等操作动作下发给玩家。
     */
    private PosOpNotice posOpNotice;
    /**
     * 位置内部操作记录 记录内容： 漏碰、漏胡、托管\机器人（用户可操作的动作）、操作列表、补花列表.
     */
    private PosOpRecord posOpRecord;
    /**
     * 白板替金
     */
    private BaiBan baiBan;
    /**
     * 记录特殊操作动作(如：单游、双游、三游等特殊记录动作)
     */
    private OpType specialOpType = OpType.Not;
    private boolean checkPao = false;
    /**
     * 当前局是否出牌过
     */
    private boolean haveOutCard = false;
    /**
     * 是否胡牌
     */
    private OpType huOpType = OpType.Not;
    /**
     * 第几胡
     */
    private int huCount = 0;
    /**
     * 一考的扣分
     */
    private double deductPointYiKao;
    /**
     * 一考的结算分
     */
    private double deductEndPoint;

    /**
     * 清空信息
     */
    public void clear() {
        if (null != this.privateCards) {
            this.privateCards.clear();
            this.privateCards = null;
        }
        if (null != this.outCardIDs) {
            this.outCardIDs.clear();
            this.outCardIDs = null;
        }
        if (null != this.publicCardList) {
            this.publicCardList.clear();
            this.publicCardList = null;
        }
        if (null != this.huCardTypes) {
            this.huCardTypes.clear();
            this.huCardTypes = null;
        }
        if (null != this.posOpRecord) {
            this.posOpRecord.clear();
            this.posOpRecord = null;
        }
        if (null != this.posOpNotice) {
            this.posOpNotice.clear();
            this.posOpNotice = null;
        }
        this.mCardInitInfo = null;
        this.roomPos = null;
        this.set = null;
        this.handCard = null;
        this.mHuOpType = null;
        this.outJinInfo = null;
    }

    public MJSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set, Class<?> mActMrg) {
        super();
        this.posID = posID;
        this.roomPos = roomPos;
        this.set = set;
        this.mActMrg = mActMrg;
        this.outJinInfo = new PosOpJinInfo();
        this.posOpNotice = new PosOpNotice();
        this.posOpRecord = new PosOpRecord();
    }


    /**
     * 玩家是否随机
     *
     * @return
     */
    public boolean isPosRandom() {
        // 公共
        return Random.isTrueDouble(this.set.getGodInfo().getMConfigMgr().getBasisRadix());
    }


    /**
     * @return
     */
    @Deprecated
    public boolean isLosePosRandom() {
        return false;
    }


    /**
     * 获取房间信息
     *
     * @return
     */
    public MahjongRoom getRoom() {
        return this.set.getRoom();
    }

    /**
     * 获取麻将牌
     *
     * @return
     */
    public AbsMJSetCard getMJSetCard() {
        return this.set.getMJSetCard();
    }

    /**
     * 获取玩家人数
     *
     * @return
     */
    public int getPlayerNum() {
        return this.set.getRoom().getPlayerNum();
    }

    /**
     * 获取位置
     *
     * @return
     */
    public AbsMJSetPos getMJSetPos() {
        return this.set.getMJSetPos(this.posID);
    }

    /**
     * 获取位置
     *
     * @param posID 指定
     * @return
     */
    public AbsMJSetPos getMJSetPos(int posID) {
        return this.set.getMJSetPos(posID);
    }


    /**
     * 获取用户Pid
     *
     * @return
     */
    public long getPid() {
        if (null != this.getRoomPos()) {
            return this.getRoomPos().getPid();
        }
        return 0;
    }

    /**
     * 获取结果数据
     *
     * @return
     */
    public AbsBaseResults getResults() {
        return (AbsBaseResults) roomPos.getResults();
    }

    /**
     * 设置结果数据
     *
     * @param results
     */
    public void setResults(AbsBaseResults results) {
        if (null != results) {
            this.getRoomPos().setResults(results);
        }
    }

    /**
     * 玩家总分和胡牌次数
     */
    public void pidPointEnd() {
        this.getRoomPos().calcRoomPoint(this.endPoint);
        this.getRoomPos().setTempPoint(this.getRoomPos().getTempPoint() + this.endPoint);
        if (PrizeType.RoomCard.equals(getRoom().getBaseRoomConfigure().getPrizeType())) {
            this.roomPos.addCountPoint(this.endPoint);
        }
        if (this.huType != HuType.NotHu && this.huType != HuType.DianPao) {
            this.getRoomPos().setHuCnt(this.getRoomPos().getHuCnt() + 1);
        }
    }

    public void setPidSumPointEnd(int point) {
        this.getRoomPos().calcRoomPoint(point);
    }

    public int pidHuCntEnd() {
        return this.getRoomPos().getHuCnt();
    }

    public int pidSumPointEnd() {
        return this.getRoomPos().getPoint();
    }

    /**
     * 用户是否明牌
     *
     * @return
     */
    public boolean isRevealCard() {
        return this.isRevealCard;
    }

    /**
     * 设置用户明牌状态
     *
     * @param isRevealCard
     */
    public void setRevealCard(boolean isRevealCard) {
        if (this.isRevealCard == isRevealCard) {
            return;
        }
        this.isRevealCard = isRevealCard;
    }

    /**
     * 设置可胡列表
     *
     * @param huCardTypes
     */
    public void setHuCardTypes(List<Integer> huCardTypes) {
        this.huCardTypes = huCardTypes;
    }

    /**
     * 获取可胡大小
     *
     * @return
     */
    public int sizeHuCardTypes() {
        return this.huCardTypes.size();
    }

    /**
     * 添加亮出的牌
     *
     * @param publicCards
     */
    public void addPublicCard(List<Integer> publicCards) {
        if (null == publicCards) {
            return;
        }
        this.publicCardList.add(publicCards);
    }

    /**
     * 获取亮出的牌组数量
     *
     * @return
     */
    public int sizePublicCardList() {
        return this.publicCardList.size();
    }

    /**
     * 添加打出的牌
     *
     * @param cardId
     */
    public void addOutCardIDs(int cardId) {
        if (cardId == 0) {
            return;
        }
        this.outCardIDs.add(cardId);
    }

    /**
     * 移除打出的牌
     *
     * @param cardId
     * @return
     */
    public boolean removeOutCardIDs(Integer cardId) {
        if (null == cardId) {
            return false;
        }

        int idex = this.outCardIDs.indexOf(cardId);
        if (idex == -1) {
            return false;
        }
        this.outCardIDs.set(idex, 0);
        return true;
    }


    /**
     * 获取打出的牌数量
     *
     * @return
     */
    public int sizeOutCardIDs() {
        return this.outCardIDs.size();
    }

    /**
     * 添加手牌
     *
     * @param tmp
     */
    public void addPrivateCard(MJCard tmp) {
        if (null == tmp) {
            return;
        }
        tmp.ownnerPos = posID;
        this.privateCards.add(tmp);
    }

    /**
     * 获取手牌的数量
     *
     * @return
     */
    public int sizePrivateCard() {
        return this.privateCards.size();
    }

    /**
     * 移除手牌
     *
     * @param tmp
     * @return
     */
    public int removePrivateCard(MJCard tmp) {
        if (null == tmp) {
            return 0;
        }
        // 创建迭代器
        Iterator<MJCard> it = this.privateCards.iterator();
        // 循环遍历迭代器
        while (it.hasNext()) {
            if (it.next().cardID == tmp.cardID) {
                it.remove();
                return tmp.cardID;
            }
        }
        return 0;
    }


    /**
     * 移除手牌
     *
     * @param tmp
     * @return
     */
    public int removePrivateCard(List<Integer> tmp) {
        if (null == tmp) {
            return 0;
        }
        this.setPrivateCard(this.getPrivateCard().stream().filter(k -> tmp.contains(k.getCardID())).collect(Collectors.toList()));
        return 0;
    }

    /**
     * 移除手牌列表
     *
     * @param tmp
     */
    public void removeAllPrivateCards(List<Integer> tmp) {
        this.setPrivateCard(this.getPrivateCard().stream().filter(k -> !tmp.contains(k.getCardID())).collect(Collectors.toList()));
    }


    /**
     * 移除手牌列表
     *
     * @param tmp
     */
    public void removeAllPrivateCard(List<MJCard> tmp) {
        if (null == tmp) {
            return;
        }
        Iterator<MJCard> tmpIt = tmp.iterator();
        while (tmpIt.hasNext()) {
            removePrivateCard(tmpIt.next());
        }
    }

    /**
     * 获取手牌列表
     *
     * @return
     */
    public List<MJCard> getPrivateCard() {
        return this.privateCards;
    }

    /**
     * 设置手牌列表
     *
     * @param tmp
     */
    public void setPrivateCard(List<MJCard> tmp) {
        this.privateCards = tmp;
    }

    /**
     * 获取指定的手牌
     *
     * @param i
     * @return
     */
    public MJCard getPCard(int i) {
        if (i >= sizePrivateCard()) {
            return null;
        }
        if (i <= -1) {
            return null;
        }
        return this.privateCards.get(i);
    }

    /**
     * 设置胡牌类型
     *
     * @param huType 胡牌类型
     * @param huPos  胡Pos
     */
    public void setHuCardType(HuType huType, int huPos, int roundId) {
        this.huType = huType;
        this.set.getMHuInfo().setHuPos(huPos);
        ;
        this.getSet().getMHuInfo().addHuPos(roundId, huPos);
    }


    /**
     * 获取全部的牌
     *
     * @return
     */
    public List<MJCard> allCards() {
        List<MJCard> allCards = new ArrayList<MJCard>();
        allCards.addAll(this.privateCards);
        if (null != this.handCard) {
            allCards.add(this.handCard);
        }
        return allCards;
    }


    /**
     * 设置首牌
     *
     * @param tmp
     */
    public void setHandCard(MJCard tmp) {
        if (null == tmp) {
            return;
        }
        tmp.ownnerPos = posID;
        this.handCard = tmp;
    }

    /**
     * 清空首牌
     */
    public void cleanHandCard() {
        this.handCard = null;
    }

    /**
     * 打牌、出牌
     *
     * @param card
     * @return
     */
    public boolean outCard(MJCard card) {
        boolean ret = false;
        if (this.handCard == card) {
            this.handCard = null;
            ret = true;
        } else if (0 < this.removePrivateCard(card)) {
            if (this.handCard != null) {
                this.privateCards.add(this.handCard);
                this.sortCards();
            }
            ret = true;
        }
        if (ret) {
            this.handCard = null;
            this.outCardIDs.add(card.cardID);
            this.set.getLastOpInfo().setLastOutCard(card.cardID);
            calcHuFan();
            this.getOutJinInfo().setOutJinCard(this.getSet().getmJinCardInfo().checkJinExist(card.type));
            setHaveOutCard(true);
        }
        return ret;
    }

    /**
     * 私有牌移到首牌
     */
    public void privateMoveHandCard() {
        if (sizePrivateCard() > 0) {
            int index = sizePrivateCard() - 1;
            this.handCard = this.privateCards.get(index);
            this.privateCards.remove(index);

        }
    }

    /**
     * 抓牌
     *
     * @param card
     */
    public void getCard(MJCard card) {
        this.handCard = card;
        card.ownnerPos = this.posID;
    }

    /**
     * 手牌牌序
     */
    public void sortCards() {
        Collections.sort(this.privateCards, new Comparator<MJCard>() {
            @Override
            public int compare(MJCard o1, MJCard o2) {
                // 金最前
                if (getSet().getmJinCardInfo().checkJinExist(o2.getType())
                        && !getSet().getmJinCardInfo().checkJinExist(o1.getType())) {
                    return 1;
                }
                if (getSet().getmJinCardInfo().checkJinExist(o1.getType())
                        && !getSet().getmJinCardInfo().checkJinExist(o2.getType())) {
                    return -1;
                }
                // 从小到大
                return o1.cardID - o2.cardID;
            }
        });
        this.sortBaiBanCards();
    }

    /**
     * 白板排序
     */
    protected abstract void sortBaiBanCards();

    /**
     * 初始化手牌
     *
     * @param cards 私有牌
     */
    public void init(List<MJCard> cards) {
        this.privateCards = new ArrayList<>(cards);
        for (MJCard card : this.privateCards) {
            card.ownnerPos = posID;
        }
        this.sortCards();
        this.calcHuFan();
    }

    /**
     * 强制发牌
     *
     * @param cards 私有牌
     */
    public void forcePopCard(List<MJCard> cards) {
        this.getPrivateCards().addAll(cards);
        for (MJCard card : this.getPrivateCards()) {
            card.setOwnnerPos(posID);
        }
    }

    /**
     * 初始刷新手牌和可胡列表
     */
    public void initSortCardsAndCalcHuFan() {
        this.sortCards();
        this.calcHuFan();
    }

    /**
     * 胡法检测
     */
    protected abstract void calcHuFan();

    /**
     * 初始化
     *
     * @param isJin 是否金
     * @return
     */
    public MJCardInit mjCardInit(boolean isJin) {
        return mjCardInit(this.allCards(), isJin);
    }

    /**
     * 初始化
     *
     * @param allCards
     * @param isJin
     * @return
     */
    public MJCardInit mjCardInit(List<MJCard> allCards, boolean isJin) {
        if (CollectionUtils.isEmpty(allCards)) {
            return null;
        }
        MJCardInit mInit = new MJCardInit();
        Map<Boolean, List<Integer>> map = allCards.stream().map(m -> m.getType())
                .collect(Collectors.partitioningBy(e -> checkJin(isJin, e)));
        // 添加牌列表
        mInit.addAllCardInts(map.get(Boolean.FALSE));
        // 添加牌列表
        mInit.addAllJins(map.get(Boolean.TRUE));
        return mInit;
    }

    /**
     * 检查金牌
     *
     * @param isJin T:判断金
     * @return T:金牌，F:普通牌
     */
    protected boolean checkJin(boolean isJin, int cardType) {
        if (isJin) {
            return this.getSet().getmJinCardInfo().checkJinExist(cardType);
        }
        return false;
    }

    /**
     * 获取玩家牌型数据和金数量
     * <p>
     * 玩家
     *
     * @param allCardList 手上牌
     * @param cardType    头牌
     * @param isJin       是否金
     * @return
     */
    public MJCardInit mCardInit(List<MJCard> allCardList, int cardType, boolean isJin) {
        MJCardInit mCardInit = mjCardInit(allCardList, isJin);
        if (null == mCardInit) {
            return mCardInit;
        }
        // 牌类型大于 0
        if (cardType > 0) {
            // 检查金牌
            if (checkJin(isJin, cardType)) {
                // 金牌
                mCardInit.addJins(cardType);
            } else {
                // 普通牌
                mCardInit.addCardInts(cardType);
            }
        }
        return mCardInit;
    }

    /**
     * 牌
     *
     * @param cardType 牌类型
     * @param isJin    T:检查金牌
     * @return
     */
    public MJCardInit mCardInit(int cardType, boolean isJin) {
        if (null == mCardInitInfo) {
            this.mCardInitInfo = this.mCardInit(this.allCards(), cardType, isJin);
        }
        return this.mCardInitInfo;
    }

    /**
     * 重新计算牌
     */
    public void clearMCardInit() {
        this.mCardInitInfo = null;
    }

    /**
     * 检查手上金牌的数量
     *
     * @return
     */
    public int isJinCard() {
        return (int) this.allCards().stream().filter(k -> this.getSet().getmJinCardInfo().checkJinExist(k.getType())).count();
    }

    /**
     * 检查私有牌金
     *
     * @return
     */
    public int isPrivateJinCard() {
        return (int) this.getPrivateCard().stream().filter(k -> this.getSet().getmJinCardInfo().checkJinExist(k.getType())).count();
    }

    /**
     * 检查玩家位置是否存在金牌
     *
     * @return
     */
    public boolean checkPosExistJin() {
        return this.allCards().stream().filter(k -> this.getSet().getmJinCardInfo().checkJinExist(k.getType())).findAny().isPresent();
    }

    /**
     * 麻将胡的动作类型，自摸和平胡
     *
     * @return
     */
    public MJHuOpType getmHuOpType() {
        return mHuOpType;
    }

    /**
     * 设置麻将胡的动作类型，自摸和平胡
     *
     * @param mHuOpType 麻将胡的动作类型，自摸和平胡
     */
    public void setmHuOpType(MJHuOpType mHuOpType) {
        this.mHuOpType = mHuOpType;
    }

    /**
     * 打金信息
     *
     * @return
     */
    public PosOpJinInfo getOutJinInfo() {
        return outJinInfo;
    }

    public void clearOpHuType() {
        this.opHuType = OpType.Not;
    }

    /**
     * 获取听的类
     *
     * @return
     */
    public Class<?> getmActMrg() {
        return mActMrg;
    }

    /**
     * 麻将玩家动作检查和操作
     *
     * @return
     */
    public AbsMJSetOp getmSetOp() {
        return mSetOp;
    }


    /**
     * 位置内部操作通知 记录内容： 吃列表、不能出列表、打出每张牌可听的牌列表。 该类记录的内容，会随着摸牌、打牌、回合开始等操作动作下发给玩家。
     *
     * @return
     */
    public PosOpNotice getPosOpNotice() {
        return posOpNotice;
    }

    /**
     * 位置内部操作记录 记录内容： 漏碰、漏胡、托管\机器人（用户可操作的动作）、操作列表、补花列表.
     *
     * @return
     */
    public PosOpRecord getPosOpRecord() {
        return posOpRecord;
    }

    /**
     * 记录操作的牌
     *
     * @return
     */
    public int getOpCardId() {
        return opCardId;
    }

    /**
     * 设置操作的牌
     *
     * @param opCardId
     */
    public void setOpCardId(int opCardId) {
        this.opCardId = opCardId;
    }

    /**
     * 获取白板替金
     *
     * @return
     */
    public BaiBan getBaiBan() {
        return baiBan;
    }

    /**
     * 设置白板替金
     *
     * @param baiBan
     */
    public void setBaiBan(BaiBan baiBan) {
        this.baiBan = baiBan;
    }


    public boolean isCheckPao() {
        return checkPao;
    }


    public void setCheckPao(boolean checkPao) {
        this.checkPao = checkPao;
    }

    /**
     * 是否出过牌
     * 天杠：发完牌后，玩家出牌前 抓到4张相同的牌
     * 如果出过牌后，接下来的杠就不是天杠
     *
     * @return
     */
    public boolean isHaveOutCard() {
        return haveOutCard;
    }

    /**
     * 是否出过牌
     *
     * @param haveOutCard
     */
    public void setHaveOutCard(boolean haveOutCard) {
        this.haveOutCard = haveOutCard;
    }

    /**
     * 清除可胡
     */
    public void clearHuCard() {
        if (huCardTypes != null) {
            huCardTypes.clear();
        }
    }

    /**
     * 是否胡牌
     *
     * @return
     */
    public boolean isHu() {
        return !OpType.Not.equals(this.huOpType);
    }

}