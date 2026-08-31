package business.global.mj;

import business.global.mj.set.*;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomSet;
import business.global.room.base.RoomPlayBack;
import business.global.room.mj.MahjongRoom;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.RoomDissolutionState;
import cenum.room.RoomEndPointEnum;
import cenum.room.SetState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetEnd;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.MJRoomSetInfo;
import jsproto.c2s.cclass.playback.PlayBackData;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public abstract class AbsMJSetRoom extends AbsRoomSet {
    /**
     * 麻将金牌信息
     */
    private final MJJinCardInfo mJinCardInfo;
    /**
     * 神信息
     */
    private final GodInfo godInfo;
    /**
     * 最近操作的信息
     */
    private final LastOpInfo lastOpInfo;
    /**
     * 麻将胡牌信息
     */
    private final MJHuInfo mHuInfo = new MJHuInfo();
    /**
     * 本局结算信息
     */
    public BaseMJRoom_SetEnd setEnd = null;
    /**
     * 每个位置信息
     */
    protected Map<Integer, AbsMJSetPos> posDict = null;
    /**
     * 房间信息
     */
    protected MahjongRoom room = null;
    /**
     * 庄家位置
     */
    protected int dPos = 0;
    /**
     * 延迟发牌的时间
     */
    protected int InitTime = 1000;
    /**
     * 回放管理器
     */
    protected RoomPlayBack roomPlayBack;
    /**
     * 麻将牌
     */
    protected AbsMJSetCard setCard = null;
    /**
     * 上回合记录
     */
    protected AbsMJSetRound preRound = null;
    /**
     * 当前回合
     */
    protected AbsMJSetRound curRound = null;
    /**
     * 玩家操作管理
     */
    protected AbsMJSetPosMgr setPosMgr;
    /**
     * 记录初始回合状态。（用于起手枪金、天胡、三金倒用。）
     */
    protected boolean isAtFirstHu = true;
    /**
     * 回合ID
     */
    private int roundId = 1;
    /**
     * 胡牌次数
     */
    private int huCount = 0;

    public AbsMJSetRoom(int setID, MahjongRoom room, int dPos) {
        super(setID);
        this.room = room;
        this.dPos = dPos;
        this.setPosDict(new ConcurrentHashMap<>(this.room.getPlayerNum()));
        this.godInfo = new GodInfo(this, isConfigName());
        this.lastOpInfo = this.newLastOpInfo();
        this.mJinCardInfo = new MJJinCardInfo(this.kaiJinNum());
        // 初始化解散次数
        initDissolveCount();
    }

    /**
     * 最近操作的信息
     *
     * @return
     */
    public LastOpInfo newLastOpInfo() {
        return new LastOpInfo(this);
    }

    /**
     * 配置文件是否需要游戏名
     *
     * @return T:需要,F:不需要
     */
    public boolean isConfigName() {
        return false;
    }

    /**
     * 设置当局牌
     *
     * @param setCard 当局牌
     */
    public void setSetCard(AbsMJSetCard setCard) {
        this.setCard = setCard;
    }

    /**
     * 开金数
     *
     * @return
     */
    public abstract int kaiJinNum();

    /**
     * 是否白板替金
     *
     * @return
     */
    public abstract boolean isBaiBanTiJin();

    /**
     * 牌数
     *
     * @return
     */
    public abstract int cardSize();

    /**
     * 房间人数
     *
     * @return
     */
    public int getPlayerNum() {
        return this.room.getPlayerNum();
    }

    /**
     * 获取指定位置
     *
     * @param opPos
     * @return
     */
    public AbsMJSetPos getMJSetPos(int opPos) {
        return this.posDict.get(opPos);
    }

    /**
     * 检查是否存在指定消耗类型
     *
     * @return
     */
    @Override
    public boolean checkExistPrizeType(PrizeType prizeType) {
        return prizeType.equals(this.getRoom().getBaseRoomConfigure().getPrizeType());
    }

    /**
     * 获取房间回放记录
     *
     * @return
     */
    public RoomPlayBack getRoomPlayBack() {
        if (null == this.roomPlayBack) {
            this.roomPlayBack = new MJRoomPlayBackImpl(this.getRoom());
        }
        return this.roomPlayBack;
    }

    /**
     * 获取麻将牌
     *
     * @return
     */
    public AbsMJSetCard getMJSetCard() {
        return this.setCard;
    }

    /**
     * 一些基本数据初始，无需理会。
     */
    public void exeStartSet() {
        // 发牌的时候记录分数
        //    只赢当前身上分的时候要用
        this.recordRoomPosPointBeginStart();
        // this.room.cleanXiPai();
        this.room.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
    }

    /**
     * 如果是房卡类型，才需要回放记录
     */
    public void roomPlayBack() {
        // 除了房卡类型，其他的都记录回放。
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            this.getRoomPlayBack()
                    .addPlayBack(new PlayBackData(this.room.getRoomID(), this.getSetID(), this.getDPos(),
                            this.room.getCount(), this.room.getRoomKey(),
                            this.room.getBaseRoomConfigure().getGameType().getId(), getPlayBackDateTimeInfo()));
        }
    }

    /**
     * 本局结算， 计算下一局的坐庄信息
     *
     * @return
     */
    public abstract int calcNextDPos();

    /**
     * 麻将当局结算
     *
     * @return
     */
    protected BaseMJRoom_SetEnd newMJRoomSetEnd() {
        return new BaseMJRoom_SetEnd();
    }

    /**
     * 获取房间当局结束数据
     *
     * @return
     */
    protected BaseMJRoom_SetEnd mRoomSetEnd() {
        if (null != setEnd) {
            return this.setEnd;
        }
        this.setEnd = this.newMJRoomSetEnd();
        this.setEnd.setDPos(this.getDPos());
        this.setEnd.setEndTime(CommTime.nowSecond());
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            this.setEnd.setPlayBackCode(getPlayBackDateTimeInfo().getPlayBackCode());
            this.setEnd.setRoomDissolutionState(this.getRoom().getRoomDissolutionState());
        }
        this.setEnd.setSetId(this.getSetID());
        this.setEnd.setPosResultList(this.getPosDict().values().stream().map(k -> k.calcPosEnd())
                .sorted(Comparator.comparing(BaseMJRoom_PosEnd::getPos)).collect(Collectors.toList()));
        this.calcDaJuFenShuRoomPoint();
        return this.setEnd;
    }

    // 结算积分
    public void calcPoint() {
        // 计算当局每个pos位置的分数。
        this.calcCurSetPosPoint();
        // 设置最后操作时间
        this.getLastOpInfo().setLastShotTime(CommTime.nowSecond());
        // 设置所有用户的超时
        this.getRoom().getRoomPosMgr().setAllLatelyOutCardTime();
        BaseMJRoom_SetEnd setEnd = this.getNotify_setEnd();
        this.getBo().setDataJsonRes(new Gson().toJson(setEnd));
        this.getBo().setEndTime(setEnd.getEndTime());
        this.getBo().setRoomID(this.getRoom().getRoomID());
        this.getBo().setSetID(this.getSetID());
        this.getBo().setPlayBackCode(setEnd.getPlayBackCode());
        this.getBo().setTabId(this.getRoom().getTabId());
        if (this.checkExistPrizeType(PrizeType.Gold)) {
            return;
        }
        this.getBo().getBaseService().saveOrUpDate(this.getBo());
        RedisSource redisSource = ContainerMgr.get().getRedis();
        for (BaseMJRoom_PosEnd posEnd : this.setEnd.getPosResultList()) {
            String cacheName = getCacheWinName() + posEnd.getPid();
            if (getRoom().getRoomTypeEnum() == RoomTypeEnum.UNION) {
                if (redisSource.exists(cacheName)) {
                    double total = Double.parseDouble(redisSource.get(cacheName));
                    redisSource.put(cacheName, String.valueOf(total + posEnd.getSportsPoint()));
                } else {
                    redisSource.put(cacheName, String.valueOf(posEnd.getSportsPoint()));
                }
            } else {
                if (redisSource.exists(cacheName)) {
                    double total = Double.parseDouble(redisSource.get(cacheName));
                    redisSource.put(cacheName, String.valueOf(total + posEnd.getPoint()));
                } else {
                    redisSource.put(cacheName, String.valueOf(posEnd.getPoint()));
                }
            }
        }
    }

    public String getCacheWinName() {
        return "mj_player_win" + getRoom().getRoomTypeEnum().ordinal() + "_" + getRoom().getGameRoomBO().getGameType() + "_" + ((BaseCreateRoom) getRoom().getCfg()).getBeishu();
    }

    /**
     * 计算当局每个pos位置的分数。
     */
    protected abstract void calcCurSetPosPoint();

    /**
     * 其他特殊结算 连庄记录
     */
    public void calcOtherPoint() {
        // 庄家胡牌、流局，庄家坐庄
        if (this.room.getCurSetID() == this.room.getCount()) {
            return;
        }
        int evenDposCount = 0;
        if (this.getMHuInfo().isHuNotEmpty()) {
            if (this.getMHuInfo().getHuPos() == this.dPos) {
                evenDposCount = this.room.getEvenDpos() + 1;
                addLianZhuang(this.dPos, evenDposCount);
            }
        } else {
            evenDposCount = this.room.getEvenDpos() + 1;
            addLianZhuang(this.dPos, evenDposCount);
        }
    }

    /**
     * 记录玩家连庄次数
     *
     * @param pos           玩家ID
     * @param evenDposCount 连庄数
     */
    public void addLianZhuang(int pos, int evenDposCount) {
        AbsMJSetPos setPos = this.getMJSetPos(pos);
        if (null == setPos) {
            return;
        }
        setPos.setLianZhuangNum(evenDposCount);
    }

    /**
     * 通知当局信息
     *
     * @param pid 玩家PID
     */
    @Override
    public MJRoomSetInfo getNotify_set(long pid) {
        return this.getMJRoomSetInfo(pid);
    }

    /**
     * 创建新的当局麻将信息
     *
     * @return
     */
    protected MJRoomSetInfo newMJRoomSetInfo() {
        return new MJRoomSetInfo();
    }

    /**
     * 获取当局信息
     *
     * @param pid 玩家PID
     * @return
     */
    protected MJRoomSetInfo getMJRoomSetInfo(long pid) {
        // 创建新的当局麻将信息
        MJRoomSetInfo ret = this.newMJRoomSetInfo();
        ret.setSetID(this.getSetID());
        // 庄家位置
        ret.setdPos(this.dPos);
        // 骰子
        if (this.getMJSetCard() != null) {
            ret.setSaizi(this.getMJSetCard().getRandomCard().Saizi);
        }
        // 当前操作时间
        ret.setSetCurrentTime(CommTime.nowSecond());
        // 最后一次操作时间
        ret.setLastShotTime(this.getLastOpInfo().getLastShotTime());
        // 开始拿牌的位置
        ret.setStartPaiPos(this.setCard.getRandomCard().getStartPaiPos());
        // 开始拿牌的蹲位
        ret.setStartPaiDun(this.setCard.getRandomCard().getStartPaiDun());
        // 杠后摸牌
        ret.setGangMoCnt(this.setCard.getRandomCard().getGangMoCnt());
        // 正常摸牌
        ret.setNormalMoCnt(this.setCard.getRandomCard().getNormalMoCnt());
        // 最近打出的牌
        ret.setWaitReciveCard(this.getLastOpInfo().getLastOutCard());
        // 状态
        ret.setState(this.state);
        // 初始手牌
        ret.setSetPosList(setPosList(pid));
        // 如果是等待状态： waiting；
        if (SetState.Playing.equals(this.state) && null != curRound) {
            int pos = this.room.getRoomPosMgr().getPosByPid(pid).getPosID();
            ret.setSetRound(this.curRound.getNotify_RoundInfo(pos)); // 当前等待信息
            // Wait
        }
        // 结束状态
        if (SetState.End.equals(this.state)) {
            ret.setSetEnd(this.getNotify_setEnd());
        } else {
            ret.setSetEnd(this.newMJRoomSetEnd());
        }
        return ret;
    }

    public List<BaseMJSet_Pos> setPosList(long pid) {
        List<BaseMJSet_Pos> pos = new ArrayList<>();
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            pos.add(this.posDict.get(i).getNotify(pid));
        }
        return pos;
    }

    /**
     * 一局结束的信息
     *
     * @return
     */
    @Override
    public BaseMJRoom_SetEnd getNotify_setEnd() {
        // 获取房间当局结束数据
        return this.mRoomSetEnd();
    }

    /**
     * 设置位置的牌
     *
     * @param pid
     * @return
     */
    public List<BaseMJSet_Pos> setPosCard(long pid) {
        List<BaseMJSet_Pos> setPosList = new ArrayList<>();
        setPosList.addAll(this.getPosDict().values().stream().map(k -> k.getNotify(pid)).collect(Collectors.toList()));
        return setPosList;
    }

    /**
     * 发送设置位置的牌
     */
    public abstract void sendSetPosCard();

    /**
     * 麻将补花
     *
     * @param pos
     */
    public abstract void MJApplique(int pos);

    /**
     * 麻将开心操作
     *
     * @param jinCard  金1
     * @param jinCard2 金2
     */
    public abstract void kaiJinNotify(MJCard jinCard, MJCard jinCard2);

    // 每200ms更新1次 秒
    @Override
    public boolean update(int sec) {
        boolean isClose = false;

        if (this.state == SetState.Init) {
            if (CommTime.nowMS() > this.startMS + this.InitTime) {
                this.state = SetState.Playing;
                if (!this.startNewRound()) {
                    this.endSet();
                }
            }
        } else if (this.state == SetState.Playing) {
            boolean isRoundClosed = this.curRound.update(sec);
            if (isRoundClosed) {
                if (curRound.isSetHuEnd() || !this.startNewRound()) {
                    this.endSet();
                }
            }
        } else if (this.state == SetState.End) {
            this.clearEndSetRoom();
            isClose = true;

        } else if (this.state == SetState.Waiting) {
            this.opWaiting();
        }
        return isClose;
    }

    /**
     * 等待操作中
     */
    public void opWaiting() {

    }

    /**
     * 麻将金牌信息
     *
     * @return
     */
    public MJJinCardInfo getmJinCardInfo() {
        return mJinCardInfo;
    }

    /**
     * 清空结束房间当前局
     */
    public void clearEndSetRoom() {
        // 清空回合记录
        if (Objects.nonNull(this.preRound)) {
            this.preRound.clear();
            this.preRound = null;
        }
        // 清空当前回合
        if (Objects.nonNull(this.curRound)) {
            this.curRound.roundClear();
            this.curRound = null;
        }
        if (Objects.nonNull(this.setCard)) {
            this.setCard.randomCardClear();
        }
        // 房间回放
        if (Objects.nonNull(this.roomPlayBack)) {
            this.roomPlayBack.clear();
            this.roomPlayBack = null;
        }
    }

    @Override
    public void clear() {
        if (MapUtils.isNotEmpty(this.posDict)) {
            this.posDict.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    value.clear();
                }
            });
            this.posDict.clear();
            this.posDict = null;
        }

        if (Objects.nonNull(this.setCard)) {
            this.setCard.clear();
            this.setCard = null;
        }
        if (Objects.nonNull(this.godInfo)) {
            this.godInfo.clear();
        }
        if (Objects.nonNull(this.setPosMgr)) {
            this.setPosMgr.clear();
            this.setPosMgr = null;
        }
        this.clearEndSetRoom();
        if (Objects.nonNull(this.mJinCardInfo)) {
            this.mJinCardInfo.clear();
        }
        // this.setEnd = null;
        this.room = null;
    }

    @Override
    public void clearBo() {
        this.setBo(null);
    }

    /**
     * 开始发牌
     */
    public void startSet() {
        CommLogD.info("startSet id:{}", getSetID());
        // 洗底牌
        this.absMJSetCard();
        // 初始化本局位置管理器
        this.setSetPosMgr(this.absMJSetPosMgr());
        // 初始化玩家手上的牌
        this.initSetPosCard();
        // 通知本局开始
        this.notify2SetStart();
        // 一些基本数据初始，无需理会。
        exeStartSet();
    }

    /**
     * 初始化玩家手上的牌
     */
    protected final void initSetPosCard() {
        // 初始化位置
        this.initSetPos();
        if (getGodInfo().isGodCardMode()) {
            // 神牌模式下：（只允许内测时开启）
            this.initGodPosCard();
        } else {
            // 正常模式下：上线模式
            // 初始玩家身上的牌
            this.initPosCard();
        }
    }

    /**
     * 初始化位置
     */
    protected void initSetPos() {
        for (int posId = 0; posId < this.getRoom().getPlayerNum(); posId++) {
            this.getPosDict().put(posId, this.absMJSetPos(posId));
        }
    }

    /**
     * 初始化神牌玩家身上的牌
     */
    protected void initGodPosCard() {
        // 神牌模式
        this.getPosDict().values().stream().forEach(k -> this.getGodInfo().isGodCard(k, k.getPosID()));
        // 神牌模式下补牌
        this.getGodInfo().godCardPrivate();
        // 初始刷新手牌和可胡列表
        this.getPosDict().values().stream().forEach(k -> k.initSortCardsAndCalcHuFan());
    }

    /**
     * 初始玩家身上的牌
     */
    protected void initPosCard() {
        // 开始牌位置
        int startPaiPos = this.getMJSetCard().getRandomCard().getStartPaiPos();
        // 玩家初始发牌
        for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
            int posId = (startPaiPos + i) % this.getRoom().getPlayerNum();
            this.getPosDict().get(posId).init(this.getSetCard().popList(this.cardSize(), posId));
        }

    }

    /**
     * 通知本局开始
     */
    protected final void notify2SetStart() {
        business.global.replay.LegacyReplayParticipantRegistry.getInstance().grantAll(
                this.room.getRoomID(), this.room.getCurSetID(), this.getPosDict().values().stream()
                        .collect(Collectors.toMap(k -> k.getPid(), k -> k.getPosID(), (left, right) -> left)));
        this.getPosDict().values().forEach(k -> {
            BaseSendMsg baseSendMsg = this.setStart(this.room.getRoomID(), this.getNotify_set(k.getPid()));
            if (k.getPosID() == 0) {
                this.getRoomPlayBack().playBack2Pos(k.getPosID(), baseSendMsg, setPosMgr.getAllPlayBackNotify());
            } else {
                this.room.getRoomPosMgr().notify2Pos(k.getPosID(), baseSendMsg);
            }
        });
    }

    /**
     * 玩家位置信息
     *
     * @param posID
     * @return
     */
    protected abstract AbsMJSetPos absMJSetPos(int posID);

    /**
     * 本局牌管理
     *
     * @return
     */
    protected abstract void absMJSetCard();

    /**
     * 本局玩家操作管理
     *
     * @return
     */
    protected abstract AbsMJSetPosMgr absMJSetPosMgr();

    /**
     * 牌局开始消息通知
     *
     * @param roomID
     * @param setInfo
     * @return
     */
    protected abstract <T> BaseSendMsg setStart(long roomID, T setInfo);

    /**
     * 下回合操作位置
     *
     * @param roundID
     * @return
     */
    protected abstract AbsMJSetRound nextSetRound(int roundID);

    /**
     * 开启新的回合
     *
     * @return
     */
    public boolean startNewRound() {
        // 记录当前回合
        this.recordCurRound();
        // 开启新的回合
        this.setCurRound(this.nextSetRound(this.getRoundId()));
        // 尝试开始回合, 如果失败，则set结束
        return this.getCurRound().tryStartRound();
    }

    /**
     * 记录当前回合
     */
    private void recordCurRound() {
        if (null != this.getCurRound()) {
            // 将当前回合设置旧回合
            this.setPreRound(this.getCurRound());
            // 回合数+=1
            this.setRoundId(this.getRoundId() + 1);
            // 清空当前回合
            this.setCurRound(null);
        }
    }

    /**
     * 摸牌
     *
     * @param opPos
     * @param isNormalMo
     * @return
     */
    public MJCard getCard(int opPos, boolean isNormalMo) {
        AbsMJSetPos setPos = this.posDict.get(opPos);
        // 随机摸牌
        MJCard card = this.setCard.pop(isNormalMo, this.getGodInfo().godHandCard(setPos));
        if (Objects.isNull(card)) {
            // 黄庄位置
            this.getMHuInfo().setHuangPos(opPos);
            return null;
        }
        // 设置牌
        setPos.getCard(card);
        // 通知房间内的所有玩家，指定玩家摸牌了。
        this.notify2GetCard(setPos);
        return card;
    }

    /**
     * 通知房间内的所有玩家，指定玩家摸牌了。
     *
     * @param setPos 操作玩家信息
     */
    protected void notify2GetCard(AbsMJSetPos setPos) {
        BaseMJSet_Pos posInfoOther = setPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = setPos.getNotify(true);
        this.roomPlayBack.playBack2Pos(setPos.getPosID(),
                this.posGetCard(this.room.getRoomID(), setPos.getPosID(), this.setCard.getRandomCard().getNormalMoCnt(),
                        this.setCard.getRandomCard().getGangMoCnt(), posInfoSelf),
                this.setPosMgr.getAllPlayBackNotify());
        this.room.getRoomPosMgr().notify2ExcludePosID(setPos.getPosID(),
                this.posGetCard(this.room.getRoomID(), setPos.getPosID(), this.setCard.getRandomCard().getNormalMoCnt(),
                        this.setCard.getRandomCard().getGangMoCnt(), posInfoOther));
    }

    /**
     * 摸牌消息
     *
     * @param roomID
     * @param pos
     * @param normalMoCnt
     * @param gangMoCnt
     * @param set_Pos
     * @return
     */
    protected abstract <T> BaseSendMsg posGetCard(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos);

    // 一局结束，是否流局
    @Override
    public void endSet() {
        CommLogD.info("endSet id:{}", getSetID());
        if (this.state == SetState.End) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState())
                    && business.global.billing.LegacySettlementGuard.beginDissolutionAdjustment(
                    this.room.getRoomID(), this.getSetID(), "mahjong-common-v1")) {
                this.calcDaJuFenShuRoomPoint();
            }
            return;
        }
        if (!business.global.billing.LegacySettlementGuard.beginRound(
                this.room.getRoomID(), this.getSetID(), "mahjong-common-v1")) {
            return;
        }
        this.state = SetState.End;
        setEnd(true);
        // 结算算分
        this.calcPoint();
        // 广播
        this.getRoomPlayBack().playBack2All(this.setEnd(room.getRoomID(), this.getNotify_setEnd()));
        // 小局托管自动解散
        this.setTrusteeshipAutoDissolution();
        // 记录回放码
        this.roomPlayBack();

    }

    /**
     * 小局结算消息
     *
     * @param roomID
     * @param setEnd
     * @return
     */
    protected abstract <T> BaseSendMsg setEnd(long roomID, T setEnd);

    /**
     * 回放记录谁发起解散
     */
    @Override
    public void addDissolveRoom(BaseSendMsg baseSendMsg) {
        if (SetState.End.equals(this.getState()) || null == this.getSetPosMgr()) {
            return;
        }
        this.getRoomPlayBack().addPlaybackList(baseSendMsg, this.getSetPosMgr().getAllPlayBackNotify());
    }

    public void setHuangPos(int huangPos) {
        this.mHuInfo.setHuangPos(huangPos);
    }

    /**
     * 房间是否结束
     *
     * @return
     */
    public boolean isRoomEnd() {
        boolean roomEnd = false;
        if (this.room.getDissolveRoom() != null) {
            roomEnd = true;
        } else if (this.getRoom().getCurSetID() >= this.getRoom().getCount()) {
            roomEnd = true;
        }
        return roomEnd;
    }

    /**
     * 结算一课
     *
     * @param isYiKe     是否是一课玩法
     * @param limitScore 一课分数上限
     */
    public void calYiKaoPoint(boolean isYiKe, int limitScore) {
        // 各个位置的算分情况map
        Map<Integer, Integer> pointMap = new HashMap<>();
        // 各个位置是否进入讨钱状态map
        Map<Integer, Boolean> pointFlag = new HashMap<>();
        // 初始化一科计算分数所需要的map
        this.getPosDict().values().stream().forEach(k -> {
            pointMap.put(k.getPosID(), k.getDeductPoint());
            pointFlag.put(k.getPosID(), false);
        });
        // 一科计算的分数
        if (isYiKe && this.getMHuInfo().getHuPos() >= 0) {
            for (int i = 0; i < this.getPlayerNum(); i++) {
                int nextPos = (this.getMHuInfo().getHuPos() + i) % this.getPlayerNum();
                AbsMJSetPos setPos = this.getPosDict().get(nextPos);
                Integer oldPoint = setPos.getDeductPoint();
                if (oldPoint < 0)
                    continue;// 如果玩家输钱 不进行循环讨钱
                setPos.setDeductPoint(0);

                for (int j = 1; j < this.getPlayerNum(); j++) {
                    int nextPos2 = (nextPos + j) % this.getPlayerNum();
                    AbsMJSetPos setPos2 = this.getPosDict().get(nextPos2);
                    int tempValue = setPos2.getDeductPoint();
                    if (tempValue > 0)
                        continue;// 如果被讨的人是赢钱的 则不进行被讨钱
                    if (Math.abs(tempValue) >= Math.abs(pointMap.get(nextPos2).intValue()) && pointFlag.get(nextPos2))
                        continue;
                    boolean subFlag = false;
                    // 初始化标志 判断是当前玩家是否是第一次被讨钱
                    if (!pointFlag.get(nextPos2)) {
                        subFlag = true;
                        setPos2.setDeductPoint(0);
                        pointFlag.put(nextPos2, true);
                    }
                    // 符号转换
                    tempValue = subFlag ? Math.abs(tempValue)
                            : Math.abs(pointMap.get(nextPos2).intValue()) - Math.abs(tempValue);
                    // 一科分数限制
                    if (setPos2.getRoomPos().getPoint() + limitScore - tempValue < 0) {
                        tempValue = setPos2.getRoomPos().getPoint() + limitScore;
                    }
                    // 如果输的钱大于要讨的钱
                    if (tempValue > oldPoint) {
                        tempValue = oldPoint;
                        setPos.setDeductPoint(setPos.getDeductPoint() + tempValue);
                        setPos2.setDeductPoint(setPos2.getDeductPoint() - tempValue);
                        break;// 讨钱满足 到下一个人进行讨钱
                    } else {
                        // 如果输的钱小于要讨的钱
                        oldPoint -= tempValue;
                        setPos.setDeductPoint(setPos.getDeductPoint() + tempValue);
                        setPos2.setDeductPoint(setPos2.getDeductPoint() - tempValue);
                    }
                }
            }
        }
    }

    /**
     * 检查小局托管自动解散
     */
    public boolean checkSetEndTrusteeshipAutoDissolution() {
        return false;
    }

    /**
     * 小局托管自动解散回放记录 注意：需要自己重写
     *
     * @param roomId  房间id
     * @param pidList 托管玩家Pid
     * @param sec     记录时间
     * @return
     */
    public BaseSendMsg DissolveTrusteeship(long roomId, List<Long> pidList, int sec) {
        return null;
    }

    /**
     * 小局托管自动解散
     */
    public void setTrusteeshipAutoDissolution() {
        // 检查小局托管自动解散
        if (checkSetEndTrusteeshipAutoDissolution()) {
            // 获取托管玩家pid列表
            List<Long> trusteeshipPlayerList = getRoom().getRoomPosMgr().getRoomPosList().stream()
                    .filter(n -> n.isTrusteeship()).map(n -> n.getPid()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(trusteeshipPlayerList)) {
                // 记录回放中
                getRoomPlayBack().addPlaybackList(
                        DissolveTrusteeship(this.getRoom().getRoomID(), trusteeshipPlayerList, CommTime.nowSecond()),
                        null);
                getRoom().setTrusteeshipDissolve(true);
            }
        }
    }

    /**
     * 设置解散次数
     */
    public void initDissolveCount() {
        room.getRoomPosMgr().posList.stream().forEach(n -> n.setDissolveCount(0));
    }

    /**
     * 标识Id
     *
     * @return
     */
    @Override
    public int getTabId() {
        return this.getRoom().getTabId();
    }

    /**
     * 发牌，不通知setStart
     */
    public void startNSet() {
        CommLogD.info("startSet id:{}", getSetID());
        // 洗底牌
        this.absMJSetCard();
        // 初始化本局位置管理器
        this.setSetPosMgr(this.absMJSetPosMgr());
        // 初始化玩家手上的牌
        this.initSetPosCard();
    }

    /**
     * 通知摸牌
     *
     * @param setPos
     */
    public void notifyGetCard(AbsMJSetPos setPos) {
        int opPos = setPos.getPosID();
        BaseMJSet_Pos posInfoOther = setPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = setPos.getNotify(true);
        // 通知房间内的所有玩家，指定玩家摸牌了。
        this.roomPlayBack.playBack2Pos(opPos,
                this.posGetCard(this.room.getRoomID(), opPos, this.setCard.getRandomCard().getNormalMoCnt(),
                        this.setCard.getRandomCard().getGangMoCnt(), posInfoSelf),
                this.setPosMgr.getAllPlayBackNotify());
        this.room.getRoomPosMgr().notify2ExcludePosID(opPos,
                this.posGetCard(this.room.getRoomID(), opPos, this.setCard.getRandomCard().getNormalMoCnt(),
                        this.setCard.getRandomCard().getGangMoCnt(), posInfoOther));
    }


    /**
     * 计算大局分数
     */
    @Override
    public void calcDaJuFenShuRoomPoint() {
        if (this.getRoom().isGuDingSuanFen()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                List<Integer> winPosList = this.getPosDict().values().stream().filter(y -> y.getRoomPos().getPoint() > 0).map(MJSetPos::getPosID).collect(Collectors.toList());
                List<Integer> losePosList = this.getPosDict().values().stream().filter(y -> y.getRoomPos().getPoint() < 0).map(MJSetPos::getPosID).collect(Collectors.toList());
                // 各个位置的算分情况map
                double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
                double suanFenNum = getRoom().getSuanFenNum();
                double losePoint = CommMath.mul(suanFenNum, beiShu);
                if (CollectionUtils.isNotEmpty(winPosList) && CollectionUtils.isNotEmpty(losePosList))
                    this.getPosDict().values().forEach(y -> {
                        y.getRoomPos().setPoint(0);
                        if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                            y.getRoomPos().setPointYiKao(0D);
                            y.getResults().setSportsPoint(0D);
                        }
                    });
                if (CollectionUtils.isNotEmpty(winPosList)) {
                    winPosList.forEach(i -> {
                        AbsMJSetPos setPos = this.getPosDict().get(i);
                        losePosList.forEach(j -> {
                            setPos.getRoomPos().setPoint(setPos.getRoomPos().getPoint() + (int) suanFenNum);
                            if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                                setPos.getRoomPos().setPointYiKao(CommMath.addDouble(setPos.getRoomPos().sportsPoint(), losePoint));
                            }
//                            setPos.getResults().setPoint(setPos.getRoomPos().getPoint());
                            if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                                setPos.getResults().setSportsPoint(setPos.getRoomPos().sportsPoint());
                            }
                            AbsMJSetPos loseAbsMjSetPos = this.getMJSetPos(j);
                            loseAbsMjSetPos.getRoomPos().setPoint(loseAbsMjSetPos.getRoomPos().getPoint() - (int) suanFenNum);
                            if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                                loseAbsMjSetPos.getRoomPos().setPointYiKao(CommMath.subDouble(loseAbsMjSetPos.getRoomPos().sportsPoint(), losePoint));
                            }
//                            loseAbsMjSetPos.getResults().setPoint(loseAbsMjSetPos.getRoomPos().getPoint());
                            if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                                loseAbsMjSetPos.getResults().setSportsPoint(loseAbsMjSetPos.getRoomPos().sportsPoint());
                            }
                        });
                    });
                }
                return;
            }
        }
        if (this.getRoom().checkDaJuFenShu()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                for (int i = 0; i < this.getPlayerNum(); i++) {
                    AbsMJSetPos setPos = this.getPosDict().get(i);
                    //最少要赢10分
                    Double roomPoint = setPos.getRoomPos().sportsPoint();
                    if (roomPoint > -10 && roomPoint < 0) {
                        Double addPoint;
                        if (RoomEndPointEnum.RoomEndPointEnum_Ten_Enough.equals(RoomEndPointEnum.valueOf(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getDajusuanfen()))) {
                            addPoint = new Double(CommMath.subDouble(10, Math.abs(roomPoint)));
                        } else {
                            addPoint = new Double(Math.abs(roomPoint));
                        }
                        if (room.isRulesOfCanNotBelowZero()) {
                            if (setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                                return;
                            }
                            addPoint = setPos.getRoomPos().getRoomSportsPoint() >= addPoint ? addPoint : setPos.getRoomPos().getRoomSportsPoint();
                        }
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(setPos.getRoomPos().sportsPoint(), addPoint));
                        setPos.getRoomPos().setRoomSportsPoint(CommMath.subDouble(setPos.getRoomPos().getRoomSportsPoint(), addPoint));
                        setPos.getResults().setSportsPoint(setPos.getRoomPos().sportsPoint());
                        int subPosID = (setPos.getPosID() + 1) % getRoom().getPlayerNum();
                        AbsMJSetPos addAbsMjSetPos = this.getMJSetPos(subPosID);
                        addAbsMjSetPos.getRoomPos().setPointYiKao(CommMath.addDouble(addAbsMjSetPos.getRoomPos().sportsPoint(), addPoint));
                        addAbsMjSetPos.getResults().setSportsPoint(addAbsMjSetPos.getRoomPos().sportsPoint());
                        addAbsMjSetPos.getRoomPos().setRoomSportsPoint(CommMath.addDouble(addAbsMjSetPos.getRoomPos().getRoomSportsPoint(), addPoint));


                    }
                }
            }
        }
        if (this.getRoom().checkTakeLose()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                if (!this.getRoom().checkDaJuFenShu()) {
                    for (int i = 0; i < this.getPlayerNum(); i++) {
                        AbsMJSetPos setPos = this.getPosDict().get(i);
                        //坐下的值
                        Double seatSportsPoint = setPos.getRoomPos().getCalcSportPoint();
                        //结束值
                        Double roomPoint = CommMath.addDouble(setPos.getRoomPos().getRoomSportsPoint(), setPos.getRoomPos().getOtherSportsPointConsume());
                        //竞技点变化值
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(roomPoint, seatSportsPoint));
                    }
                }
                for (int i = 0; i < this.getPlayerNum(); i++) {
                    AbsMJSetPos setPos = this.getPosDict().get(i);
                    //坐下的值
                    Double seatSportsPoint = setPos.getRoomPos().getCalcSportPoint();
                    //结束值
                    Double roomPoint = CommMath.addDouble(setPos.getRoomPos().getRoomSportsPoint(), setPos.getRoomPos().getOtherSportsPointConsume());
                    if (roomPoint > seatSportsPoint * 2) {
                        Double subPoint;
                        if (seatSportsPoint <= 0) {
                            subPoint = new Double(Math.abs(CommMath.subDouble(seatSportsPoint, roomPoint)));
                        } else {
                            subPoint = new Double(Math.abs(CommMath.subDouble(roomPoint, CommMath.mul(seatSportsPoint, 2))));//5
                        }
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(setPos.getRoomPos().getPointYiKao(), subPoint));
                        setPos.getResults().setSportsPoint(setPos.getRoomPos().sportsPoint());
                        int subPosID = (setPos.getPosID() + 1) % getRoom().getPlayerNum();
                        AbsMJSetPos addAbsMjSetPos = this.getMJSetPos(subPosID);
                        addAbsMjSetPos.getRoomPos().setPointYiKao(CommMath.addDouble(addAbsMjSetPos.getRoomPos().getPointYiKao(), subPoint));
                        addAbsMjSetPos.getResults().setSportsPoint(addAbsMjSetPos.getRoomPos().sportsPoint());
                    }
                }
            }
        }
    }


//    /**
//     * 亲友圈竞技点不能输到0分
//     */
//    @SuppressWarnings("Duplicates")
//    public void calYiKaoPoint() {
//        // 各个位置的算分情况map
//        double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
//        //各个位置的输分情况
//        Map<Integer, Double> pointMap = new HashMap<>();
//        //各个位置的最多能输多少分
//        Map<Integer, Double> sportPointMap = new HashMap<>();
//        if(!(room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()))){
//            //身上多少分赢多少分
//            // 初始化一科计算分数所需要的map
//            this.getPosDict().values().forEach(k -> {
//                k.setDeductPointYiKao(CommMath.mul(k.getDeductPoint(), beiShu));
//            });
//        }
//        // 初始化一科计算分数所需要的map
//        this.getPosDict().values().forEach(k -> {
//            pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint(), beiShu));
//            sportPointMap.put(k.getPosID(), k.getRoomPos().getRoomSportsPoint());
//        });
//        // 亲友圈竞技点不能输到0分
//        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && this.getMHuInfo().getHuPos() >= 0) {
//            Double totalWinPoint = 0D;//输的总分
//            for (int i = 0; i < this.getPlayerNum(); i++) {
//                AbsMJSetPos setPos = this.getPosDict().get(i);
//
//                Double losePoint = setPos.getDeductPointYiKao();//-12
//                if (losePoint <= 0) {
//                    //如果这个玩家本身没有分数可以扣 设置为0  继续下一个玩家计算
//                    if (sportPointMap.get(setPos.getPosID()) <= 0) {
//                        setPos.setDeductPointYiKao(0);
//                        continue;
//                    }
//                    // 一科分数限制 10
//                    if (sportPointMap.get(setPos.getPosID()) - Math.abs(losePoint) < 0) {
//                        //输的分数为-的
//                        losePoint = (-sportPointMap.get(setPos.getPosID()));
//                        setPos.setDeductPointYiKao(losePoint);
//                    }
//                    totalWinPoint += Math.abs(losePoint);
//                }
//            }
//            for (int i = 0; i < this.getPlayerNum(); i++) {
//                AbsMJSetPos setPos = this.getPosDict().get(i);
//                Double winPoint = setPos.getDeductPointYiKao();//12
//                if (totalWinPoint <= 0) {
//                    for (int j = i; j < this.getPlayerNum(); j++) {
//                        AbsMJSetPos setPos2 = this.getPosDict().get(j);
//                        if (setPos2.getDeductPointYiKao() >= 0) {
//                            setPos2.setDeductPointYiKao(0);
//                        }
//                    }
//                    break;
//                }
//                if (winPoint > 0) {
//                    if (totalWinPoint < winPoint) {
//                        setPos.setDeductPointYiKao(totalWinPoint);
//                        totalWinPoint -= totalWinPoint;
//                    } else {
//                        totalWinPoint -= winPoint;
//                    }
//                }
//            }
//        }
//
//    }

    /**
     * 亲友圈竞技点不能输到0分
     */
    @SuppressWarnings("Duplicates")
    public void calYiKaoPoint() {
        // 各个位置的算分情况map
        double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());

        //各个位置的输分情况
        Map<Integer, Double> pointMap = new HashMap<>();
        //各个位置的最多能输多少分
        Map<Integer, Double> sportPointMap = new HashMap<>();
        if (!(room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()))) {
            //身上多少分赢多少分
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                k.setDeductPointYiKao(CommMath.mul(k.getDeductPoint(), beiShu));
            });
        }
        // 初始化一科计算分数所需要的map
        this.getPosDict().values().forEach(k -> {
            pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint(), beiShu));
            sportPointMap.put(k.getPosID(), k.getRoomPos().getRoomSportsPoint());
        });
        // 亲友圈竞技点不能输到0分
        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && this.getMHuInfo().getHuPos() >= 0) {
            Double totalWinPoint = 0D;//输的总分
            for (int i = 0; i < this.getPlayerNum(); i++) {
                AbsMJSetPos setPos = this.getPosDict().get(i);

                Double losePoint = setPos.getDeductPointYiKao();//-12
                if (losePoint <= 0) {
                    //如果这个玩家本身没有分数可以扣 设置为0  继续下一个玩家计算
                    if (sportPointMap.get(setPos.getPosID()) <= 0) {
                        setPos.setDeductPointYiKao(0);
                        continue;
                    }
                    // 一科分数限制 10
                    if (sportPointMap.get(setPos.getPosID()) - Math.abs(losePoint) < 0) {
                        //输的分数为-的
                        losePoint = CommMath.mul(sportPointMap.get(setPos.getPosID()), -1);
                        setPos.setDeductPointYiKao(losePoint);
                    }
                    totalWinPoint = CommMath.addDouble(totalWinPoint, Math.abs(losePoint));
                }
            }
            for (int i = 0; i < this.getPlayerNum(); i++) {
                AbsMJSetPos setPos = this.getPosDict().get(i);
                Double winPoint = setPos.getDeductPointYiKao();//12
                if (totalWinPoint <= 0) {
                    for (int j = i; j < this.getPlayerNum(); j++) {
                        AbsMJSetPos setPos2 = this.getPosDict().get(j);
                        if (setPos2.getDeductPointYiKao() >= 0) {
                            setPos2.setDeductPointYiKao(0);
                        }
                    }
                    break;
                }
                if (winPoint > 0) {
                    if (totalWinPoint < winPoint) {
                        setPos.setDeductPointYiKao(totalWinPoint);
                        totalWinPoint = CommMath.subDouble(totalWinPoint, totalWinPoint);
                    } else {
                        totalWinPoint = CommMath.subDouble(totalWinPoint, winPoint);
                    }
                }
            }

        }
    }

    /**
     * 进园子，最多输到0分
     */
    @SuppressWarnings("Duplicates")
    public void calJinYuanZiPoint(int startPos) {
        // 进园子身上的分不能输到0分以下
        if (this.getMHuInfo().getHuPos() >= 0) {
            int totalWinPoint = 0;//输的总分
            for (int i = 0; i < this.getPlayerNum(); i++) {
                AbsMJSetPos setPos = this.getPosDict().get(i);
                int losePoint = setPos.getDeductPoint();
                if (losePoint <= 0) {
                    //如果这个玩家本身没有分数可以扣 设置为0  继续下一个玩家计算
                    if (setPos.getRoomPos().getPoint() <= 0) {
                        setPos.setDeductPoint(0);
                        continue;
                    }
                    // 一科分数限制 10
                    if (setPos.getRoomPos().getPoint() - Math.abs(losePoint) < 0) {
                        //输的分数为-的
                        losePoint = (-setPos.getRoomPos().getPoint());
                        setPos.setDeductPoint(losePoint);
                    }
                    totalWinPoint += Math.abs(losePoint);
                }
            }
            for (int i = 0; i < this.getPlayerNum(); i++) {
                int pos = (startPos + i) % getPlayerNum();
                AbsMJSetPos setPos = this.getPosDict().get(pos);
                int winPoint = setPos.getDeductPoint();
                if (totalWinPoint <= 0) {
                    for (int j = i; j < this.getPlayerNum(); j++) {
                        AbsMJSetPos setPos2 = this.getPosDict().get(j);
                        if (setPos2.getDeductPoint() >= 0) {
                            setPos2.setDeductPoint(0);
                        }
                    }
                    break;
                }
                if (winPoint > 0) {
                    if (totalWinPoint < winPoint) {
                        setPos.setDeductPoint(totalWinPoint);
                        totalWinPoint = 0;
                    } else {
                        totalWinPoint -= winPoint;
                    }
                }
            }
        }
    }

    /**
     * 是否能吃碰，吃副打副
     *
     * @param chis    气
     * @param mSetPos m集pos
     * @param type    类型
     * @return boolean
     */
    public boolean isCanChiPeng(List<Integer> chis, AbsMJSetPos mSetPos, Integer... type) {
        return true;
    }

    /**
     * 亲友圈竞技点不能输到0分
     */
    @SuppressWarnings("Duplicates")
    public void onlyWinRightNowPoint() {
        // 各个位置的算分情况map
        double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
        //临时存储玩家最多输赢分
        Map<Integer, Double> deductPointTemp = new HashMap<>();
        //最后玩家应该输赢的竞技点
        Map<Integer, Double> resultPointTemp = new HashMap<>();
        //各个位置的输分情况
        Map<Integer, Double> pointMap = new HashMap<>();
        //各个位置的最多能赢多少分
        Map<Integer, Double> sportPointMap = new HashMap<>();
        //不同的情况 初始化的数据来源不同
        int huPos = this.getMHuInfo().getHuPos();
        if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            //身上多少分赢多少分
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                k.setDeductPointYiKao(CommMath.mul(k.getDeductPoint(), beiShu));
            });
        }
        //不同的情况 初始化的数据来源不同
        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) && huPos >= 0) {
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                pointMap.put(k.getPosID(), k.getDeductPointYiKao());
                sportPointMap.put(k.getPosID(), k.getRoomPos().getGameBeginSportsPoint());
                //更新临时存储值
                deductPointTemp.put(k.getPosID(), k.getDeductPointYiKao());
                if (k.getDeductPointYiKao() > 0) {
                    resultPointTemp.put(k.getPosID(), k.getDeductPointYiKao());
                } else {
                    resultPointTemp.put(k.getPosID(), 0D);
                }
            });
        } else if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) && huPos >= 0) {
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint(), beiShu));
                sportPointMap.put(k.getPosID(), k.getRoomPos().getGameBeginSportsPoint());
                //更新临时存储值
                deductPointTemp.put(k.getPosID(), k.getDeductPointYiKao());
                if (k.getDeductPointYiKao() > 0) {
                    resultPointTemp.put(k.getPosID(), k.getDeductPointYiKao());
                } else {
                    resultPointTemp.put(k.getPosID(), 0D);
                }
            });
        }

        //如果全部都在正常范围，按正常算分
        boolean allMatch = this.getPosDict().entrySet().stream().allMatch(i -> {
            AbsMJSetPos setPos = i.getValue();
            Double losePoint = setPos.getDeductPointYiKao();
            return setPos.getRoomPos().getGameBeginSportsPoint() < 0 || CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint()) <= 0D;
        });
        if (allMatch) {
            return;
        }

        boolean isFirst = false;
        // 只能赢自己身上的分数
        if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && this.getMHuInfo().getHuPos() >= 0) {
            for (int i = 0; i < this.getPlayerNum(); i++) {
                AbsMJSetPos setPos = this.getPosDict().get(i);
                Double losePoint = setPos.getDeductPointYiKao();
                //没有赢分的话 不进行考虑
                if (losePoint <= 0) {
                    continue;
                }
                //需要扣减的分数
                Double needSubPoint = CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint());
                //有需要扣减的话进行扣减
                if (needSubPoint > 0D) {
                    Map<Integer, Double> resultMap = this.subPointByOnlyWinRightNowPoint(deductPointTemp, setPos, isFirst, !isFirst ? this.getExceptOneOtherPoint() : 0);
                    for (int j = 0; j < this.getPosDict().size(); j++) {
                        if (resultMap.get(j) < 0) {
                            //分数进行增减
                            deductPointTemp.put(j, CommMath.addDouble(deductPointTemp.get(j), Math.abs(resultMap.get(j))));
                            resultPointTemp.put(j, CommMath.addDouble(resultPointTemp.get(j), resultMap.get(j)));
                        } else {
                            if (j == setPos.getPosID()) {
                                resultPointTemp.put(j, resultMap.get(j));
                            }
                        }
                    }
                    isFirst = true;
                }
            }
            for (int j = 0; j < this.getPosDict().size(); j++) {
                this.getPosDict().get(j).setDeductPointYiKao(resultPointTemp.get(j));
            }
        }
    }

    /**
     * @param setPos  多赢的那个人
     * @param isFirst
     */
    private Map<Integer, Double> subPointByOnlyWinRightNowPoint(Map<Integer, Double> temp, AbsMJSetPos setPos, boolean isFirst, Double otherPoint) {
        Map<Integer, Double> resultMap = new HashMap<>(temp);
//        for (int i = 0; i < this.getPlayerNum(); i++) {
//            AbsMJSetPos absMJSetPos = this.getPosDict().get(i);
//            resultMap.put(absMJSetPos.getPosID(), absMJSetPos.getDeductPointYiKao());
//        }
        //传递参数使用
        Map<Integer, Double> resultMap2 = new HashMap<>(resultMap);

        Long newPlayerNum = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        Double beginGamePoint = CommMath.addDouble(setPos.getRoomPos().getGameBeginSportsPoint(), otherPoint);
        Double newMin = CommMath.div(-beginGamePoint, newPlayerNum.intValue());
        Double minValue = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).
                sorted(Comparator.comparing(k -> -k.getValue())).map(k -> k.getValue()).findFirst().orElse(0D);
        Map<Integer, Double> calcMap = this.diGuiMethod(beginGamePoint, resultMap2, minValue, newMin);

        Double addDouble = 0D;
        for (Map.Entry<Integer, Double> con : calcMap.entrySet()) {
            if (con.getValue() > 0) {
                if (con.getKey() == setPos.getPosID()) {
                    resultMap.put(con.getKey(), CommMath.subDouble(beginGamePoint, otherPoint));
                }
            }
            if (con.getValue() == 0) {
                addDouble = CommMath.addDouble(addDouble, Math.abs(resultMap.get(con.getKey())));
            }
        }
        long count = calcMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        //计算除后余不进的值，加输分最高的第一个人（例：剩10，平均3份。每人3.33，还差0.1补给第一分最低的人）
        //剩余的份数10
        double levelNum = CommMath.subDouble(beginGamePoint, addDouble);
        //平均每人3.33
        double everyCount = CommMath.div(levelNum, count);
        //还差0.01
        double difference = CommMath.subDouble(levelNum, CommMath.mul(everyCount, count));
        for (Map.Entry<Integer, Double> con : calcMap.entrySet()) {
            if (con.getValue() < 0) {
                double point = -everyCount;
                if (difference != 0) {
                    point = CommMath.addDouble(point, -difference);
                    difference = 0;
                }
                resultMap.put(con.getKey(), point);
            }
        }
        return resultMap;
//        for (int i = 0; i < this.getPlayerNum(); i++) {
//            AbsMJSetPos absMJSetPos = this.getPosDict().get(i);
//            if(resultMap.get(i)<0){
//                if(!isFirst){
//                    absMJSetPos.setDeductPointYiKao(resultMap.get(i));
//                }else{
//                    absMJSetPos.setDeductPointYiKao(CommMath.addDouble(absMJSetPos.getDeductPointYiKao(),resultMap.get(i)));
//                }
//            }else{
//                absMJSetPos.setDeductPointYiKao(resultMap.get(i));
//            }
//        }


        //todo 从小到大一个个扣的算法
//        Double setPosSubPoint=new Double(needSubPoint);
//        //输多少
//        Long loseNum=this.getPosDict().entrySet().stream().filter(k->k.getValue().getDeductPointYiKao()<0).count();
//        //平均扣分
//        Double avgrPoint=CommMath.div(-setPos.getRoomPos().getGameBeginSportsPoint(),loseNum);
//        List<Integer> losePos=this.getPosDict().entrySet().stream().filter(k->k.getValue().getDeductPointYiKao()<0).
//                sorted(Comparator.comparing(k->-k.getValue().getDeductPointYiKao())).
//                map(k->k.getValue().getPosID()).collect(Collectors.toList());
//        for (int i = 0; i < losePos.size(); i++) {
//            Integer calcPos=losePos.get(i);
//            AbsMJSetPos addPos = this.getPosDict().get(calcPos);
//            //玩家身上扣分
//            Double endPoint = addPos.getDeductPointYiKao();
//            //没有分可以加了
//            if(needSubPoint==0D){
//                break;
//            }
//            //赢的身上不进行补分
//            if(endPoint>0||i==setPos.getPosID()){
//                continue;
//            }
//            //如果输的分数小于分当的平均分 则不考虑
//            if(endPoint>=avgrPoint){
//                continue;
//            }
//            //小于的话 则要进行加分
//            //加的分等于
//            Double needAdd=CommMath.subDouble(avgrPoint,endPoint);
//            //如果超过了 就取剩下的
//            if(needAdd>=needSubPoint){
//                needAdd=needSubPoint;
//            }
//            needSubPoint=CommMath.subDouble(needSubPoint,needAdd);
//            addPos.setDeductPointYiKao(CommMath.addDouble(addPos.getDeductPointYiKao(),needAdd));
//        }
//        //如果有全部扣完 赢的人减去对应的分数
//        if(needSubPoint==0D){
//            setPos.setDeductPointYiKao(CommMath.subDouble(setPos.getDeductPointYiKao(),setPosSubPoint));
//        }

    }

    /**
     * 获取除了某个玩家外 其他玩家 分数超过带多少赢的
     *
     * @return
     */
    private Double getExceptOneOtherPoint() {
        Double totalValue = new Double(0D);
        for (int i = 0; i < this.getPlayerNum(); i++) {
            AbsMJSetPos absMJSetPos = this.getPosDict().get(i);
            if (absMJSetPos.getDeductPointYiKao() <= 0 ||
                    absMJSetPos.getDeductPointYiKao() > absMJSetPos.getRoomPos().getGameBeginSportsPoint()) {
                continue;
            }
            totalValue = CommMath.addDouble(totalValue, absMJSetPos.getDeductPointYiKao());
        }
        return totalValue;
    }

    /**
     * 递归方法
     *
     * @param beginGamePoint
     * @param resultMap
     * @param minValue
     * @return
     */
    private Map<Integer, Double> diGuiMethod(double beginGamePoint, Map<Integer, Double> resultMap, double minValue, double avgrPoint) {
        Long belowZero = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        boolean allBeolo = resultMap.entrySet().stream().allMatch(k -> k.getValue() >= 0 || (k.getValue() <= avgrPoint));

        if (belowZero.intValue() == 1 || allBeolo) {
            return resultMap;
        }

        for (Map.Entry<Integer, Double> con : resultMap.entrySet()) {
            if (con.getValue() >= 0) {
                continue;
            }
            resultMap.put(con.getKey(), CommMath.addDouble(con.getValue(), Math.abs(minValue)));
            beginGamePoint = CommMath.subDouble(beginGamePoint, Math.abs(minValue));
        }
        Long newPlayerNum = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        Double newMin = CommMath.div(-beginGamePoint, newPlayerNum.intValue());
        minValue = resultMap.entrySet().stream().filter(k -> k.getValue() > newMin && k.getValue() < 0).
                sorted(Comparator.comparing(k -> -k.getValue())).map(k -> k.getValue()).findFirst().orElse(0D);
        return diGuiMethod(beginGamePoint, resultMap, minValue, newMin);
    }

    /**
     * 检测是否需要解散房间
     *
     * @return
     */
    @Override
    public boolean checkNeedEndRoom() {
        //同时勾选了这两个玩法 比赛分有小等于0的  房间直接结束
        if (room.isOnlyWinRightNowPoint() && room.isRulesOfCanNotBelowZero()) {
            if (RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
                for (int i = 0; i < this.getPlayerNum(); i++) {
                    AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(i);
                    if (roomPos.getRoomSportsPoint() <= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
