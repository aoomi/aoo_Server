package business.global.pk;

import business.global.mj.set.MJRoomPlayBackImpl;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomSet;
import business.global.room.base.RoomPlayBack;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.RoomDissolutionState;
import cenum.room.RoomEndPointEnum;
import cenum.room.SetState;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.base.BasePKRoom_PosEnd;
import jsproto.c2s.cclass.pk.base.BasePKRoom_SetEnd;
import jsproto.c2s.cclass.pk.base.BasePKSet_Pos;
import jsproto.c2s.cclass.playback.PlayBackData;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public abstract class AbsPKSetRoom extends AbsRoomSet {
    /**
     * 每个位置信息
     */
    protected Map<Integer, AbsPKSetPos> posDict = null;
    /**
     * 神信息
     */
    private final PKGodInfo godInfo;
    /**
     * 房间信息
     */
    protected AbsBaseRoom room = null;
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
     * 扑克牌
     */
    protected AbsPKSetCard setCard = null;
    /**
     * 上回合记录
     */
    protected AbsPKSetRound preRound = null;
    /**
     * 回合ID
     */
    private int roundId = 1;
    /**
     * 当前回合
     */
    protected AbsPKSetRound curRound = null;
    /**
     * 玩家操作管理
     */
    protected AbsPKSetPosMgr setPosMgr;
    /**
     * 本局结算信息
     */
    public BasePKRoom_SetEnd setEnd = null;

    /**
     * 最后操作时间
     */
    private int lastShotTime;

    /**
     * 当前操作信息
     */
    private PKCurOutCardInfo curOutCard = this.newPKCurOutCardInfo();

    /**
     * 是否保存回放了
     */
    private boolean isPlayBack;
    /**
     * 整局的打出信息
     * key 回合id
     */
    private Map<Integer, PKPosOutCardInfo> outCardInfoMap = new HashMap<>();

    public AbsPKSetRoom(int setID, PKRoom room, int dPos) {
        super(setID);
        this.room = room;
        this.dPos = dPos;
        this.setPosDict(new ConcurrentHashMap<>(this.room.getPlayerNum()));
        this.godInfo = new PKGodInfo(this, isConfigName());
    }

    public void reInit() {
        if (state == SetState.Init) {
            initSetPos();
        }
    }

    /**
     * 打牌
     *
     * @return
     */
    public PKCurOutCardInfo newPKCurOutCardInfo() {
        return new PKCurOutCardInfo();
    }

    /**
     * 获取指定位置
     *
     * @param opPos
     * @return
     */
    public AbsPKSetPos getPKSetPos(int opPos) {
        return this.posDict.get(opPos);
    }

    /**
     * 获取扑克牌
     *
     * @return
     */
    public AbsPKSetCard getPKSetCard() {
        return this.setCard;
    }

    /**
     * 牌数
     *
     * @return
     */
    public abstract int cardSize();

    /**
     * 小局结算消息
     *
     * @param roomID
     * @param setEnd
     * @return
     */
    protected abstract <T> BaseSendMsg setEnd(long roomID, T setEnd);

    /**
     * 玩家位置信息
     *
     * @param posID
     * @return
     */
    protected abstract AbsPKSetPos absPKSetPos(int posID);

    /**
     * 本局牌管理
     *
     * @return
     */
    protected abstract void absPKSetCard();

    /**
     * 本局玩家操作管理
     *
     * @return
     */
    protected abstract AbsPKSetPosMgr absPKSetPosMgr();

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
    protected abstract AbsPKSetRound nextSetRound(int roundID);

    /**
     * 摸牌
     *
     * @param opPos
     * @return
     */
    public Integer getCard(int opPos) {
        AbsPKSetPos setPos = this.posDict.get(opPos);
        // 随机摸牌
        Integer card = this.getGodInfo().godHandCard(setPos);
        if (Objects.isNull(card)) {
            return null;
        }
        // 设置牌
        setPos.addCard(card);
        return card;
    }

    /**
     * 初始化位置
     */
    protected void initSetPos() {
        for (int posId = 0; posId < this.getRoom().getPlayerNum(); posId++) {
            this.getPosDict().put(posId, this.absPKSetPos(posId));
        }
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
     * 检查小局托管自动解散
     */
    public boolean checkSetEndTrusteeshipAutoDissolution() {
        return false;
    }

    /**
     * 初始化神牌玩家身上的牌
     */
    protected void initGodPosCard() {
        // 神牌模式
        this.getPosDict().values().stream().forEach(k -> this.getGodInfo().isGodCard(k, k.getPosID()));
        // 神牌模式下补牌
        this.getGodInfo().godCardPrivate();
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
     * 计算当局每个pos位置的分数。
     */
    protected abstract void calcCurSetPosPoint();

    /**
     * 一局结束的信息
     *
     * @return
     */
    @Override
    public BasePKRoom_SetEnd getNotify_setEnd() {
        // 获取房间当局结束数据
        return this.mRoomSetEnd();
    }

    /**
     * 扑克当局结算
     *
     * @return
     */
    protected BasePKRoom_SetEnd newPKRoomSetEnd() {
        return new BasePKRoom_SetEnd();
    }

    /**
     * 获取房间当局结束数据
     *
     * @return
     */
    protected BasePKRoom_SetEnd mRoomSetEnd() {
        if (null != setEnd) {
            return this.setEnd;
        }
        this.setEnd = this.newPKRoomSetEnd();
        this.setEnd.setDPos(this.getDPos());
        this.setEnd.setEndTime(CommTime.nowSecond());
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            this.setEnd.setPlayBackCode(getPlayBackDateTimeInfo().getPlayBackCode());
            this.setEnd.setRoomDissolutionState(this.getRoom().getRoomDissolutionState());
        }
        this.setEnd.setSetId(this.getSetID());
        this.setEnd.setPosResultList(
                this.getPosDict().values().stream()
                        .map(k -> k.calcPosEnd())
                        .sorted(Comparator.comparing(BasePKRoom_PosEnd::getPos))
                        .collect(Collectors.toList()));
        return this.setEnd;
    }

    // 结算积分
    public void calcPoint() {
        // 计算当局每个pos位置的分数。
        this.calcCurSetPosPoint();
        // 设置最后操作时间
        this.setLastShotTime(CommTime.nowSecond());
        // 设置所有用户的超时
        this.getRoom().getRoomPosMgr().setAllLatelyOutCardTime();
        BasePKRoom_SetEnd setEnd = this.getNotify_setEnd();
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
    }

    /**
     * 初始玩家身上的牌
     */
    protected void initPosCard() {
        // 玩家初始发牌
        for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
            this.getPosDict().get(i).init(this.getSetCard().popList(this.cardSize()));
        }
    }


    /**
     * 设置位置的牌
     *
     * @param pid
     * @return
     */
    public List<BasePKSet_Pos> setPosCard(long pid) {
        return this.getPosDict().values().stream().map(k -> k.getNotify(pid)).collect(Collectors.toList());
    }

    /**
     * 通知本局开始
     */
    public void notify2SetStart() {
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
     * 记录当前回合
     */
    public void recordCurRound() {
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

    public RoomPlayBack newRoomPlayBackImpl() {
        return new MJRoomPlayBackImpl(this.getRoom());
    }

    /**
     * 获取房间回放记录
     *
     * @return
     */
    public RoomPlayBack getRoomPlayBack() {
        if (null == this.roomPlayBack) {
            this.roomPlayBack = newRoomPlayBackImpl();
        }
        return this.roomPlayBack;
    }

    /**
     * 如果是房卡类型，才需要回放记录
     */
    public void roomPlayBack() {
        // 除了房卡类型，其他的都记录回放。
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            this.isPlayBack = true;
            this.getRoomPlayBack().addPlayBack(new PlayBackData(this.room.getRoomID(), this.getSetID(), this.getDPos(), this.room.getCount(), this.room.getRoomKey(), this.room.getBaseRoomConfigure().getGameType().getId(), getPlayBackDateTimeInfo()));
        }
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
//        this.setEnd = null;
        this.curOutCard = null;
        this.room = null;
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
            ;
            this.curRound = null;
        }
        // 房间回放
        if (Objects.nonNull(this.roomPlayBack)) {
            this.roomPlayBack.clear();
            this.roomPlayBack = null;
        }
    }

    @Override
    public void calcDaJuFenShuRoomPoint() {
        if (this.getRoom().checkDaJuFenShu()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                    AbsPKSetPos setPos = this.getPosDict().get(i);
                    //最少要赢10分
                    Double roomPoint = setPos.getRoomPos().sportsPoint();
                    if (roomPoint > -10 && roomPoint < 0) {
                        Double addPoint;
                        if (RoomEndPointEnum.RoomEndPointEnum_Ten_Enough.equals(RoomEndPointEnum.valueOf(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getDajusuanfen()))) {
                            addPoint = new Double(10 - Math.abs(roomPoint));
                        } else {
                            addPoint = new Double(Math.abs(roomPoint));
                        }
                        if (room.isRulesOfCanNotBelowZero()) {
                            addPoint = setPos.getRoomPos().getRoomSportsPoint() >= addPoint ? addPoint : setPos.getRoomPos().getRoomSportsPoint();
                        }
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(setPos.getRoomPos().getPointYiKao(), addPoint));
                        setPos.getResults().setSportsPoint(setPos.getRoomPos().sportsPoint());
                        int subPosID = (setPos.getPosID() + 1) % getRoom().getPlayerNum();
                        AbsPKSetPos subAbsPKSetPos = this.getPKSetPos(subPosID);
                        subAbsPKSetPos.getRoomPos().setPointYiKao(CommMath.addDouble(subAbsPKSetPos.getRoomPos().getPointYiKao(), addPoint));
                        subAbsPKSetPos.getResults().setSportsPoint(subAbsPKSetPos.getRoomPos().sportsPoint());
                    }
                }
            }
        }
        if (this.getRoom().checkTakeLose()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                if (!this.getRoom().checkDaJuFenShu()) {
                    for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                        AbsPKSetPos setPos = this.getPosDict().get(i);
                        //坐下的值
                        Double seatSportsPoint = setPos.getRoomPos().getCalcSportPoint();
                        //结束值
                        Double roomPoint = CommMath.addDouble(setPos.getRoomPos().getRoomSportsPoint(), setPos.getRoomPos().getOtherSportsPointConsume());
                        //竞技点变化值
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(roomPoint, seatSportsPoint));
                    }
                }
                for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                    AbsPKSetPos setPos = this.getPosDict().get(i);
                    //坐下的值
                    Double seatSportsPoint = setPos.getRoomPos().getCalcSportPoint();
                    //结束值
                    Double roomPoint = CommMath.addDouble(setPos.getRoomPos().getRoomSportsPoint(), setPos.getRoomPos().getOtherSportsPointConsume());
                    //竞技点变化值
//                    setPos.getRoomPos().setPointYiKao(CommMath.subDouble(roomPoint, seatSportsPoint));
                    if (roomPoint > seatSportsPoint * 2) {
                        Double subPoint;
                        if (seatSportsPoint <= 0) {
                            subPoint = new Double(Math.abs(seatSportsPoint - roomPoint));
                        } else {
                            subPoint = new Double(Math.abs(roomPoint - seatSportsPoint * 2));//5
                        }
                        setPos.getRoomPos().setPointYiKao(CommMath.subDouble(setPos.getRoomPos().getPointYiKao(), subPoint));
                        setPos.getResults().setSportsPoint(setPos.getRoomPos().sportsPoint());
                        int subPosID = (setPos.getPosID() + 1) % getRoom().getPlayerNum();
                        AbsPKSetPos addAbsMjSetPos = this.getPKSetPos(subPosID);
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
//        Map<Integer, Double> pointMap = new HashMap<>();
//        // 各个位置是否进入讨钱状态map
//        Map<Integer, Boolean> pointFlag = new HashMap<>();
//        // 初始化一科计算分数所需要的map
//        this.getPosDict().values().forEach(k -> {
//            pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint() ,beiShu));
//            k.setDeductPointYiKao(k.getDeductPoint());
//            pointFlag.put(k.getPosID(), false);
//        });
//        // 亲友圈竞技点不能输到0分
//        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && this.getMHuInfo().getHuPos() >= 0) {
//            for (int i = 0; i < this.getPlayerNum(); i++) {
//                int nextPos = (this.getMHuInfo().getHuPos() + i) % this.getPlayerNum();
//                AbsPKSetPos setPos = this.getPosDict().get(nextPos);
//                Double oldPoint = CommMath.mul(setPos.getDeductPointYiKao(),beiShu);
//                if (oldPoint < 0) {
//                    continue;// 如果玩家输钱 不进行循环讨钱
//
//                }
//                setPos.setDeductPointYiKao(0);
//                for (int j = 1; j < this.getPlayerNum(); j++) {
//                    int nextPos2 = (nextPos + j) % this.getPlayerNum();
//                    AbsPKSetPos setPos2 = this.getPosDict().get(nextPos2);
//                    Double tempValue =  CommMath.mul(setPos2.getDeductPointYiKao() ,beiShu);
//                    if (tempValue > 0) {
//                        continue;// 如果被讨的人是赢钱的 则不进行被讨钱
//
//                    }
//                    if (Math.abs(tempValue) >= Math.abs(pointMap.get(nextPos2).intValue()) && pointFlag.get(nextPos2)) {
//                        continue;
//                    }
//                    boolean subFlag = false;
//                    // 初始化标志 判断是当前玩家是否是第一次被讨钱
//                    if (!pointFlag.get(nextPos2)) {
//                        subFlag = true;
//                        setPos2.setDeductPointYiKao(0);
//                        pointFlag.put(nextPos2, true);
//                    }
//                    // 符号转换
//                    tempValue = subFlag ? Math.abs(tempValue)
//                            : Math.abs(pointMap.get(nextPos2).intValue()) - Math.abs(tempValue);
//                    // 一科分数限制
//                    if (setPos2.getRoomPos().getRoomSportsPoint().intValue() - tempValue < 0) {
//                        tempValue = setPos2.getRoomPos().getRoomSportsPoint();
//                    }
//                    // 如果输的钱大于要讨的钱
//                    if (tempValue > oldPoint) {
//                        tempValue = oldPoint;
////                        Double finalValue = CommMath.div(tempValue, beiShu);
//                        setPos.setDeductPointYiKao(CommMath.addDouble(setPos.getDeductPointYiKao() , tempValue));
//                        setPos2.setDeductPointYiKao(CommMath.subDouble(setPos.getDeductPointYiKao() , tempValue));
//                        break;// 讨钱满足 到下一个人进行讨钱
//                    } else {
//                        // 如果输的钱小于要讨的钱
//                        oldPoint -= tempValue;
////                        Double finalValue = CommMath.div(tempValue, beiShu);
//                        setPos.setDeductPointYiKao(CommMath.addDouble(setPos.getDeductPointYiKao() , tempValue));
//                        setPos2.setDeductPointYiKao(CommMath.subDouble(setPos2.getDeductPointYiKao() ,tempValue));
//                    }
//                }
//            }
//        }
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
        //不同的情况 初始化的数据来源不同
        AbsPKSetPos sPos = this.getPosDict().values().stream().max(Comparator.comparingInt(m -> m.getDeductPoint())).get();
        int huPos = sPos.getPosID();
        // 亲友圈竞技点不能输到0分
        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && huPos >= 0) {
            Double totalWinPoint = 0D;//输的总分
            for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                AbsPKSetPos setPos = this.getPosDict().get(i);

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
            for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                AbsPKSetPos setPos = this.getPosDict().get(i);
                Double winPoint = setPos.getDeductPointYiKao();//12
                if (totalWinPoint <= 0) {
                    for (int j = i; j < this.getRoom().getPlayerNum(); j++) {
                        AbsPKSetPos setPos2 = this.getPosDict().get(j);
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
        AbsPKSetPos sPos = this.getPosDict().values().stream().max(Comparator.comparingInt(m -> m.getDeductPoint())).get();
        int huPos = sPos.getPosID();
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
            AbsPKSetPos setPos = i.getValue();
            Double losePoint = setPos.getDeductPointYiKao();
            return (setPos.getRoomPos().getGameBeginSportsPoint() < 0D && losePoint <= 0D) || CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint()) <= 0D;
        });
        if (allMatch) {
            return;
        }

        boolean isFirst = false;
        // 只能赢自己身上的分数
        if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) && huPos >= 0) {
            for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
                AbsPKSetPos setPos = this.getPosDict().get(i);
                Double losePoint = setPos.getDeductPointYiKao();
                //没有赢分的话 不进行考虑
                if (losePoint <= 0) {
                    continue;
                }
                //需要扣减的分数
                Double needSubPoint;
                if (setPos.getRoomPos().getGameBeginSportsPoint() <= 0D) {
                    //需要扣减的分数
                    needSubPoint = new Double(losePoint);
                } else {
                    //需要扣减的分数
                    needSubPoint = CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint());
                }

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
    private Map<Integer, Double> subPointByOnlyWinRightNowPoint(Map<Integer, Double> temp, AbsPKSetPos setPos, boolean isFirst, Double otherPoint) {
        Map<Integer, Double> resultMap = new HashMap<>(temp);
//        for (int i = 0; i < this.getPlayerNum(); i++) {
//            AbsMJSetPos absMJSetPos = this.getPosDict().get(i);
//            resultMap.put(absMJSetPos.getPosID(), absMJSetPos.getDeductPointYiKao());
//        }
        //传递参数使用
        Map<Integer, Double> resultMap2 = new HashMap<>(resultMap);
        Long newPlayerNum = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        Double beginGamePoint;
        if (setPos.getRoomPos().getGameBeginSportsPoint() <= 0D) {
            beginGamePoint = new Double(otherPoint);
        } else {
            beginGamePoint = CommMath.addDouble(setPos.getRoomPos().getGameBeginSportsPoint(), otherPoint);
        }
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
     * 获取除了某个玩家外 其他玩家 分数超过带多少赢的
     *
     * @return
     */
    private Double getExceptOneOtherPoint() {
        Double totalValue = new Double(0D);
        for (int i = 0; i < getRoom().getPlayerNum(); i++) {
            AbsPKSetPos absMJSetPos = this.getPosDict().get(i);
            if (absMJSetPos.getDeductPointYiKao() <= 0 ||
                    absMJSetPos.getDeductPointYiKao() > absMJSetPos.getRoomPos().getGameBeginSportsPoint()) {
                continue;
            }
            totalValue = CommMath.addDouble(totalValue, absMJSetPos.getDeductPointYiKao());
        }
        return totalValue;
    }
}
