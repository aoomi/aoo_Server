package business.global.mj.extbussiness;

import business.global.mj.*;
import business.global.mj.extbussiness.dto.StandardMJPlayerPosInfo;
import business.global.mj.extbussiness.dto.StandardMJRoomSetEnd;
import business.global.mj.extbussiness.dto.StandardMJRoomSetInfo;
import business.global.mj.extbussiness.dto.StandardMJWaitingExInfo;
import business.global.mj.extbussiness.dto.iclass.CStandardMJ_PiaoFen;
import business.global.mj.extbussiness.dto.iclass.SStandardMJ_BiaoShi;
import business.global.mj.extbussiness.dto.iclass.SStandardMJ_ChangeStatus;
import business.global.mj.extbussiness.dto.iclass.SStandardMJ_SetEnd;
import business.global.mj.extbussiness.hutype.StandardMJTingImpl;
import business.global.mj.extbussiness.optype.StandardMJBuHuaImpl;
import business.global.mj.extbussiness.optype.StandardMJKaiJinImpl;
import business.global.mj.manage.MJFactory;
import business.global.room.base.AbsRoomPos;
import business.global.room.mj.MJRoomPos;
import cenum.RoomTypeEnum;
import cenum.mj.FlowerEnum;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;
import cenum.room.RoomDissolutionState;
import cenum.room.SetState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.pos.PlayerPosInfo;
import jsproto.c2s.iclass.mj.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 麻将 一局游戏逻辑
 *
 * @author Huaxing
 * @apiNote 子游戏请继承这个类
 */
@Data
public class StandardMJRoomSet<T extends StandardMJTingImpl, E extends StandardMJBuHuaImpl, Y extends StandardMJKaiJinImpl> extends AbsMJSetRoom {

    /**
     * 抄庄位置
     */
    private int outCardPos = -1;
    /**
     * 抄庄的牌值
     */
    private int outCardType = 0;
    /**
     * <hr>
     * 抄庄状态
     * <p style="color:yellow;">【0:还没开始抄庄 1:抄庄成功 2:抄庄失败】
     * <hr>
     */
    private int chaoState = 0;
    /**
     * 抄庄成功次数
     */
    private int zhuiZhuangNum = 0;
    /**
     * 补花延迟时间
     */
    private long buHuaTime = 0;
    /**
     * 剩余可摸的牌数量
     */
    private int leftSize = 0;
    /**
     * 进金翻开的牌
     */
    private int openCard = 0;
    /**
     * 神牌设置的翻开金列表
     **/
    public final List<Integer> jinList;
    /**
     * 初始分数
     *
     * @implNote 输分上限，每个人带这个分数进去，输到0分就游戏结束
     **/
    private int limitScore;
    /**
     * 天胡标志
     */
    private boolean tiAnHuFlag = true;
    /**
     * 漂分状态
     */
    protected StandardMJRoomEnum.WaitingExType curWaitingExType = StandardMJRoomEnum.WaitingExType.NOT;
    /**
     * 翻开马牌列表
     */
    private List<Integer> maList = new ArrayList<>();

    public StandardMJRoomSet(int setID, StandardMJRoom room, int dPos) {
        super(setID, room, dPos);
        this.startMS = CommTime.nowMS();
        this.jinList = getGodInfo().getJinList();
        this.addGameConfig();
        this.getmJinCardInfo().setJinMap(new LinkedHashMap<>());
        this.initSet();
        this.initSetState();
        this.limitScore = room.getGuoFen();
    }

    @Override
    protected void calcCurSetPosPoint() {
        this.getPosDict().values().forEach(n -> {
            int realPiao = ((StandardMJRoomPos) n.getRoomPos()).getPiaoFenEnum().getPiao();
            if (realPiao > 0) {
                n.calcOpPointType(StandardMJRoomEnum.OpPoint.Piao, realPiao);
            }
        });
        this.getPosDict().values().forEach(AbsMJSetPos::calcPosPoint);
        this.calcOtherPoint();
        // 计算圈
        this.calcCurSetQuan();
    }


    @Override
    public boolean update(int sec) {
        boolean isClose = false;
        if (this.state == SetState.Init) {
            if (CommTime.nowMS() > this.startMS + this.InitTime) {
                if (((StandardMJRoom) room).getJinType().ordinal() > 0) {
                    MJFactory.getOpCard(getKaiJinClass()).doOpCard(getMJSetPos(getDPos()), 0);
                    this.sendSetPosCard();
                }
                if (CollectionUtils.isNotEmpty(((StandardMJRoom) room).getHuaList())) {
                    this.state = SetState.Init2;
                } else {
                    this.state = SetState.Playing;
                    if (!this.startNewRound()) {
                        this.endSet();
                    }
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
            isClose = true;
        } else if (this.state == SetState.Init2) {
            if (CommTime.nowMS() - this.buHuaTime > ((StandardMJRoom)room).getCustomerBuHuaTime()) {
                for (int i = 0; i < room.getPlayerNum(); i++) {
                    int posID = (i + this.dPos) % this.getRoom().getPlayerNum();
                    if (MJFactory.getOpCard(getBuHuaClass()).checkOpCard(this.getMJSetPos(posID), FlowerEnum.PRIVATE.ordinal())) {
                        this.buHuaTime = CommTime.nowMS();
                        return false;
                    }
                }
                this.state = SetState.Playing;
                if (!this.startNewRound()) {
                    this.endSet();
                }
            }
        } else if (this.state == SetState.WaitingEx) {
            if (CommTime.nowMS() - this.startMS >= 30000) {
                if (curWaitingExType == StandardMJRoomEnum.WaitingExType.PIAO) {
                    for (AbsRoomPos roomPos : this.room.getRoomPosMgr().getPosList()) {
                        StandardMJRoomPos EXTRoomPos = (StandardMJRoomPos) roomPos;
                        if (EXTRoomPos.getPiaoFenEnum().getPiao() < 0) {
                            opPiaoFen(null, EXTRoomPos.getPid(), CStandardMJ_PiaoFen.make(this.room.getRoomID(), 0));
                        }
                    }
                }
            }
        }
        return isClose;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StandardMJRoomSet) {
            return getSetID() == ((StandardMJRoomSet) o).getSetID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getSetID());
    }

    @Override
    public BaseSendMsg DissolveTrusteeship(long roomId, List<Long> pidList, int sec) {
        return _DissolveTrusteeship.make(roomId, pidList, sec, room.getGameName());
    }

    @Override
    public void sendSetPosCard() {
        for (int i = 0; i < room.getPlayerNum(); i++) {
            AbsMJSetPos setPos = posDict.get(i);
            setPos.sortCards();
        }
        for (int i = 0; i < room.getPlayerNum(); i++) {
            long pid = this.room.getRoomPosMgr().getPosByPosID(i).getPid();
            if (i == 0) {
                this.getRoomPlayBack().playBack2Pos(i,
                        _SetPosCard.make(this.room.getRoomID(), this.setPosCard(pid), getRoom().getGameName()), null);
            } else {
                this.room.getRoomPosMgr().notify2Pos(i,
                        _SetPosCard.make(this.room.getRoomID(), this.setPosCard(pid), getRoom().getGameName()));
            }
        }
    }

    @Override
    public void MJApplique(int pos) {
        AbsMJSetPos setPos = posDict.get(pos);
        BaseMJSet_Pos posInfoOther = setPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = setPos.getNotify(true);
        getRoomPlayBack().playBack2Pos(pos,
                _Applique.make(getRoom().getRoomID(), pos, OpType.Out, 0, false, posInfoSelf, getSetCard().getRandomCard().getNormalMoCnt(), getSetCard().getRandomCard().getGangMoCnt(), getRoom().getGameName()), null);
        for (int i = 0; i < getRoom().getPlayerNum(); i++) {
            if (i == pos)
                continue;
            getRoom().getRoomPosMgr().notify2Pos(i,
                    _Applique.make(getRoom().getRoomID(), pos, OpType.Out, 0, false, posInfoOther, getSetCard().getRandomCard().getNormalMoCnt(), getSetCard().getRandomCard().getGangMoCnt(), getRoom().getGameName()));
        }
    }

    @Override
    public void kaiJinNotify(MJCard jinCard, MJCard jinCard2) {
        int jin2 = getmJinCardInfo().getJinKeys().size() >= 2 ? getmJinCardInfo().getJin(2).getCardID() : 0;
        int jin1 = getmJinCardInfo().getJinKeys().size() >= 1 ? getmJinCardInfo().getJin(1).getCardID() : 0;
        getRoomPlayBack().playBack2All(_Jin.make(getRoom().getRoomID(), jin1, jin2, openCard, 0, getMJSetCard().getRandomCard().getNormalMoCnt(), getMJSetCard().getRandomCard().getGangMoCnt(), getRoom().getGameName()));
    }

    @Override
    protected <T> BaseSendMsg posGetCard(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos) {
        return _PosGetCard.make(roomID, pos, normalMoCnt, gangMoCnt, set_Pos, room.getGameName());
    }

    @Override
    protected <T> BaseSendMsg setEnd(long roomID, T setEnd) {
        return SStandardMJ_SetEnd.make(roomID, setEnd, room.checkIsRoomEnd(), getRoom().getGameName());
    }

    @Override
    protected <T> BaseSendMsg setStart(long roomID, T setInfo) {
        return _SetStart.make(roomID, setInfo, getRoom().getGameName());
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void clearBo() {
        super.clearBo();
    }

    @Override
    public int kaiJinNum() {
        return 0;
    }

    @Override
    public boolean isBaiBanTiJin() {
        return false;
    }

    @Override
    public void addGameConfig() {
        this.getRoomPlayBack().addPlaybackList(_Config.make(this.getRoom().getCfg(), this.getRoom().getRoomTyepImpl().getRoomTypeEnum(), getRoom().getGameName()), null);
    }

    public boolean isConfigName() {
        return true;
    }

    public void exeStartSet() {
        //    只赢当前身上分的时候要用
        this.recordRoomPosPointBeginStart();
        this.room.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
    }

    public long getGangNum() {
        return getPosDict().values().stream().mapToLong(k -> k.getPublicCardList().stream().filter(n -> OpType.Gang.value() == n.get(0) || OpType.JieGang.value() == n.get(0) || OpType.AnGang.value() == n.get(0)).count()).sum();
    }

    @Override
    public boolean isCanChiPeng(List<Integer> chis, AbsMJSetPos mSetPos, Integer... type) {
        if (((StandardMJRoom) room).isChiPengWuPai()) {
            long count = mSetPos.allCards().stream().filter(n -> !chis.contains(n.getCardID()) && !n.isLock).count();
            return count > 0;
        }
        return true;
    }

    public Integer popJin() {
        if (CollectionUtils.isNotEmpty(jinList)) {
            return jinList.remove(0);
        }
        return 0;
    }

    public MJCard getCard(int opPos, boolean isNormalMo) {
        MJCard card = super.getCard(opPos, isNormalMo);
        StandardMJSetPos setPos = (StandardMJSetPos) this.posDict.get(opPos);
        // 通知房间内的所有玩家，指定玩家摸牌了。
        if (card != null && ((StandardMJRoom) room).getHuaList().contains(card.type)) {
            MJFactory.getOpCard(getBuHuaClass()).checkOpCard(setPos, FlowerEnum.HAND_CARD.ordinal());
        }
        return card;
    }

    public void setStateInit() {
        setState(SetState.Init);
        startSet();
    }

    @Override
    public void setState(SetState setState) {
        this.state = setState;
        this.startMS = CommTime.nowMS();
        StandardMJRoomPosMgr roomPosMgr = (StandardMJRoomPosMgr) this.room.getRoomPosMgr();
        this.room.getRoomPosMgr().notify2All(SStandardMJ_ChangeStatus.make(this.room.getRoomID(), this.getSetID(), this.getState(), this.dPos, curWaitingExType.name().toLowerCase(), roomPosMgr.getPiaoFenInfoList(), room.getGameName()));
    }

    @Override
    public void startSet() {
        CommLogD.info("startSet id:{}", getSetID());
        for (int i = 0; i < room.getPlayerNum(); i++) {
            long pid = this.room.getRoomPosMgr().getPosByPosID(i).getPid();
            // 牌局开始消息通知
            BaseSendMsg baseSendMsg = this.setStart(this.room.getRoomID(), this.getNotify_set(pid));
            if (i == 0) {
                this.getRoomPlayBack().playBack2Pos(i, baseSendMsg, setPosMgr.getAllPlayBackNotify());
            } else {
                this.room.getRoomPosMgr().notify2Pos(i, baseSendMsg);
            }
        }

        exeStartSet();
    }

    /**
     * 开始发牌
     */
    public void superStartSet() {
        super.startSet();
    }

    public void initSetState() {
        if (((StandardMJRoom) room).isPiaoFenEvery()) {
            this.curWaitingExType = StandardMJRoomEnum.WaitingExType.PIAO;
            getRoom().getRoomPosMgr().getRoomPosList().forEach(n -> ((StandardMJRoomPos) n).getPiaoFenEnum().setPiao(-1));
            setState(SetState.WaitingEx);
        } else if (((StandardMJRoom) room).isPiaoFenGuDing()) {
            int piaoFenGuDing = ((StandardMJRoom) room).getPiaoFenGuDing();
            getRoom().getRoomPosMgr().getRoomPosList().forEach(n -> ((StandardMJRoomPos) n).getPiaoFenEnum().setPiao(piaoFenGuDing));
            setStateInit();
        } else {
            setStateInit();
        }
    }

    public void initSet() {
        this.absMJSetCard();
        this.setPosMgr = this.absMJSetPosMgr();
        for (int idx = 0; idx < room.getPlayerNum(); idx++) {
            int i = (setCard.getRandomCard().getStartPaiPos() + idx)
                    % room.getPlayerNum();
            AbsMJSetPos setPos = this.absMJSetPos(i);
            this.posDict.put(i, setPos);
            if (!this.getGodInfo().isGodCard(setPos, i)) {
                setPos.init(this.setCard.popList(this.cardSize(), i));
            }
        }
        this.getGodInfo().godCardPrivate();
        if (getGodInfo().isGodCardMode()) {
            this.getPosDict().values().forEach(MJSetPos::initSortCardsAndCalcHuFan);
        }
    }

    @Override
    public void endSet() {
        CommLogD.info("endSet id:{}", getSetID());
        if (this.state == SetState.End) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomRealDissolutionState())
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
        if (((StandardMJRoom) room).getMaiMa() > 0 && this.getMHuInfo().isHuNotEmpty()) {
            if (this.getMHuInfo().getHuPosList().size() >= 1) {
                this.maList = ((StandardMJSetCard) setCard).popMaPai();
                if (CollectionUtils.isNotEmpty(maList)) {
                    for (AbsMJSetPos value : posDict.values()) {
                        if (getMHuInfo().getHuPosList().contains(value.getPosID())) {
                            ((StandardMJSetPos) value).zhongList = maList.stream().filter(z -> ((StandardMJSetCard) getSetCard()).isZhongMa(room, value.getPosID(), z / 100)).collect(Collectors.toList());
                        }
                    }
                }
            }
        }
        this.state = SetState.End;
        setEnd(true);
        this.calcPoint();
        this.getRoomPlayBack().playBack2All(this.setEnd(room.getRoomID(), this.getNotify_setEnd()));
        this.setTrusteeshipAutoDissolution();
        this.roomPlayBack();
    }

    public void opPiaoFen(WebSocketRequest request, long pid, CStandardMJ_PiaoFen data) {
        if (!this.state.equals(SetState.WaitingEx)) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "opPiaoFen cur setstate=" + this.state);
            }
            return;
        }
        if (StandardMJRoomEnum.WaitingExType.PIAO != curWaitingExType) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "opPiaoFen cur curWaitingExType=" + curWaitingExType);
            }
            return;
        }
        if (data.choose < 0) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "opPiaoFen cur choose over=" + data.choose);
            }
            return;
        }
        StandardMJRoomPos roomPos = (StandardMJRoomPos) this.room.getRoomPosMgr().getPosByPid(pid);
        StandardMJWaitingExInfo piaoFenEnum = roomPos.getPiaoFenEnum();
        if (piaoFenEnum.getPiao() >= 0) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "opPiaoFen  your PaoFen = ");
            }
            return;
        }
        if (curWaitingExType == StandardMJRoomEnum.WaitingExType.PIAO) {
            piaoFenEnum.setPiao(data.choose);
        }
        roomPos.setPiaoFenEnum(piaoFenEnum);
        this.room.getRoomPosMgr().notify2All(SStandardMJ_BiaoShi.make(data.roomID, roomPos.getPosID(), ((StandardMJRoomPosMgr) room.getRoomPosMgr()).getPiaoFenInfoList(), room.getGameName()));
        StandardMJRoomPosMgr roomPosMgr = (StandardMJRoomPosMgr) this.room.getRoomPosMgr();
        if (roomPosMgr.checkAllOpPiaoFen()) {
            curWaitingExType = StandardMJRoomEnum.WaitingExType.NOT;
            setStateInit();
        }
    }

    @Override
    public boolean checkSetEndTrusteeshipAutoDissolution() {
        if (((StandardMJRoom) getRoom()).isSetAutoJieSan()) {
            return true;
        } else if (((StandardMJRoom) getRoom()).isFangJianConstant(StandardMJRoomEnum.GameRoomConfigEnum.TuoGuan2XiaoJuJieSan)) {
            List<Long> trusteeshipPlayerList = getRoom().getRoomPosMgr().getRoomPosList().stream()
                    .filter(n -> n.isTrusteeship() && ((StandardMJRoomPos) n).getTuoGuanSetCount() >= 2).map(AbsRoomPos::getPid).collect(Collectors.toList());
            if (trusteeshipPlayerList.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public void calcOtherPoint() {
        this.onlyWinRightNowPoint();
        //计算不低于0
        this.calYiKaoPoint();
        //统计完后计算房间结束状态
        ((StandardMJRoom) room).setEnd(this.checkNeedEndRoom());
        long count = 0;
        if (((StandardMJRoom) getRoom()).getGuoFen() > 0) {
            count = posDict.values().stream().filter(m -> m.getEndPoint() + m.getRoomPos().getPoint() + limitScore <= 0).count();
        }
        if (count > 0) {
            ((StandardMJRoom) room).isEnd = true;
        }
    }

    public void outCard(int opPos, int opCardID) {
        if (!((StandardMJRoom) this.room).isChaoZhuang() || chaoState != 0) {
            return;
        }
        int cardType = opCardID / 100;
        int i = (opPos + 1) % this.room.getPlayerNum();
        if (this.outCardPos < 0) {
            this.outCardPos = opPos;
            this.outCardType = cardType;
        } else {
            if (this.outCardType == cardType) {
                if (i == this.outCardPos && i == this.dPos) {
                    this.setChaoState(1);
                    getRoomPlayBack().playBack2All(_GenZhuang.make(getRoom().getRoomID(), getDPos(), zhuiZhuangNum, getRoom().getGameName()));
                    outCardPos = -1;
                    outCardType = 0;
                    chaoState = 0;
                    zhuiZhuangNum += 1;
                }
            } else {
                this.setChaoState(2);
            }
        }
    }

    public List<StandardMJPlayerPosInfo> getPlayerInfoList(int kouFenPos, int point, int losePos) {
        List<PlayerPosInfo> playerPosInfoList = this.room.getRoomPosMgr().getPlayerPosInfoList();
        List<StandardMJPlayerPosInfo> playerPosInfos = new ArrayList<>();
        int kouFen;
        for (PlayerPosInfo playerPosInfo : playerPosInfoList) {
            if (playerPosInfo.getPosID() == kouFenPos) {
                kouFen = point * (losePos == -1 ? getPlayerNum() - 1 : 1);
            } else if (playerPosInfo.getPosID() == losePos || losePos == -1) {
                kouFen = -point;
            } else {
                kouFen = 0;
            }
            StandardMJPlayerPosInfo playerPosInfo1 = new StandardMJPlayerPosInfo(playerPosInfo, kouFen);
            playerPosInfos.add(playerPosInfo1);
        }
        return playerPosInfos;
    }

    /**
     * point是基础分数，losePos出双倍，其他人出单倍。kouFenPos是赢家
     * @param kouFenPos
     * @param point
     * @param losePos
     * @return
     */
    public List<StandardMJPlayerPosInfo> getOtherPlayerInfoList(int kouFenPos, int point, int losePos) {
        List<PlayerPosInfo> playerPosInfoList = this.room.getRoomPosMgr().getPlayerPosInfoList();
        List<StandardMJPlayerPosInfo> playerPosInfos = new ArrayList<>();
        int kouFen;
        int points = 0;
        for (PlayerPosInfo playerPosInfo : playerPosInfoList) {
            if (playerPosInfo.getPosID() != kouFenPos) {
                points+= playerPosInfo.getPosID() == losePos ? 2*point : point;
            }
        }
        for (PlayerPosInfo playerPosInfo : playerPosInfoList) {
            if (playerPosInfo.getPosID() == kouFenPos) {
                kouFen = points;
            } else if (playerPosInfo.getPosID() == losePos) {
                kouFen = -2*point;
            } else {
                kouFen = -point;
            }
            StandardMJPlayerPosInfo playerPosInfo1 = new StandardMJPlayerPosInfo(playerPosInfo, kouFen);
            playerPosInfos.add(playerPosInfo1);
        }
        return playerPosInfos;
    }

    public Class getTingClass() {
        try {
            // 以下调用带参的、私有构造函数
            return (Class<T>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
            return StandardMJTingImpl.class;
        }
    }

    public Class getBuHuaClass() {
        try {
            // 以下调用带参的、私有构造函数
            return (Class<E>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1]);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
            return StandardMJBuHuaImpl.class;
        }
    }

    public Class getKaiJinClass() {
        try {
            // 以下调用带参的、私有构造函数
            return (Class<Y>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2]);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
            return StandardMJKaiJinImpl.class;
        }
    }

    @Override
    protected StandardMJRoomSetInfo newMJRoomSetInfo() {
        return ((StandardMJRoom) room).childrenMJRoomSetInfo();
    }

    @Override
    public StandardMJRoomSetEnd getNotify_setEnd() {
        StandardMJRoomSetEnd setEndInfo = (StandardMJRoomSetEnd) this.mRoomSetEnd();
        StandardMJRoomEnum.KaiJin jinType = ((StandardMJRoom) getRoom()).getJinType();
        if (jinType.value > 0) {
            int jin2 = getmJinCardInfo().getJinKeys().size() >= 2 ? getmJinCardInfo().getJin(2).getCardID() : 0;
            if (jin2 > 0) {
                setEndInfo.setJin2(this.getmJinCardInfo().getJin(2).getCardID());
            }
            int jin1 = getmJinCardInfo().getJinKeys().size() >= 1 ? getmJinCardInfo().getJin(1).getCardID() : 0;
            if (jin1 > 0) {
                setEndInfo.setJin(this.getmJinCardInfo().getJin(1).getCardID());
            }
            setEndInfo.setJinJin(openCard);
        }
        setEndInfo.maList = maList;
        if (((StandardMJRoom) room).isSameZhongMa()) {
            setEndInfo.zhongList = posDict.values().stream().flatMap(z -> ((StandardMJSetPos) z).getZhongList().stream()).collect(Collectors.toList());
        }
        setEndInfo.zhongMa = CollectionUtils.isNotEmpty(setEndInfo.maList);
        return setEndInfo;
    }

    @Override
    public StandardMJRoomSetInfo getNotify_set(long pid) {
        StandardMJRoom standardMJRoom = (StandardMJRoom) this.room;
        StandardMJRoomSetInfo ret = (StandardMJRoomSetInfo) this.getMJRoomSetInfo(pid);
        StandardMJRoomEnum.KaiJin jinType = ((StandardMJRoom) getRoom()).getJinType();
        if (jinType.value > 0) {
            int jin2 = getmJinCardInfo().getJinKeys().size() >= 2 ? getmJinCardInfo().getJin(2).getCardID() : 0;
            if (jin2 > 0) {
                ret.setJin2(this.getmJinCardInfo().getJin(2).getCardID());
            }
            int jin1 = getmJinCardInfo().getJinKeys().size() >= 1 ? getmJinCardInfo().getJin(1).getCardID() : 0;
            if (jin1 > 0) {
                ret.setJin(this.getmJinCardInfo().getJin(1).getCardID());
            }
            ret.setJinJin(openCard);
        }
//        System.out.println("Gsagsa"+getmJinCardInfo().getJin(1).getCardID());
        if (standardMJRoom.isPiaoFenEvery() || standardMJRoom.isPiaoFenGuDing()) {
            StandardMJRoomPosMgr roomPosMgr = (StandardMJRoomPosMgr) this.room.getRoomPosMgr();
            ret.setWaitingExType(curWaitingExType.name().toLowerCase(Locale.ROOT));
            ret.setBiaoShiList(roomPosMgr.getPiaoFenInfoList());
        }
        ret.setQuanShu(standardMJRoom.circleNum);
        return ret;
    }

    @Override
    protected StandardMJRoomSetEnd newMJRoomSetEnd() {
        return new StandardMJRoomSetEnd();
    }

    @Override
    protected AbsMJSetRound nextSetRound(int roundID) {
        return new StandardMJSetRound(this, roundID);
    }

    @Override
    protected AbsMJSetPosMgr absMJSetPosMgr() {
        return new StandardMJSetPosMgr(this);
    }

    @Override
    protected AbsMJSetPos absMJSetPos(int posID) {
        return new StandardMJSetPos(posID, (MJRoomPos) this.room.getRoomPosMgr().getPosByPosID(posID), this);
    }

    @Override
    protected void absMJSetCard() {
        this.setSetCard(new StandardMJSetCard(this));
    }

    @Override
    public int cardSize() {
        return MJSpecialEnum.SIZE_13.value();
    }

    @Override
    public int calcNextDPos() {
        if (this.getMHuInfo().isHuNotEmpty()) {
            if (this.getMHuInfo().getRoundMaxHuPosList().size() > 1) {
                return this.getLastOpInfo().getLastOpPos();
            }
            return this.getMHuInfo().getHuPos() == dPos ? dPos : (dPos + 1) % getPlayerNum();
        } else {
            return dPos;
        }
    }

    public void setLeftSize() {
        if (((StandardMJRoom) room).isGangHouLiuPai()) {
            long sum = getGangNum();
            if (sum == 1) {
                this.leftSize = 18;
            } else if (sum == 2) {
                this.leftSize = 20;
            } else if (sum == 3) {
                this.leftSize = 24;
            } else if (sum == 4) {
                this.leftSize = 200;
            }
        }
    }

    /**
     * 发牌的时候记录分数
     * 只赢当前身上分的时候要用
     */
    @Override
    public void recordRoomPosPointBeginStart() {
        if (this.room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            for (int i = 0; i < room.getPlayerNum(); i++) {
                AbsRoomPos roomPos = this.room.getRoomPosMgr().getPosByPosID(i);
                roomPos.setGameBeginSportsPoint(roomPos.getRoomSportsPoint());
            }
        }
    }

    public int getLeftSizeByOpType(OpType opType) {
        return getLeftSize();
    }

    /**
     * 计算圈
     */
    private void calcCurSetQuan() {
        StandardMJRoom bdRoom = ((StandardMJRoom) this.getRoom());
        // 是否算圈
        if (bdRoom.getQuanSetCount().contains(bdRoom.getCount())) {
            int beforeDpos = getDPos();
            int dPosInt = calcNextDPos();
            //庄数增加
            StandardMJRoom room = (StandardMJRoom) this.room;
            if (beforeDpos != dPosInt && beforeDpos == room.getLastPosId()) {
                //下庄了，新加一圈
                if (room.getCircleNum() + 1 > room.getSetCount()) {
                    //圈结束
                    room.isEnd = true;
                }
            }
        }
    }
}
				
