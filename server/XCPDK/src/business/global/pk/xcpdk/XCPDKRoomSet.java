package business.global.pk.xcpdk;

import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomSet;
import business.global.room.base.RoomPlayBack;
import business.xcpdk.c2s.cclass.*;
import business.xcpdk.c2s.iclass.*;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.Robot.RobotMgr;
import business.player.feature.PlayerCurrency;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_GameStatus;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_ROBCLOSE_STATUS;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_WANFA;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.CommLogD;
import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.GameSetBO;
import core.db.service.clarkGame.GameSetBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.BasePocker;
import jsproto.c2s.cclass.pk.BasePocker.PockerListType;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.Victory;
import jsproto.c2s.cclass.playback.PlayBackData;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.pk.SPDK_OutCardList;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 跑得快一局游戏逻辑
 *
 * @author zaf
 */

public class XCPDKRoomSet extends AbsRoomSet {
    private final GameRandomSource gameRandom = SeededGameRandomSource.create();
    /**
     * 每个位置信息
     */
    protected Map<Integer, XCPDKSetPos> posDict = null;
    public XCPDKRoom room = null;
    public long startMS = 0;
    protected XCPDK_GameStatus status = XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD;
    public XCPDKSetCard setCard = null;
    public XCPDKRoom_SetEnd setEnd = new XCPDKRoom_SetEnd();
    public GameSetBO bo = null;
    protected int m_OpPos = 0; // 当前操作位置
    protected int eightPos = -1; // 8大队操作位置
    protected Victory m_FirstOpVic = new Victory(-1, -1); // 首出操作位置
    protected int m_Razz = -1; // 赖子
    protected int m_FirstOpCard = 0; // 先出的牌
    protected boolean m_bFirstOp = true; // 是否是一轮的首出
    protected Victory m_RobCloseVic = new Victory(-1, XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_NOMAL.value()); // 关门位置
    protected Victory m_ReverseRobCloseVic = new Victory(-1, XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_NOMAL.value()); // 反关门位置
    protected int m_RoomDouble = 1; // 房间倍数
    protected ArrayList<Integer> m_RoomDoubleList = new ArrayList<Integer>(); // 房间倍数
    protected boolean m_bRobCloseCalc = false; // 抢关门是否结算
    protected int m_AddRoomDoubleCount = 0; // 房间炸弹了加了几次
    public ArrayList<Victory> addDouble; // 是否加倍 victory：pos:加倍位置 num 加倍陪数
    public ArrayList<Integer> pointList; // 得分
    public ArrayList<Boolean> resultCalcList; // 是否结算过
    public ArrayList<Integer> surplusCardList; // 剩余牌数
    public ArrayList<ArrayList<Integer>> surplusCardRecordList; // 剩余牌数
    public ArrayList<Boolean> beShutDowList; // 是否被关门
    public ArrayList<Victory> openCardList; // 是否明牌 victory：pos:加倍位置 num 加倍陪数
    public ArrayList<Victory> robCloseList; // 是否抢关门 victory：pos:加倍位置 num 加倍陪数
    protected RoomPlayBack roomPlayBack; // 回放
    public List<Integer> totalPointResult = new ArrayList<>();//总分

    HashMap<Integer, List<Integer>> hMap = new HashMap<Integer, List<Integer>>();

    public static final int WAITTRUSTEESHIPTIME = 3000; // 托管延迟2s
    public static final int ROBCLOSEFIAL = 2; // 抢关门翻倍
    public static final byte HONGTAO10FANBEI = 0x2A; // 红桃十
    public static final int HONGTAO10FANBEIADDDOUBLE = 2;// 红桃十翻倍
    public static final int OPENCARDDOUBLE = 2; // 明牌翻倍
    public static final int DEFAULTDAINUM = 2; // 默认带两张
    public static final int DEFAULT4DAINUM = 3; // 默认带三张
    public static final int BOMBDOUBLE = 2; // 炸弹翻倍
    public static final int FOURDAIFANDOUBLE = 2; // 四带翻倍
    public XCPDKRoomSetSound curRound = null;
    public List<XCPDKRoomSetSound> historyRound = new ArrayList<>();
    public List<SPDK_OutCardList> cardList = new ArrayList<>();//出手牌顺序
    private int playBackCode = 0;
    public ArrayList<Double> sportsPointList = null;
    public ArrayList<Boolean> zhaNiao = new ArrayList<>() ;		//红桃十扎鸟
    public List<List<XCPDK_Bomb>> bombList = new ArrayList<>() ;		//炸弹列表
    public ArrayList<Boolean> xiaoGuanList; // 小关列表
    public boolean atOnceStart = false;//立马开始

    public XCPDKRoomSet(XCPDKRoom room) {
        super(room.getCurSetID());
        this.room = room;
        this.addDouble = new ArrayList<Victory>();
        this.openCardList = new ArrayList<Victory>();
        this.robCloseList = new ArrayList<Victory>();
        this.pointList = new ArrayList<Integer>(Collections.nCopies(this.room.getPlayerNum(), 0));
        this.initSportsPointList();
        this.resultCalcList = new ArrayList<Boolean>(Collections.nCopies(this.room.getPlayerNum(), false));
        this.beShutDowList = new ArrayList<Boolean>(Collections.nCopies(this.room.getPlayerNum(), false));
        this.surplusCardList = new ArrayList<Integer>(Collections.nCopies(this.room.getPlayerNum(), 0));
        this.surplusCardRecordList = new ArrayList<ArrayList<Integer>>();
        this.zhaNiao = new ArrayList<Boolean>(Collections.nCopies(this.room.getPlayerNum(), false)); //红桃十扎鸟
        this.xiaoGuanList = new ArrayList<Boolean>(Collections.nCopies(this.room.getPlayerNum(), false));
        if(getSetID()==1){
            if(room.getRoomCfg().getFangJianXianShi()!=0){
                for (AbsRoomPos con : room.getRoomPosMgr().getRoomPosList()) {
                    ((XCPDKRoomPos)con).setSecTotal(getTime(room.getRoomCfg().getFangJianXianShi()) * 1000);
                }
            }
        }
        // 回放记录添加游戏配置
        this.addGameConfig();
        this.recordRoomPosPointBeginStart();
        //初始化解散次数
        initDissolveCount();
        this.startSet();
        //8大对
        if(eightPos>=0){
            if(m_OpPos!=eightPos){
                room.getRoomPosMgr().getPosByPosID(m_OpPos).setLatelyOutCardTime(0);
            }
            //这家伙设置成他的回合
            setStatus(XCPDK_GameStatus.XCPDK_GAME_STATUS_WaitingEx,false);
        }else{
            //设置成正常打牌阶段
            setStatus(XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD,false);
        }
    }

    private boolean eightDui(int relativePos,boolean firstTurn) {
        int endPos = !firstTurn?(m_OpPos + 1)%room.getPlayerNum():-1;
        for(int i=0;i<room.getPlayerNum();i++){
            int nextPos = (relativePos + 1+i)%room.getPlayerNum();
            if(nextPos==endPos){
                //第n次循环没人能进入8大队，进入正常打牌
                this.eightPos = -1;
                return false;
            }
            XCPDKRoomPos roomPos = (XCPDKRoomPos) room.getRoomPosMgr().getPosByPosID(nextPos);
            Map<Integer, Long> cardGroup = roomPos.getPrivateCards().stream().collect(Collectors.groupingBy(BasePocker::getCardValueEx, Collectors.counting()));
            int sum = cardGroup.values().stream().mapToInt(z -> z >= 4 ? 2 : (z >= 2 ? 1 : 0)).sum();
            if(sum==8){
                this.eightPos = nextPos;
                room.getRoomPosMgr().getPosByPosID(eightPos).setLatelyOutCardTime(CommTime.nowMS());
                return true;
            }
        }
        this.eightPos = -1;
        //第一次循环没有人能8大队
        return false;
    }

    private void initSportsPointList() {
        if (RoomTypeEnum.UNION.equals(this.room.getRoomTypeEnum())) {
            this.sportsPointList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), 0D));
        }
    }
    /**
     * 初始化位置
     */
    protected void initSetPos() {
        this.posDict = new ConcurrentHashMap<>(this.room.getPlayerNum());
        for (int posId = 0; posId < this.room.getPlayerNum(); posId++) {
            this.getPosDict().put(posId, new XCPDKSetPos(posId, this.room.getRoomPosMgr().getPosByPosID(posId), this));
        }
    }

    public Map<Integer, XCPDKSetPos> getPosDict() {
        return posDict;
    }
    /**
     * 标识Id
     *
     * @return
     */
    @Override
    public int getTabId() {
        return this.room.getTabId();
    }

    @Override
    public boolean update(int sec) {
        boolean isClose = false;
        switch (this.getStatus()) {
            case XCPDK_GAME_STATUS_SENDCARD:
                if (!checkFaPai() || CommTime.nowMS() - this.startMS >= this.getWaitTimeByStatus()) {
                    this.onSendCardEnd(sec);
                }
                break;
            case XCPDK_GAME_STATUS_COMPAER_SECOND:
                if (this.curRound == null) {
                    if (!this.startNewRound()) {
                        this.endSet();
                    }
                } else if (this.curRound != null) {
                    boolean isRoundClosed = this.curRound.update(sec);
                    if (isRoundClosed) {
                        if (this.curRound.isSetEnd() || !this.startNewRound()) {
                            this.endSet();
                        }
                    }
                }
                break;
            case XCPDK_GAME_STATUS_WaitingEx:
                if (CommTime.nowMS() - this.startMS >= 15000) {
                    if(eightPos>=0){
                        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(eightPos);
                        if (Objects.nonNull(roomPos)) {
                            opBaDaDui(null, roomPos.getPid(), CXCPDK_BaDaDui.make(room.getRoomID(), 1));
                        }
                    }
                }
                break;
            case XCPDK_GAME_STATUS_RESULT:
                isClose = true;
                cleanEndSetRoom();
                break;
            default:
                break;
        }

        return isClose;
    }

    @Override
    public void clear() {
        this.room = null;
        if (null != this.setCard) {
            this.setCard.clean();
            this.setCard = null;
        }
        this.cleanEndSetRoom();
        this.addDouble = null;
        this.pointList = null;
        this.resultCalcList = null;
        this.surplusCardList = null;
        this.surplusCardRecordList = null;
        this.openCardList = null;
        this.beShutDowList = null;
        this.m_FirstOpVic = null;
        this.m_RobCloseVic = null;
        this.m_ReverseRobCloseVic = null;
        this.m_RoomDoubleList = null;
        this.setEnd = null;
        this.robCloseList = null;
        this.hMap = null;
        this.cardList = null;
        this.totalPointResult = null;
    }

    /**
     * 清空结束房间当前局
     */
    public void cleanEndSetRoom() {
        // 清空回合记录
        if (null != this.historyRound) {
            this.historyRound.forEach(key -> {
                if (null != key) {
                    key.clean();
                }
            });
            this.historyRound.clear();
            this.historyRound = null;
        }
        // 清空当前回合
        if (null != this.curRound) {
            this.curRound.clean();
            this.curRound = null;
        }
        // 房间回放
        if (null != this.roomPlayBack) {
            this.roomPlayBack.clear();
            this.roomPlayBack = null;
        }
    }

    @Override
    public void clearBo() {
        this.bo = null;

    }

    /**
     * 发牌
     */
    public void onSendCardEnd(int sec) {
        this.setStatus(XCPDK_GameStatus.XCPDK_GAME_STATUS_COMPAER_SECOND,true);

        boolean isRobCloseSuccess = false;

        // 红桃十翻倍
        if (this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_HONGTAO10FANBEI)) {
            XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
            for (int i = 0; i < this.room.getPlayerNum(); i++) {
                XCPDKRoomPos roomPos = (XCPDKRoomPos) roomPosMgr.getPosByPosID(i);
                int count = BasePockerLogic.getCardCount(roomPos.cards(), HONGTAO10FANBEI, false);
                for (int j = 0; j < count; j++) {
                    this.addDoubleNum(i, HONGTAO10FANBEIADDDOUBLE);
                }
            }
        }

        //红桃十扎鸟
        if (this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_HONGTAO10ZHANIAO)) {
            XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
            for (int i = 0 ; i < this.room.getPlayerNum() ; i++) {
                XCPDKRoomPos roomPos = (XCPDKRoomPos) roomPosMgr.getPosByPosID(i);
                boolean hasDouble = roomPos.cards().stream().anyMatch(n->n==HONGTAO10FANBEI);
                zhaNiao.set(i,hasDouble);
            }
        }

        // 抢关门可以改变首出的玩家
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_QIANGGUANMEN)) {
            this.room.getRoomPosMgr().notify2All(SXCPDK_ChangeStatus.make(this.room.getRoomID(), this.status.value(), this.m_OpPos, isRobCloseSuccess));
            return;
        }

        if (this.getRobClosePlayerNum() > 0) {
            Victory vic = new Victory(-1, -1);
            int index = gameRandom.nextInt(this.room.getPlayerNum());
            for (int i = 0; i < this.room.getPlayerNum(); i++) {
                index = (index + 1) % this.room.getPlayerNum();
                if (this.getRobCloseByPos(i) <= 0) {
                    continue;
                }
                if (-1 == vic.getPos() || (vic.getNum() < this.getAddDoubleNum(i) + this.getOpenCard(i))) {
                    vic.setPos(i);
                    vic.setNum(this.getAddDoubleNum(i) + this.getOpenCard(i));
                }
            }
            setOpPos(vic.getPos());
            m_FirstOpVic.setPos(m_OpPos);
            isRobCloseSuccess = true;

            this.m_RobCloseVic.setPos(this.m_OpPos);
            this.m_RobCloseVic.setNum(XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value());
        }

        this.room.getRoomPosMgr().notify2All(SXCPDK_ChangeStatus.make(this.room.getRoomID(), this.status.value(), this.m_OpPos, isRobCloseSuccess));
    }

    /**
     * 结算
     */
    public void resultCalc() {

        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_XUEZHANDAODI)) {
            this.resultCalcEx();
        }

        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos iRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            iRoomPos.setMaxPoint(this.pointList.get(i));
            if (this.checkExistPrizeType(PrizeType.Gold)) {
                this.pointList.set(i, this.pointList.get(i) * this.room.getBaseMark());
            }
        }

        this.room.setLastWinPos(this.calWinPos());
        int winPos = this.room.getLastWinPos();

        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            if(winPos < 0){
                roomPos.addFlat(1);
            }else if (i == winPos) {
                roomPos.addWin(1);
            } else {
                roomPos.addLose(1);
            }
        }
    }

    /*
     * 血战到底 先算部分
     **/
    public void resultCalcEx() {
        this.calcSurplusCardList();
        XCPDKGameResult result = new XCPDKGameResult(this.room);
        result.resultCalc();
        List<Victory> roomDoubleList = getRoomDoubleList();

        if (XCPDK_define.BombScore.Not_ADD.has(room.getRoomCfg().zhadanfenshu)) {
            this.m_RoomDoubleList.add(0);
            roomDoubleList.forEach(victory -> victory.setNum(0));
        }else{
            this.m_RoomDoubleList
                    .add(roomDoubleList.stream().map(Victory::getNum).reduce(0, (left, right) -> left + right));
        }

//        // 大赢家
//        if (XCPDK_define.BombAlgorithm.WINNER.has(this.room.getRoomCfg().zhadansuanfa)) {
//            //roomDoubleList.forEach(victory -> victory.setNum(victory.getPos() != getWinPos() ? 0 : victory.getNum()));
//            this.m_RoomDoubleList
//                    .add(roomDoubleList.stream().map(Victory::getNum).reduce(0, (left, right) -> left + right));
//            // 炸弹归每轮最大玩家
//        } else if (XCPDK_define.BombAlgorithm.GETROUNDALLBOMB.has(this.room.getRoomCfg().zhadansuanfa)) {
//            this.m_RoomDoubleList
//                    .add(getRoomDoubleList().stream().map(Victory::getNum).reduce(0, (left, right) -> left + right));
//            // 有炸就算
//        } else if (XCPDK_define.BombAlgorithm.ALWAYS.has(this.room.getRoomCfg().zhadansuanfa)) {
//            this.m_RoomDoubleList
//                    .add(getRoomDoubleList().stream().map(Victory::getNum).reduce(0, (left, right) -> left + right));
//            // 炸弹不算分，移除所有炸弹
//        } else {
//            this.m_RoomDoubleList.add(0);
//            roomDoubleList.forEach(victory -> victory.setNum(0));
//        }

        //CommLogD.info("resultCalcEx 00000 pointList=" + this.pointList.toString());

        if (!m_bRobCloseCalc) {
//            int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
//            for (int i = 0; i < this.room.getPlayerNum(); i++) {
//                /*
//                 * if (i == this.getFirstOpPos() && this.surplusCardList.get(i)
//                 * == this.getFirstOpNum()) { this.beShutDowList.set(i, true); }
//                 * else
//                 */
//                if (this.surplusCardList.get(i) == cardNum) {
//                    this.beShutDowList.set(i, true);
//                }
//            }

            int robClosePoint = 0;
            if (this.room.getConfigMgr().getGuDingFen() == this.room.getRoomCfg().resultCalc) {
                robClosePoint = this.room.getConfigMgr().getRobClosePointByGuDingFen() * this.room.getPlayerNum();
            } else {
                robClosePoint = this.room.getConfigMgr().getRobClosePointByNotGuDingFen();
            }
            // int robCloseAddDouble =
            // this.room.getConfigMgr().getRobCloseAddDouble();

            // 抢关门失败 没有被反关门
            if (m_RobCloseVic.getPos() >= 0
                    && m_RobCloseVic.getNum() != XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()
                    && m_ReverseRobCloseVic.getPos() >= 0
                    && m_ReverseRobCloseVic.getNum() != XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()) {
                for (int i = 0; i < this.room.getPlayerNum(); i++) {
                    if (i == m_RobCloseVic.getPos()) {
                        continue;
                    }
                    this.pointList.set(i, this.pointList.get(i) + robClosePoint);
                    this.pointList.set(m_RobCloseVic.getPos(),
                            this.pointList.get(m_RobCloseVic.getPos()) - robClosePoint);
                }
                // 抢关门失败 被反关门
            } else if (m_RobCloseVic.getPos() >= 0
                    && m_RobCloseVic.getNum() != XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()
                    && m_ReverseRobCloseVic.getPos() >= 0
                    && m_ReverseRobCloseVic.getNum() == XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()) {
                for (int i = 0; i < this.room.getPlayerNum(); i++) {
                    if (i == m_RobCloseVic.getPos()) {
                        continue;
                    }
                    if (i == m_ReverseRobCloseVic.getPos()) {
                        int point = this.pointList.get(m_RobCloseVic.getPos());
                        this.pointList.set(i, this.pointList.get(i)
                                + robClosePoint /*
                         * + Math.abs( point
                         * )*(robCloseAddDouble -1)
                         */);
                        this.pointList.set(m_RobCloseVic.getPos(), point/** robCloseAddDouble */
                                - robClosePoint);
                    } else {
                        int point = this.pointList.get(i);
                        this.pointList.set(i, point + robClosePoint);
                        this.pointList.set(m_RobCloseVic.getPos(),
                                this.pointList.get(m_RobCloseVic.getPos()) - robClosePoint);
                    }
                }
                // 被反关门
            } else if (m_RobCloseVic.getPos() < 0 && m_ReverseRobCloseVic.getPos() >= 0
                    && m_ReverseRobCloseVic.getNum() == XCPDK_ROBCLOSE_STATUS.XCPDK_ROBCLOSE_STATUS_SUCCESS.value()) {
                // int point =this.pointList.get(m_FirstOpVic.getPos()) ;
                // this.pointList.set(m_ReverseRobCloseVic.getPos(),
                // this.pointList.get(m_ReverseRobCloseVic.getPos()) + Math.abs(
                // point ) *(robCloseAddDouble - 1));
                // this.pointList.set(m_FirstOpVic.getPos(),
                // point*robCloseAddDouble );
            }
            m_bRobCloseCalc = true;
        }
        //CommLogD.info("resultCalcEx 11111 pointList=" + this.pointList.toString());
    }

    /*
     * 设置状态
     */
    public void setStatus(XCPDK_GameStatus state,boolean needCheck) {
        if (needCheck && this.status == state) {
            return;
        }
        this.status = state;
        this.startMS = CommTime.nowMS();
        int nextPos = -1;
        if(XCPDK_GameStatus.XCPDK_GAME_STATUS_WaitingEx.equals(state)){
            nextPos = eightPos;
        }else if(XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD.equals(state) || XCPDK_GameStatus.XCPDK_GAME_STATUS_COMPAER_SECOND.equals(state)){
            nextPos = m_OpPos;
        }
        this.room.getRoomPosMgr().notify2All(SXCPDK_ChangeStatus.make(this.room.getRoomID(), this.getStatus().value(),nextPos,false));
    }

    /*
     * 获取状态
     */
    public XCPDK_GameStatus getStatus() {
        return this.status;
    }

    // 开启新的回合
    public boolean startNewRound() {
        if (this.curRound != null) {
            this.historyRound.add(this.curRound);
        }

        this.curRound = new XCPDKRoomSetSound(this); // 开启第一轮
        return this.curRound.tryStartRound();
    }

    /**
     * 开始设置
     */
    public void startSet() {
        // 设置参与游戏的玩家
        for (AbsRoomPos pos : this.room.getRoomPosMgr().getPosList()) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) pos;
            if ((pos.isReady() && this.room.getCurSetID() == 1) || (this.room.getCurSetID() > 1 && pos.getPid() != 0)) {
                roomPos.setPlayTheGame(true);
            }
        }
        // 初始化位置
        this.initSetPos();
        // 洗底牌
        this.setCard = new XCPDKSetCard(this.room);
        // 是否开启神牌模式
        if (room.isGodCard()) {
            godCard();
        }
        for (int i = 0; i < this.room.getXiPaiList().size(); i++) {
            this.setCard.onXiPai();
        }
        this.room.getXiPaiList().clear();
        ;
        if (this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_LAIZI)) {
            this.m_Razz = BasePockerLogic.getRandomRazz(PockerListType.POCKERLISTTYPE_TWOEND);
        }

        int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
        for (int j = 0; j < this.room.getPlayerNum(); j++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID((m_OpPos + j) % this.room.getPlayerNum());
            // 如果是DEBUG模式发送神牌
            if (room.isGodCard()) {
                roomPos.init(hMap.get(j));
            } else {
                roomPos.init(this.setCard.popList(cardNum));
            }
        }

        this.setDefeault();
        m_FirstOpVic.setPos(m_OpPos);
        this.startMS = CommTime.nowMS();
        boolean enterEightDui = this.eightDui(m_OpPos, true);
        if(enterEightDui){
            this.status = XCPDK_GameStatus.XCPDK_GAME_STATUS_WaitingEx;
        }

        XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
        // 开始发牌
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            long pid = this.room.getRoomPosMgr().getPosByPosID(i).getPid();
            if (0 == i) {
                this.getRoomPlayBack().playBack2Pos(i, SXCPDK_SetStart.make(this.room.getRoomID(), this.getNotify_set(pid)), roomPosMgr.getAllPlayBackNotify());
            } else {
                this.room.getRoomPosMgr().notify2Pos(i, SXCPDK_SetStart.make(this.room.getRoomID(), this.getNotify_set(pid)));
            }
        }
        this.room.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
    }

    /**
     * 练习场结算
     */
    private void goldEnd(int posID, int shui) {
        if (this.checkExistPrizeType(PrizeType.Gold)) {
            XCPDKRoomPos pos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(posID);
            if (!RobotMgr.getInstance().isRobot((int) pos.getPid())) {
                Player player = PlayerMgr.getInstance().getPlayer(pos.getPid());
                player.getFeature(PlayerCurrency.class).goldRoomEnd(shui, room.getBaseMark(),
                        this.room.getBaseRoomConfigure().getGameType().getId());
            } else {
                RobotMgr.getInstance().freeRobot((int) pos.getPid());
            }
        }
    }

    @Override
    public XCPDKRoomSetInfo getNotify_set(long pid) {
        XCPDKRoomSetInfo ret = new XCPDKRoomSetInfo();
        ret.setSetID(this.room.getCurSetID());
        ret.roomID = this.room.getRoomID();
        ret.startTime = this.startMS;
        ret.runWaitSec = (CommTime.nowMS() - startMS)/1000;
        ret.state = this.status.value();
        ret.opPos = m_OpPos;
        ret.eightPos = eightPos;
        ret.razz = m_Razz;
        ret.firstOpCard = m_FirstOpCard;
        ret.firstOpPos = m_FirstOpVic.getPos();
        ret.isFirstOp = m_bFirstOp;
        ret.roomDoubleList = m_RoomDoubleList;
        ret.robCloseVic = m_RobCloseVic;
        ret.reverseRobCloseVic = m_ReverseRobCloseVic;
        if (null != this.curRound) {
            ret.lastOpPos = this.curRound.getLastOpPos();
            ret.opType = this.curRound.getOpCardType();
            ret.cardList = this.curRound.getCardList();
            ret.opPos = this.curRound.getOpPos();
            ret.isSetEnd = this.curRound.isSetEnd();
        }

        if (this.status == XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT) {
            List<List<Integer>> privateList = new ArrayList<>();
            for (AbsRoomPos pos : room.getRoomPosMgr().posList) {
                ((XCPDKRoomPos) pos).cards().sort(BasePockerLogic.sorterBigToSmallNotTrump);
                privateList.add(pos.getPid() == pid ? List.copyOf(((XCPDKRoomPos) pos).cards()) : List.of());
            }
            List<Integer> bombList = new ArrayList<>();
            for (int i = 0; i < room.getPlayerNum(); i++) {
                bombList.add(i < this.getRoomDoubleList().size() ? this.getRoomDoubleList().get(i).getNum() : 0);
            }
            SXCPDK_SetEnd end = SXCPDK_SetEnd.make(this.room.getRoomID(), this.status.value(), this.startMS,
                    this.m_RoomDoubleList, this.getFirstOpPos(), this.m_RobCloseVic, this.m_ReverseRobCloseVic,
                    this.pointList, this.surplusCardList, bombList, this.beShutDowList, this.bo!=null?this.bo.getPlayBackCode():0, cardList, privateList, true, this.sportsPointList,this.totalPointResult,xiaoGuanList);
            ret.setEnd = end;
        }

        // 每个玩家的牌面
        ret.posInfo = new ArrayList<>();
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            RoomPosInfo room_Pos = this.room.getRoomPosMgr().getNotify_PosList().get(i);
            int setPoint = this.pointList.get(i);
            XCPDKRoomSet_Pos roomSet_Pos = new XCPDKRoomSet_Pos();
            roomSet_Pos.posID = room_Pos.getPos();
            roomSet_Pos.pid = room_Pos.getPid();
            roomSet_Pos.cards = roomPos.getNotifyCard(pid, this.getOpenCard(i) == 1);
            roomSet_Pos.addDouble = this.getAddDoubleNum(i);
            roomSet_Pos.openCard = this.getOpenCard(i);
            roomSet_Pos.robClose = this.getRobCloseByPos(i);
            roomSet_Pos.point = this.pointList.get(i);
            roomSet_Pos.beShutDow = this.beShutDowList.get(room_Pos.getPos());
            roomSet_Pos.sportsPoint = roomPos.setSportsPoint(setPoint);
            roomSet_Pos.sportsPoint1 = roomPos.getRoomSportsPoint();
            roomSet_Pos.setSecTotal(getTime1(i));
            for (ArrayList<Integer> sizeList : surplusCardRecordList) {
                roomSet_Pos.surplusCardList.add(sizeList.get(room_Pos.getPos()));
            }
            ret.posInfo.add(roomSet_Pos);
        }

        return ret;
    }

    @Override
    public XCPDKRoom_SetEnd getNotify_setEnd() {
        return setEnd;
    }

    // 设置所有玩家都准备进行下一场游戏
    public void setAllGameReady(boolean flag) {
        if (this.room.getCurSetID() >= this.room.getRoomCfg().getSetCount()) {
            return;
        }
        for (AbsRoomPos pos : this.room.getRoomPosMgr().getPosList()) {
            if (pos.getPid() != 0) {
                pos.setGameReady(flag);
                if (flag) {
                    pos.setLatelyOutCardTime(0L);
                }
            }
        }
    }

    /**
     * 结算积分
     */
    public void calcPoint() {
        GameSetBO gameSetBO = ContainerMgr.get().getComponent(GameSetBOService.class).findOne(room.getRoomID(), this.room.getCurSetID());
        this.bo = gameSetBO == null ? new GameSetBO() : gameSetBO;
        if (gameSetBO == null) {
            bo.setRoomID(room.getRoomID());
            bo.setSetID(this.room.getCurSetID());
            bo.setTabId(this.room.getTabId());
        }

        this.resultCalc();
        this.onlyWinRightNowPoint();
        this.calYiKaoPoint();
        this.getPosDict().values().forEach(k -> calcPosPoint(k));
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            XCPDKSetPos setPos = this.posDict.get(i);
            XCPDKRoom_PosEnd posEnd = roomPos.calcPosEnd(setPos);
            goldEnd(i, this.pointList.get(i));
            if (CollectionUtils.isNotEmpty(this.sportsPointList)) {
                this.sportsPointList.set(posEnd.pos, Objects.isNull(posEnd.sportsPoint) ? 0D : posEnd.sportsPoint);
            }
            this.setEnd.posResultList.add(posEnd);
            totalPointResult.add(roomPos.getPoint());
        }

        room.getRoomPosMgr().setAllLatelyOutCardTime();

        this.setEnd.roomDoubleList = this.m_RoomDoubleList;
        this.setEnd.endTime = CommTime.nowSecond();

        XCPDKRoom_SetEnd lSetEnd = this.getNotify_setEnd();

        String gsonSetEnd = new Gson().toJson(lSetEnd);
        bo.setDataJsonRes(gsonSetEnd);
        bo.setEndTime(setEnd.endTime);
        bo.setPlayBackCode(getPlayBackDateTimeInfo().getPlayBackCode());
        bo.getBaseService().saveOrUpDate(bo);
    }

    public void calcPosPoint(XCPDKSetPos mSetPos) {
        int point = mSetPos.getEndPoint() + mSetPos.getDeductPoint();
        mSetPos.setEndPoint(point);
        mSetPos.setDeductEndPoint(CommMath.addDouble(mSetPos.getDeductEndPoint() ,mSetPos.getDeductPointYiKao()));
    }

    /**
     * 亲友圈竞技点不能输到0分
     */
    @SuppressWarnings("Duplicates")
    public void calYiKaoPoint() {
        // 各个位置的算分情况map
        double beiShu = Math.max(0D, this.room.getRoomTyepImpl().getSportsDouble());
        //各个位置的输分情况
        Map<Integer, Double> pointMap = new HashMap<>();
        //各个位置的最多能输多少分
        Map<Integer, Double> sportPointMap = new HashMap<>();
        if(!(room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()))){
            //身上多少分赢多少分
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                k.setDeductPoint(pointList.get(k.getPosID()));
                k.setDeductPointYiKao(CommMath.mul(k.getDeductPoint(), beiShu));
            });
        }
        // 初始化一科计算分数所需要的map
        this.getPosDict().values().forEach(k -> {
            pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint(), beiShu));
            sportPointMap.put(k.getPosID(), k.getRoomPos().getRoomSportsPoint());
        });
        XCPDKSetPos sPos=this.getPosDict().values().stream().max(Comparator.comparingInt(m->m.getDeductPoint())).get();
        int huPos = sPos.getPosID();
        // 亲友圈竞技点不能输到0分
        if (room.isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.room.getRoomTypeEnum()) && huPos >= 0) {
            Double totalWinPoint = 0D;//输的总分
            for (int i = 0; i < room.getPlayerNum(); i++) {
                XCPDKSetPos setPos = this.getPosDict().get(i);
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
            for (int i = 0; i < room.getPlayerNum(); i++) {
                XCPDKSetPos setPos = this.getPosDict().get(i);
                Double winPoint = setPos.getDeductPointYiKao();//12
                if (totalWinPoint <= 0) {
                    for (int j = i; j < room.getPlayerNum(); j++) {
                        XCPDKSetPos setPos2 = this.getPosDict().get(j);
                        if (setPos2.getDeductPointYiKao() >= 0) {
                            setPos2.setDeductPointYiKao(0);
                        }
                    }
                    break;
                }
                if (winPoint > 0) {
                    if (totalWinPoint < winPoint) {
                        setPos.setDeductPointYiKao(totalWinPoint);
                        totalWinPoint = CommMath.subDouble(totalWinPoint,totalWinPoint);
                    } else {
                        totalWinPoint = CommMath.subDouble(totalWinPoint,winPoint);
                    }
                }
            }

        }
    }
    @Override
    public void endSet() {
        if (this.status == XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT) {
            return;
        }
        if (!business.global.billing.LegacySettlementGuard.beginRound(
                this.room.getRoomID(), this.room.getCurSetID(), "xcpdk-v1")) {
            return;
        }

        this.setStatus(XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT,true);
        setEnd(true);
        this.calcPoint();
        this.checkEndConditions();
        setTrusteeshipAutoDissolution();
        setPlayBackCode();
        for (AbsRoomPos pos : room.getRoomPosMgr().posList) {
            ((XCPDKRoomPos) pos).cards().sort(BasePockerLogic.sorterBigToSmallNotTrump);
        }
        roomPlayBack();
        List<Integer> bombList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), 0));
        for (Victory victory : this.getRoomDoubleList()) {
            bombList.set(victory.getPos(), victory.getNum());
        }
        if (this.room.getDissolveRoom() == null) {
            // 广播
            XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) room.getRoomPosMgr();
            for (int viewerPos = 0; viewerPos < room.getPlayerNum(); viewerPos++) {
                getRoomPlayBack().playBack2Pos(viewerPos, SXCPDK_SetEnd.make(this.room.getRoomID(), this.status.value(), this.startMS,
                        this.m_RoomDoubleList, this.getFirstOpPos(), this.m_RobCloseVic, this.m_ReverseRobCloseVic,
                        this.pointList, this.surplusCardList, bombList, this.beShutDowList, this.bo.getPlayBackCode(),
                        cardList, cardViewFor(viewerPos), isRoomEnd() || room.isEnd, this.sportsPointList,
                        this.totalPointResult, xiaoGuanList), roomPosMgr.getAllPlayBackNotify());
            }
        }

    }

    /**
     * 判断结束条件
     */
    private void checkEndConditions() {
        //同时勾选了这两个玩法 比赛分有小等于0的  房间直接结束
        if(room.isOnlyWinRightNowPoint()&&room.isRulesOfCanNotBelowZero()){
            if(RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())){
                for (int i = 0; i < room.getPlayerNum(); i++) {
                    AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(i);
                    if(roomPos.getRoomSportsPoint()<=0){
                        room.isEnd = true;
                    }
                }
            }
        }
    }

    /**
     * 设置回放码。
     */
    protected void setPlayBackCode() {
        if (this.room.getBaseRoomConfigure().getPrizeType() == PrizeType.RoomCard) {
            this.playBackCode = getPlayBackDateTimeInfo().getPlayBackCode();
        }
    }

    /**
     * 获取房间回放记录
     *
     * @return
     */
    public RoomPlayBack getRoomPlayBack() {
        if (null == this.roomPlayBack) {
            this.roomPlayBack = new XCPDKRoomPlayBackImpl(this.room);
        }
        return this.roomPlayBack;
    }

    /**
     * 如果是房卡类型，才需要回放记录
     */
    public void roomPlayBack() {
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            PlayBackData playBackData = new PlayBackData(this.room.getRoomID(),
                    this.room.getCurSetID(), 0, this.room.getCount(),
                    this.room.getRoomKey(),
                    this.room.getBaseRoomConfigure().getGameType().getId(), getPlayBackDateTimeInfo());
            this.getRoomPlayBack().addPlayBack(playBackData);
        }
    }

    /**
     * 回放记录谁发起解散
     */
    @Override
    public void addDissolveRoom(BaseSendMsg baseSendMsg) {
        if (this.status == XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT) {
            return;
        }
        XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
        this.getRoomPlayBack().addPlaybackList(baseSendMsg, roomPosMgr.getAllPlayBackNotify());
    }

    /**
     * 回放记录添加游戏配置
     */
    @Override
    public void addGameConfig() {
        this.getRoomPlayBack().addPlaybackList(SXCPDK_Config.make(room.getCfg(), this.room.getRoomTyepImpl().getRoomTypeEnum()), null);
    }

    // 托管
    public void roomTrusteeship(int pos) {
        XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);
        if (this.getStatus() == XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD && roomPos.isRobot()) {
            this.checkAiOpenCard(pos);
            return;
        }
        if (null != this.curRound) {
            this.curRound.roomTrusteeship(pos);
        }
    }

    public void onAddDouble(WebSocketRequest request, CXCPDK_AddDouble addDouble) {
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_KEJIABEI)) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "onAddDouble error:do not add double");
            }
            return;
        }

        if (this.status != XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD) {
            if (null != request) {
                request.error(ErrorCode.NotAllow,
                        "onAddDouble error:state is not  XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD  state:"
                                + this.status);
            }
            return;
        }

        if (this.getAddDoubleNum(addDouble.pos) >= 0) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "onAddDouble error:you already add double");
            }
            return;
        }
        if (null != request) {
            request.response();
        }
        if (XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT != this.status) {
            this.addDoubleNum(addDouble.pos, addDouble.addDouble + 1);
        }
        this.getRoomPlayBack().playBack2All(SXCPDK_AddDouble.make(addDouble.roomID, addDouble.pos, addDouble.addDouble));
    }

    public void onOpenCard(WebSocketRequest request, CXCPDK_OpenCard openCard) {

        if (this.status == XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD) {
            if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_KEMINGPAI)) {
                if (null != request) {
                    request.error(ErrorCode.NotAllow, "onOpenCard error:do not open card");
                }
                return;
            }

            if (this.status != XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD) {
                if (null != request) {
                    request.error(ErrorCode.NotAllow,
                            "onOpenCard error:state is not  XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD  state:"
                                    + this.status);
                }
                return;
            }
        } else if (XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT == this.status) {

        } else {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "onOpenCard error:state is not  can open card  state:" + this.status);
            }
            return;
        }

        if (this.getOpenCard(openCard.pos) >= 0) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "onOpenCard error:you already opened");
            }
            return;
        }

        if (null != request) {
            request.response();
        }
        XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(openCard.pos);
        if(Objects.isNull(roomPos)) {
            CommLogD.error("XCPDK RoomId:{},RoomKey:{},Pos:{}",this.room.getRoomID(),this.room.getRoomKey(),openCard.pos);
            return;
        }
        this.addOpenCard(openCard.pos, openCard.OpenCard);
        if (openCard.OpenCard == 1) {
            if (XCPDK_GameStatus.XCPDK_GAME_STATUS_RESULT != this.status) {
                this.addDoubleNum(openCard.pos, OPENCARDDOUBLE);
            }
        }
        this.getRoomPlayBack().playBack2All(
                SXCPDK_OpenCard.make(openCard.roomID, openCard.pos, openCard.OpenCard, roomPos.cards()));
    }

    private List<List<Integer>> cardViewFor(int viewerPos) {
        List<List<Integer>> view = new ArrayList<>(room.getPlayerNum());
        for (int pos = 0; pos < room.getPlayerNum(); pos++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) room.getRoomPosMgr().getPosByPosID(pos);
            view.add(pos == viewerPos ? List.copyOf(roomPos.cards()) : List.of());
        }
        return view;
    }

    public void onRobClose(WebSocketRequest request, CXCPDK_RobClose robClose) {
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_QIANGGUANMEN)) {
            request.error(ErrorCode.NotAllow, "onRobClose error:do not XCPDK_WANFA_QIANGGUANMEN");
            return;
        }

        if (this.status != XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD) {
            request.error(ErrorCode.NotAllow,
                    "onRobClose error:state is not  XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD  state:" + this.status);
            return;
        }

        if (this.getRobCloseByPos(robClose.pos) >= 0) {
            request.error(ErrorCode.NotAllow, "onRobClose error:you already rob close pos");
            return;
        }

        request.response();
        this.addRobCloseByPos(robClose.pos, robClose.robClose);
        this.getRoomPlayBack().playBack2All(SXCPDK_RobClose.make(robClose.roomID, robClose.pos, robClose.robClose));
    }

    /*
     * 加倍倍数
     */
    public int getAddDoubleNum(int pos) {
        return this.getNumByList(this.addDouble, pos);
    }

    /*
     * 加倍倍数
     */
    public void addDoubleNum(int pos, int Num) {
        this.addNumByList(this.addDouble, pos, Num);
    }

    /*
     * 翻牌
     */
    public int getOpenCard(int pos) {
        return this.getNumByList(this.openCardList, pos);
    }

    /*
     * 翻牌倍数
     */
    public void addOpenCard(int pos, int Num) {
        this.addNumByList(this.openCardList, pos, Num);
    }

    /*
     * 抢关门
     */
    public int getRobCloseByPos(int pos) {
        return this.getNumByList(this.robCloseList, pos);
    }

    /*
     * 抢关门倍数
     */
    public void addRobCloseByPos(int pos, int Num) {
        this.addNumByList(this.robCloseList, pos, Num);
    }

    /*
     * 获取list上对应的值
     */
    @SuppressWarnings("finally")
    public int getNumByList(ArrayList<Victory> list, int pos) {
        int flag = -1;
        try {
            for (Victory vic : list) {
                if (null != vic && vic.getPos() == pos) {
                    flag = vic.getNum();
                    break;
                }
            }
            // CommLogD.info("getNumByList num:%s ,pos:%s", flag, pos);
            // if(this.openCardList.size() > 0) {
            // CommLogD.info("this.getNumByList.toString():%s",
            // list.toString());
            // }
        } catch (Exception e) {
            CommLogD.error("getNumByList error:" + e);
        }
        return flag;
    }

    /*
     * 设置list中对应的值
     */
    public void addNumByList(ArrayList<Victory> list, int pos, int Num) {
        boolean flag = false;
        try {
            if (null != list && list.size() > 0) {
                for (Victory vic : list) {
                    if (null == vic) {
                        continue;
                    }
                    if (pos == vic.getPos()) {
                        int count = 0;
                        if (Num != 0) {
                            count = vic.getNum() != 0 ? vic.getNum() : 1;
                        }
                        Num = Num != 0 ? Num : 1;
                        vic.setNum(Num * count);
                        flag = true;
                    }
                }
            }
            if (!flag) {
                if (list == null) {
                    list = new ArrayList<Victory>();
                }
                Victory vic = new Victory(pos, Num);
                list.add(vic);
            }
        } catch (Exception e) {
            CommLogD.error("addNumByList error:", e);
        }
        return;
    }

    /*
     * 获取抢关门玩家个数
     **/
    public int getRobClosePlayerNum() {
        int count = 0;
        for (Victory victory : this.robCloseList) {
            if (null != victory && victory.getNum() > 0) {
                count++;
            }
        }
        return count;
    }

    // 获取阶段时间
    public int getWaitTimeByStatus() {
        int waitTime = 0;
        if(atOnceStart){
            return 0;
        }
        switch (this.status) {
            case XCPDK_GAME_STATUS_SENDCARD:
//                int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().cardNum);
//                waitTime = 100 * cardNum * this.room.getPlayerNum() + 1000;
//                if (this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_KEJIABEI)
//                        || this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_QIANGGUANMEN)) {
//                    waitTime += 15000;
//                }
                waitTime += 15000;
                break;
            // case XCPDK_GAME_STATUS_COMPAER_ONE:
            // waitTime = 5000;
            // break;
            case XCPDK_GAME_STATUS_COMPAER_SECOND:
                waitTime = 15000;
                break;
            case XCPDK_GAME_STATUS_RESULT:
                waitTime = 10000;
                break;
            default:
                break;
        }
        return waitTime;
    }

    /*
     * 默认位置
     **/
    public void setDefeault() {
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_SHOUJUHEITAO3) || this.room.getCurSetID() > 1) {
            setOpPos(this.room.getLastWinPos());
            this.m_FirstOpCard = 0;
            return;
        }

        XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
        List<Integer> permissionsList = new ArrayList<>(Arrays.asList(2, 1, 0, 3));
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < BasePocker.ONE_COLOR_POCKER_COUNT; j++) {
                int card = BasePocker.PockerList_TWOEnd[j + permissionsList.get(i) * BasePocker.ONE_COLOR_POCKER_COUNT];
                int pos = roomPosMgr.checkCard(card);
                if (pos >= 0) {
                    setOpPos(pos);
                    m_FirstOpCard = card;
                    return;
                }
            }
        }
    }

    /**
     * @return curRound
     */
    public XCPDKRoomSetSound getCurRound() {
        return curRound;
    }

    /**
     * 设置神牌
     */

    // 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
    // 0x0F, //方块3~2
    // 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E,
    // 0x1F, //梅花3~2
    // 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E,
    // 0x2F, //红桃3~2
    // 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E,
    // 0x3F, //黑桃3~2
    private void godCard() {
        if (!room.isGodCard()) {
            return;
        }
        if(room.isDecimalism()){
            ArrayList<Integer> card1 = getGodCard(BasePockerLogic.deleteCardDecimalism(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card1()));
            ArrayList<Integer> card2 = getGodCard(BasePockerLogic.deleteCardDecimalism(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card2()));
            ArrayList<Integer> card3 = getGodCard(BasePockerLogic.deleteCardDecimalism(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card3()));
            ArrayList<Integer> card4 = getGodCard(BasePockerLogic.deleteCardDecimalism(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card4()));
            hMap.put(0, card1);
            hMap.put(1, card2);
            hMap.put(2, card3);
            hMap.put(3, card4);
            return;
        }
        boolean flag1 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), (ArrayList<Integer>) room.getConfigMgr().getPrivate_Card1());
        boolean flag2 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), (ArrayList<Integer>) room.getConfigMgr().getPrivate_Card2());
        boolean flag3 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), (ArrayList<Integer>) room.getConfigMgr().getPrivate_Card3());
        boolean flag4 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), (ArrayList<Integer>) room.getConfigMgr().getPrivate_Card4());
        if (flag1 && flag2 && flag3 && flag4) {
            ArrayList<Integer> card1 = getGodCard(room.getConfigMgr().getPrivate_Card1());

            ArrayList<Integer> card2 = getGodCard(room.getConfigMgr().getPrivate_Card2());

            ArrayList<Integer> card3 = getGodCard(room.getConfigMgr().getPrivate_Card3());

            ArrayList<Integer> card4 = getGodCard(room.getConfigMgr().getPrivate_Card4());

            hMap.put(0, card1);
            hMap.put(1, card2);
            hMap.put(2, card3);
            hMap.put(3, card4);
        } else {
            this.setCard.randomCard();
            int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
            hMap.put(0, this.setCard.popList(cardNum));
            hMap.put(1, this.setCard.popList(cardNum));
            hMap.put(2, this.setCard.popList(cardNum));
            hMap.put(3, this.setCard.popList(cardNum));
        }
    }

    /**
     * @return m_OpPos
     */
    public int getOpPos() {
        return m_OpPos;
    }

    /**
     * @param m_OpPos 要设置的 m_OpPos
     */
    public void setOpPos(int m_OpPos) {
        XCPDKRoomPos tempRoomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(m_OpPos);
        tempRoomPos.setLatelyOutCardTime(CommTime.nowMS());
        this.m_OpPos = m_OpPos;
    }

    /**
     * @return m_Razz
     */
    public int getRazz() {
        if (m_Razz == -1) {
            CommLogD.error("getRazz error: m_Razz == -1");
        }
        return m_Razz;
    }

    /**
     * @param m_bFirstOp 要设置的 m_bFirstOp
     */
    public void setFirstOp(boolean m_bFirstOp) {
        this.m_bFirstOp = m_bFirstOp;
    }

    /**
     * @return m_bFirstOp
     */
    public boolean isFirstOp() {
        return m_bFirstOp;
    }

    /**
     * @return m_RobClosePos
     */
    public int getRobClosePos() {
        return m_RobCloseVic.getPos();
    }

    /**
     */
    public void setRobCloseNum(int num) {
        this.m_RobCloseVic.setNum(num);
    }

    /**
     * @return m_RobClosePos
     */
    public int getRobCloseNum() {
        return this.m_RobCloseVic.getNum();
    }

    /**
     * @return m_ReverseRobClosePos
     */
    public int getReverseRobClosePos() {
        return m_ReverseRobCloseVic.getPos();
    }

    /**
     */
    public void setReverseRobCloseNum(int num) {
        this.m_ReverseRobCloseVic.setNum(num);
    }

    /**
     */
    public void setReverseRobClosePos(int pos) {
        this.m_ReverseRobCloseVic.setPos(pos);
    }

    /**
     * @return m_RoomDouble
     */
    public int getRoomDouble(int pos) {
        return Math.max(1, this.m_RoomDouble);
    }

    /**
     */
    public void addRoomDouble(int pos, int roomAddDouble) {
        if (this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_MAXZHADAN)) {
            if (this.m_AddRoomDoubleCount >= this.room.getConfigMgr().getMaxRoomAddDouble()) {
                return;
            } else {
                this.m_AddRoomDoubleCount++;
            }
        }
        this.m_RoomDouble *= roomAddDouble;
    }

    /*
     * 检查机器人是否明牌
     */
    public void checkAiOpenCard(int pos) {
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_KEMINGPAI)) {
            return;
        }
        if (this.getOpenCard(pos) >= 0) {
            return;
        }
        int aiOpenCard = this.room.getConfigMgr().getAiOpenCard();
        if (gameRandom.nextInt(100) > 100 - aiOpenCard) {
            this.onOpenCard(null, CXCPDK_OpenCard.make(this.room.getRoomID(), pos, 1));
        } else {
            this.onOpenCard(null, CXCPDK_OpenCard.make(this.room.getRoomID(), pos, 0));
        }
    }

    /*
     * 检查机器人是否加倍
     */
    public void checkAiAddDouble(int pos) {
        if (!this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_KEJIABEI)) {
            return;
        }
        if (this.getAddDoubleNum(pos) >= 0) {
            return;
        }
        int aiAddDouble = this.room.getConfigMgr().getAiAddDouble();
        ArrayList<Integer> addDoubleList = this.room.getConfigMgr().getAddDoubleList();
        if (gameRandom.nextInt(100) > aiAddDouble) {
            this.onAddDouble(null, CXCPDK_AddDouble.make(this.room.getRoomID(), pos,
                    addDoubleList.get(gameRandom.nextInt(addDoubleList.size()))));
        } else {
            this.onAddDouble(null, CXCPDK_AddDouble.make(this.room.getRoomID(), pos, 0));
        }
    }

    /**
     * @return m_FirstOpPos
     */
    public int getFirstOpPos() {
        return m_FirstOpVic.getPos();
    }

    /**
     * @return m_FirstOpPos
     */
    public int getFirstOpNum() {
        return m_FirstOpVic.getNum();
    }

    /**
     * @return m_FirstOpPos
     */
    public void setFirstOpNum(int num) {
        m_FirstOpVic.setNum(num);
    }

    /*
     * 计算剩余牌数
     **/
    @SuppressWarnings("unchecked")
    public void calcSurplusCardList() {
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            if (roomPos.cards().size() > 0) {
                this.surplusCardList.set(i, roomPos.cards().size());
            } else {
                this.surplusCardList.set(i, 0);
            }
        }
        ArrayList<Integer> temp = (ArrayList<Integer>) surplusCardList.clone();
        this.surplusCardRecordList.add(temp);
    }

    /**
     * @return m_bRobCloseCalc
     */
    public boolean isRobCloseCalc() {
        return m_bRobCloseCalc;
    }

    /**
     * 获取赢家
     */
    public int getWinPos() {
        Victory vic = new Victory(-1, -1);
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (-1 == vic.getPos() || (vic.getNum() < this.pointList.get(i))) {
                vic.setPos(i);
                vic.setNum(this.pointList.get(i));
            }
        }
        return vic.getPos();
    }

    /**
     * 结算赢家
     *
     * @return
     */
    public int calWinPos() {
        int winPos = -1;
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            if (roomPos.cards().size() <= 0) {
                winPos = i;
                break;
            }
        }

        if (winPos == -1) return -1;
        return winPos;
    }

    /**
     * 获取房间加倍
     *
     * @return
     */
    protected List<Victory> getRoomDoubleList() {
        return addDouble;
    }

    /**
     * 插入牌 并在牌堆里面删除
     */
    public ArrayList<Integer> getGodCard(ArrayList<Integer> list) {
        if (!room.isGodCard()) {
            return new ArrayList<Integer>();
        }
        int cardNum = this.room.getConfigMgr().getHandleCard().get(this.room.getRoomCfg().shoupai);
        ArrayList<Integer> cardList = new ArrayList<Integer>(cardNum);
        cardList.addAll(list);
        if(!room.isDecimalism()){
            int count = cardNum - cardList.size();
            ArrayList<Integer> tempList = this.setCard.popList(count);
            BasePockerLogic.deleteCard(this.setCard.getLeftCards(), tempList);
            cardList.addAll(tempList);
        }
        return cardList;
    }

    /**
     * 新增到操作链
     *
     * @param cardList
     * @param opCardType
     * @param opCardType
     */
    public void addOpCardList(List<Integer> cardList, int opCardType, int pos) {
        if (opCardType != XCPDK_define.XCPDK_CARD_TYPE.XCPDK_CARD_TYPE_BUCHU.value()) {
            this.cardList.add(SPDK_OutCardList.make(pos, opCardType, cardList));
        }
    }

    /**
     * 检查是否存在指定消耗类型
     *
     * @return
     */
    @Override
    public boolean checkExistPrizeType(PrizeType prizeType) {
        return prizeType.equals(this.room.getBaseRoomConfigure().getPrizeType());
    }

    /**
     * 房间是否结束
     * @return
     */
    public boolean isRoomEnd(){
        boolean roomEnd = false;
        if (this.room.getDissolveRoom() != null) {
            roomEnd = true;
        }else if(room.getCurSetID() >= room.getCount()){
            roomEnd = true;
        }
        return roomEnd;
    }

    /**
     * 判断是否可以发牌 检查没有发送发牌消息的人有几个  有一个存在都不能发牌
     * @return
     */
    private boolean checkFaPai() {
        return room.getRoomPosMgr().getPosList().stream().filter(k->!((XCPDKRoomPos)k).getBeginFlag()).count()>0;
    }

    /**
     * 接收客户端消息 判断是否进入游戏
     * @param request
     * @param pos
     */
    public void faPaiJieShu(WebSocketRequest request, int pos) {
        if(this.getStatus() != XCPDK_define.XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD && this.getStatus() != XCPDK_define.XCPDK_GameStatus.XCPDK_GAME_STATUS_WaitingEx){
            if(null != request) {
                request.error(ErrorCode.NotAllow, "faPaiJieShu error:state is not  can   state:"+this.getStatus());
            }
            return;
        }
        XCPDKRoomPos roomPos = (XCPDKRoomPos) room.getRoomPosMgr().getPosByPosID(pos);
        if(roomPos==null){
            if(null != request) {
                request.error(ErrorCode.NotAllow, "roomPos is null posID:"+pos);
            }
            return;
        }
        roomPos.setBeginFlag(true);
    }

    /**
     * 设置解散次数
     */
    private void initDissolveCount() {
        room.getRoomPosMgr().posList.stream().forEach(n -> n.setDissolveCount(0));
    }

    /**
     * 小局托管自动解散
     */
    public void setTrusteeshipAutoDissolution() {
        // 检查小局托管自动解散
        if (room.isSetAutoJieSan()) {
            // 获取托管玩家pid列表
            List<Long> trusteeshipPlayerList = room.getRoomPosMgr().getRoomPosList().stream().filter(n -> n.isTrusteeship()).map(n -> n.getPid()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(trusteeshipPlayerList)) {
                // 记录回放中
                getRoomPlayBack().addPlaybackList(SXCPDK_DissolveTrusteeship.make(room.getRoomID(), trusteeshipPlayerList, CommTime.nowSecond()), null);
                room.setTrusteeshipDissolve(true);
            }
        }else if(room.isSetAutoJieSan2()){
            // 有玩家连续2局托管
            // 获取托管玩家pid列表
            List<Long> trusteeshipPlayerList = room.getRoomPosMgr().getRoomPosList().stream()
                    .filter(n -> n.isTrusteeship() && ((XCPDKRoomPos)n).getTuoGuanSetCount() >= 2).map(n -> n.getPid()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(trusteeshipPlayerList)) {
                // 记录回放中
                getRoomPlayBack().addPlaybackList(SXCPDK_DissolveTrusteeship.make(room.getRoomID(), trusteeshipPlayerList, CommTime.nowSecond()), null);
                room.setTrusteeshipDissolve(true);
            }
        }
    }

    /**
     * 发牌的时候记录分数
     * 只赢当前身上分的时候要用
     */
    @Override
    public  void  recordRoomPosPointBeginStart(){
        if(this.room.isOnlyWinRightNowPoint()&&RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())){
            for (int i = 0; i < room.getPlayerNum(); i++) {
                AbsRoomPos roomPos = this.room.getRoomPosMgr().getPosByPosID(i);
                roomPos.setGameBeginSportsPoint(roomPos.getRoomSportsPoint());
            }
        }
    }

    /**
     * 亲友圈竞技点不能输到0分
     */
    @SuppressWarnings("Duplicates")
    public void onlyWinRightNowPoint() {
        // 各个位置的算分情况map
        double beiShu = Math.max(0D, room.getRoomTyepImpl().getSportsDouble());
        //各个位置的输分情况
        Map<Integer, Double> pointMap = new HashMap<>();
        //临时存储玩家最多输赢分
        Map<Integer, Double> deductPointTemp = new HashMap<>();
        //最后玩家应该输赢的竞技点
        Map<Integer, Double> resultPointTemp = new HashMap<>();
        //各个位置的最多能赢多少分
        Map<Integer, Double> sportPointMap = new HashMap<>();
        XCPDKSetPos sPos=this.getPosDict().values().stream().max(Comparator.comparingInt(m->m.getDeductPoint())).get();
        int huPos = sPos.getPosID();
        if(room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())){
            //身上多少分赢多少分
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                k.setDeductPoint(pointList.get(k.getPosID()));
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
                if(k.getDeductPointYiKao()>0){
                    resultPointTemp.put(k.getPosID(),k.getDeductPointYiKao());
                }else{
                    resultPointTemp.put(k.getPosID(),0D);
                }
            });
        } else if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) && huPos >= 0) {
            // 初始化一科计算分数所需要的map
            this.getPosDict().values().forEach(k -> {
                pointMap.put(k.getPosID(), CommMath.mul(k.getDeductPoint(), beiShu));
                sportPointMap.put(k.getPosID(), k.getRoomPos().getGameBeginSportsPoint());
                //更新临时存储值
                deductPointTemp.put(k.getPosID(), k.getDeductPointYiKao());
                if(k.getDeductPointYiKao()>0){
                    resultPointTemp.put(k.getPosID(),k.getDeductPointYiKao());
                }else{
                    resultPointTemp.put(k.getPosID(),0D);
                }
            });
        }

        //如果全部都在正常范围，按正常算分
        boolean allMatch = this.getPosDict().entrySet().stream().allMatch(i -> {
            XCPDKSetPos setPos = i.getValue();
            Double losePoint = setPos.getDeductPointYiKao();
            return setPos.getRoomPos().getGameBeginSportsPoint()<0 || CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint()) <= 0D;
        });
        if(allMatch){
            return;
        }

        boolean isFirst = false;
        // 只能赢自己身上的分数
        if (room.isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) && huPos >= 0) {
            for (int i = 0; i < room.getPlayerNum(); i++) {
                XCPDKSetPos setPos = this.getPosDict().get(i);
                Double losePoint = setPos.getDeductPointYiKao();
                //没有赢分的话 不进行考虑
                if (losePoint <= 0) {
                    continue;
                }
                //需要扣减的分数
                Double needSubPoint = CommMath.subDouble(losePoint, setPos.getRoomPos().getGameBeginSportsPoint());
                //有需要扣减的话进行扣减
                if (needSubPoint > 0D) {
                    Map<Integer, Double> resultMap = this.subPointByOnlyWinRightNowPoint(deductPointTemp,setPos,isFirst,!isFirst?this.getExceptOneOtherPoint():0);
                    for (int j = 0; j < this.getPosDict().size(); j++) {
                        if(resultMap.get(j)<0){
                            //分数进行增减
                            deductPointTemp.put(j,CommMath.addDouble(deductPointTemp.get(j),Math.abs(resultMap.get(j))));
                            resultPointTemp.put(j,CommMath.addDouble(resultPointTemp.get(j),resultMap.get(j)));
                        }else{
                            if(j==setPos.getPosID()){
                                resultPointTemp.put(j,resultMap.get(j));
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
     * 获取除了某个玩家外 其他玩家 分数超过带多少赢的
     * @return
     */
    private Double getExceptOneOtherPoint(){
        Double totalValue=new Double(0D);
        for (int i = 0; i < room.getPlayerNum(); i++) {
            XCPDKSetPos absMJSetPos = this.getPosDict().get(i);
            if (absMJSetPos.getDeductPointYiKao() <= 0  ||
                    absMJSetPos.getDeductPointYiKao() > absMJSetPos.getRoomPos().getGameBeginSportsPoint()) {
                continue;
            }
            totalValue=CommMath.addDouble(totalValue,absMJSetPos.getDeductPointYiKao());
        }
        return totalValue;
    }

    /**
     * @param setPos       多赢的那个人
     * @param isFirst
     */
    private Map<Integer, Double> subPointByOnlyWinRightNowPoint(Map<Integer, Double> temp,XCPDKSetPos setPos, boolean isFirst,Double otherPoint) {
        Map<Integer, Double> resultMap =  new HashMap<>(temp);
//        Map<Integer, Double> resultMap = new HashMap<>();
//        for (int i = 0; i < room.getPlayerNum(); i++) {
//            XCPDKSetPos absMJSetPos = this.getPosDict().get(i);
//            resultMap.put(absMJSetPos.getPosID(), absMJSetPos.getDeductPointYiKao());
//        }
        //传递参数使用
        Map<Integer, Double> resultMap2 = new HashMap<>(resultMap);

        Long newPlayerNum = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).count();
        Double beginGamePoint = CommMath.addDouble(setPos.getRoomPos().getGameBeginSportsPoint(),otherPoint);
        Double newMin = CommMath.div(-beginGamePoint, newPlayerNum.intValue());
        Double minValue = resultMap.entrySet().stream().filter(k -> k.getValue() < 0).
                sorted(Comparator.comparing(k -> -k.getValue())).map(k -> k.getValue()).findFirst().orElse(0D);
        Map<Integer, Double> calcMap = this.diGuiMethod(beginGamePoint, resultMap2, minValue, newMin);

        Double addDouble = 0D;
        for (Map.Entry<Integer, Double> con : calcMap.entrySet()) {
            if (con.getValue() > 0) {
                if (con.getKey() == setPos.getPosID()) {
                    resultMap.put(con.getKey(), CommMath.subDouble(beginGamePoint,otherPoint));
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
        double difference = CommMath.subDouble(levelNum,CommMath.mul(everyCount, count));
        for (Map.Entry<Integer, Double> con : calcMap.entrySet()) {
            if (con.getValue() < 0) {
                double point = -everyCount;
                if(difference!=0){
                    point = CommMath.addDouble(point, -difference);
                    difference=0;
                }
                resultMap.put(con.getKey(), point);
            }
        }
        return resultMap;
//        for (int i = 0; i < room.getPlayerNum(); i++) {
//            XCPDKSetPos absMJSetPos = this.getPosDict().get(i);
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

    public long getTime(int xianshi) {
        switch (xianshi) {
            case 0:
                return 99999;
            case 1:
                return 8*60;
            case 2:
                return 10*60;
            case 3:
                return 12*60;
            case 4:
                return 15*60;
            case 5:
                return 20*60;
        }
        return 180;
    }

    public int getTime1(int pos){
        if (room.getRoomCfg().getFangJianXianShi() != 0) {//不是罚分玩法 用过的时间
            XCPDKSetPos mjSetPos = posDict.get(pos);
            // 自己 或 公开
            long total = getTime(room.getRoomCfg().getFangJianXianShi())*1000;
            //除了本回合，之前的所有回合跑了多少秒
            long useTime2 = ((XCPDKRoomPos) mjSetPos.getRoomPos()).getSecTotal();
            int useTime1 = (int)((total - useTime2)/1000);
            XCPDKRoomSetSound curRound = getCurRound();
            if(curRound!=null){
                if(curRound.getOpPos()==pos){
                    //本回合跑了多少秒
                    int useTime = (int)((CommTime.nowMS() - startMS)/1000);
                    //总的跑多少(本回合+之前所有回合跑的时间)，让客户端自己拿剩余多少秒去减
                    return useTime1 + useTime;
                }
            }
            return useTime1;
        }
        return -1;
    }

    /**
     * 八大队
     */
    public synchronized void opBaDaDui(WebSocketRequest request, long pid, CXCPDK_BaDaDui data) {
        if (!this.getStatus().equals(XCPDK_GameStatus.XCPDK_GAME_STATUS_WaitingEx)) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "badadui cur setstate=" + this.getStatus());
            }
            return;
        }
        XCPDKRoomPos roomPos = (XCPDKRoomPos) this.room.getRoomPosMgr().getPosByPid(pid);
        if(eightPos!=roomPos.getPosID()){
            if (null != request) {
                request.error(ErrorCode.NotAllow, "badadui cur pos=" + roomPos.getPosID());
            }
            return;
        }
        //8大对
        XCPDKRoomPosMgr roomPosMgr = (XCPDKRoomPosMgr) this.room.getRoomPosMgr();
        boolean enterEightDui = this.eightDui(eightPos, false);
        if(data.dui>0){
            eightPos=-1;
        }
        SXCPDK_BaDaDui baDaDui = SXCPDK_BaDaDui.make(data.roomID, data.dui,roomPos.getPosID(), eightPos);
        this.getRoomPlayBack().playBack2Pos(roomPos.getPosID(), baDaDui, roomPosMgr.getAllPlayBackNotify());
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (i == roomPos.getPosID()) {
                continue;
            }
            this.room.getRoomPosMgr().notify2Pos(i, baDaDui);
        }
        // 清除最近出手的时间
        roomPos.setLatelyOutCardTime(0L);
        if(data.dui>0){
            int fou1ZhaTypeValue = new XCPDKRoomSetSound(this).getFou1ZhaTypeValue(roomPos.getPrivateCards());
            if(fou1ZhaTypeValue>20){
                List<XCPDK_Bomb> bombs = new ArrayList<>();
                bombs.add(new XCPDK_Bomb(roomPos.getPosID(),new ArrayList<>(roomPos.getPrivateCards()),true,true));
                bombList.add(bombs);
            }
            addOpCardList(new ArrayList<>(roomPos.getPrivateCards()),XCPDK_define.XCPDK_CARD_TYPE.XCPDK_WANFA_BADADUI.value(),roomPos.getPosID());
            for (int i = 0; i < this.room.getPlayerNum(); i++) {
                if (0 == i) {
                    getRoomPlayBack().playBack2Pos(i, SXCPDK_OpCard.make(room.getRoomID(), roomPos.getPosID(), XCPDK_define.XCPDK_CARD_TYPE.XCPDK_WANFA_BADADUI.value(), m_OpPos, roomPos.getPrivateCards(), true, 0, true,new ArrayList<>(),false,0,0,0,getTrusteeshipList()), roomPosMgr.getAllPlayBackNotify());
                } else {
                    this.room.getRoomPosMgr().notify2Pos(i, SXCPDK_OpCard.make(room.getRoomID(),roomPos.getPosID(), XCPDK_define.XCPDK_CARD_TYPE.XCPDK_WANFA_BADADUI.value(), m_OpPos, roomPos.getPrivateCards(), true, 0, true,new ArrayList<>(),false,0,0,0,getTrusteeshipList()));
                }
            }
            roomPos.getPrivateCards().clear();
            this.endSet();
        }else{
            if (!enterEightDui) {
                atOnceStartGame();
            }
        }
    }

    /**
     * 获取托管列表
     * @return
     */
    public List<Boolean> getTrusteeshipList(){
        return room.getRoomPosMgr().getPlayingPos().stream().map(AbsRoomPos::isTrusteeship).collect(Collectors.toList());
    }

    /**
     * 设置为init状态
     */
    public void atOnceStartGame() {
        setStatus(XCPDK_GameStatus.XCPDK_GAME_STATUS_SENDCARD,true);
        room.getRoomPosMgr().getPosByPosID(m_OpPos).setLatelyOutCardTime(CommTime.nowMS());
        atOnceStart = true;
    }

    public void addNewRoomDouble(int pos, int bombNum,int bombScore){

    }

}
