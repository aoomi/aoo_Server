package business.global.mj.scjymj;

import business.global.mj.*;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJPiao;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJPiaoWanFa;
import business.global.room.mj.MJRoomPos;
import business.global.room.mj.MahjongRoom;
import business.scjymj.c2s.cclass.SCJYMJRoomSetEnd;
import business.scjymj.c2s.cclass.SCJYMJRoomSetInfo;
import business.scjymj.c2s.iclass.*;
import cenum.mj.MJSpecialEnum;
import cenum.room.SetState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetEnd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安岳麻将 一局游戏逻辑
 *
 * @author Huaxing
 */
public class SCJYMJRoomSet extends AbsMJSetRoom {

    // 是否最后的牌
    private boolean isLastCard = false;
    // 练习场玩家超时买子时间
    private final static int WAITINGTIME = 6000;// 等待6s
    // 练习场机器人买子时间
    private final static int INTERVAL = 3000;// 间隔3s
    @SuppressWarnings("rawtypes")
    private SCJYMJRoom aRoom;
    /**
     * 杠上炮转雨
     */
    private Map<Integer, List<Integer>> gangShangPaoMap = new HashMap<>(4);
    /**
     * 记录杠牌
     */
    private Map<Integer, List<Integer>> gangCardMap = new HashMap<>();

    // 杠位置
    private int gangPos = -1;
    /**
     * 下局庄家的位置
     */
    private int nextDPos = -1;
    /**
     * 是否摸玩牌
     */
    private boolean isHuang = false;
    /***
     * 最后4张牌
     */
    private boolean isLastFourCard = false;
    /**
     * 最后出的牌，一炮多响使用
     */
    private int lastOutCard = 0;

    @SuppressWarnings("rawtypes")
    public SCJYMJRoomSet(int setID, MahjongRoom room, int dPos) {
        super(setID, room, dPos);
        this.aRoom = (SCJYMJRoom) room;
        this.startMS = CommTime.nowMS();
        this.getRoomPlayBack().addPlaybackList(SSCJYMJ_Config.make(this.room.getCfg()), null);
        this.initSet();
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
    public int cardSize() {
        return MJSpecialEnum.SIZE_13.value();
    }

    /**
     * 当局初始化
     */
    private void initSet() {
        SCJYMJPiaoWanFa piaoWanFa = aRoom.getPiaoWanFa();
        // 开始发牌
        this.startSet();
        if (getSetID() == 1 && (SCJYMJPiaoWanFa.Piao_Net_Xuan.equals(piaoWanFa) || SCJYMJPiaoWanFa.Piao_Wai_Xuan.equals(piaoWanFa))) {
            // 初始化位置
            this.initSetPos();
            // 进入买子状态
            this.initSetState();
        } else {
            // 初始化玩家手上的牌
            this.initSetPosCard();
            setPiao();
            notify2SetStart();
            exeStartSet();
        }
    }

    /**
     * 设置漂分
     */
    private void setPiao() {
        SCJYMJPiaoWanFa piaoWanFa = aRoom.getPiaoWanFa();
        room.getRoomPosMgr().posList.stream().forEach(n -> {
            if (getSetID() == 1
                    && (SCJYMJPiaoWanFa.Piao_Net_Zuo.equals(piaoWanFa) || SCJYMJPiaoWanFa.Piao_Wai_Zuo.equals(piaoWanFa))) {
                ((SCJYMJRoomPos) n).setaPiao(SCJYMJPiao.Piao);
            }
        });
    }

    /**
     * 添加胡牌顺序
     *
     * @param huPosId 胡牌ID
     */
    public void addHuMap(int huPosId) {
        // 获取回合ID
        int roundID = this.getCurRound().getRoundID();
        Map<Integer, List<Integer>> huMap = getMHuInfo().getHuPosMap();
        // 检查是否存在
        if (huMap.containsKey(roundID)) {
            // 记录胡牌玩家列表
            huMap.get(roundID).add(huPosId);
            if (getHuCount() <= 1) {
                // 第一次胡牌,出现多人胡牌,则由点炮的玩家做庄;
                // 一炮多响时，由点炮的玩家做庄;
                setNextDPos(getLastOpInfo().getLastOpPos());
            }
        } else {
            List<Integer> list = new ArrayList<>();
            list.add(huPosId);
            huMap.put(roundID, list);
            // 计算胡几次
            this.setHuCount(getHuCount() + 1);
            if (getHuCount() <= 1) {
                // 第一次胡牌,则由胡牌玩家坐庄;
                setNextDPos(huPosId);
            }
        }
    }

    /**
     * 设置下一个庄家
     *
     * @param huPos
     */
    public void setNextDPos(int huPos) {
        // 计算胡几次
        if (getHuCount() <= 1) {
            // 第一次胡牌,则由胡牌玩家坐庄;
            this.nextDPos = huPos;
        }
    }

    @Override
    public int calcNextDPos() {
        // 庄家胡牌、流局，庄家坐庄
        if (this.getMHuInfo().getHuPos() >= 0) {
            return this.nextDPos;
        } else {
            // 流局时，庄家连庄；
            return this.dPos;
        }
    }

    @Override
    protected void calcCurSetPosPoint() {
        // 计算位置小局分数
        this.getPosDict().values().forEach(k -> ((SCJYMJSetPos) k).setPosEnd());
        this.getPosDict().values().forEach(k -> k.calcPosPoint());
        // 其他特殊结算 连庄记录
        this.calcOtherPoint();
    }

    @Override
    public void sendSetPosCard() {
        return;
    }

    @Override
    public void MJApplique(int pos) {
    }

    @Override
    public void kaiJinNotify(MJCard jinCard, MJCard jinCard2) {

    }

    /**
     * 是否最后的牌
     *
     * @return
     */
    public boolean isLastCard() {
        return isLastCard;
    }

    /**
     * 设置是否最后的牌
     *
     * @param isLastCard
     */
    public void setLastCard(boolean isLastCard) {
        this.isLastCard = isLastCard;
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
    }

    @Override
    public boolean isConfigName() {
        return true;
    }

    @Override
    protected AbsMJSetPos absMJSetPos(int posID) {
        return new SCJYMJSetPos(posID, (MJRoomPos) this.room.getRoomPosMgr().getPosByPosID(posID), this);
    }

    @Override
    protected void absMJSetCard() {
        this.setSetCard(new SCJYMJSetCard(this));
    }

    @Override
    protected AbsMJSetPosMgr absMJSetPosMgr() {
        return new SCJYMJSetPosMgr(this);
    }

    @Override
    protected <T> BaseSendMsg setStart(long roomID, T setInfo) {
        return SSCJYMJ_SetStart.make(roomID, setInfo);
    }

    @Override
    protected AbsMJSetRound nextSetRound(int roundID) {
        return new SCJYMJSetRound(this, roundID);
    }

    @Override
    protected <T> BaseSendMsg posGetCard(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos) {
        return SSCJYMJ_PosGetCard.make(roomID, pos, normalMoCnt, gangMoCnt, set_Pos, this.setCard.getRandomCard().getSize());
    }

    @Override
    protected <T> BaseSendMsg setEnd(long roomID, T setEnd) {
        return SSCJYMJ_SetEnd.make(roomID, setEnd);
    }

    /**
     * 买子操作后发牌
     */
    private void startPiaoOp() {
        // 初始化玩家手上的牌
        this.initSetPosCard();
        notify2SetStart();
        exeStartSet();
    }

    /**
     * 玩家买子操作
     *
     * @param request
     * @param opPos   操作位置
     * @param piao    买子值
     */
    @SuppressWarnings("rawtypes")
    public void opPiao(WebSocketRequest request, int opPos, SCJYMJPiao piao) {
        if (!SetState.Waiting.equals(this.getState())) {
            // 状态不正确
            if (null == request) {
                return;
            }
            request.error(ErrorCode.NotAllow, "opPiao error :{%s}", this.getState());
            return;
        }

        SCJYMJSetPos sPos = (SCJYMJSetPos) this.getMJSetPos(opPos);
        if (null == sPos) {
            // 找不到玩家
            if (null == request) {
                return;
            }
            request.error(ErrorCode.NotAllow, "opPiao null == sPos error :{%d}", opPos);
            return;
        }
        SCJYMJRoomPos roomPos = (SCJYMJRoomPos) sPos.getRoomPos();
        if (null == roomPos) {
            // 找不到玩家
            if (null == request) {
                return;
            }
            request.error(ErrorCode.NotAllow, "opPiao null == roomPos error :{%d}", opPos);
            return;
        }

        if (roomPos.getaPiao().value() >= SCJYMJPiao.Pass.value()) {
            // 已操作买子
            if (null == request) {
                return;
            }
            request.error(ErrorCode.NotAllow, "opPiao value error :{%d}", roomPos.getaPiao().value());
            return;
        }
        if (null != request) {
            request.response();
        }
        roomPos.setaPiao(piao);
        // 通知玩家买子成功。
        this.getRoom().getRoomPosMgr().notify2All(SSCJYMJ_OpPiao.make(this.getRoom().getRoomID(), opPos, roomPos.getaPiao().value()));
        // 检查是否所有玩家买子成功.
        this.checkExistNotMaiZi();
    }

    /**
     * 检查是否存在没有买子的玩家
     */
    @SuppressWarnings("rawtypes")
    public void checkExistNotMaiZi() {
        // 检查是否存在没有操作买子的玩家
        if (this.getPosDict().values().stream()
                .filter(k -> ((SCJYMJRoomPos) k.getRoomPos()).getaPiao().value() == SCJYMJPiao.Error.value()).findAny()
                .isPresent()) {
            return;
        } else {
            this.setStateInit();
        }
    }

    /**
     * 练习场买子操作
     */
    @SuppressWarnings("rawtypes")
    private void goldPiao() {
        this.getPosDict().values().forEach(k -> {
            if (SCJYMJPiao.Error.value() == ((SCJYMJRoomPos) k.getRoomPos()).getaPiao().value()) {
                SCJYMJRoomPos roomPos = (SCJYMJRoomPos) k.getRoomPos();
                if (roomPos.isLostConnect()) {
                    this.opPiao(null, k.getPosID(), SCJYMJPiao.Pass);
                } else if (CommTime.nowMS() > startMS + WAITINGTIME) {
                    this.opPiao(null, k.getPosID(), SCJYMJPiao.Pass);

                }
            }
        });
    }

    /**
     * 设置为init状态
     */
    public void setStateInit() {
        setState(SetState.Init);
        startPiaoOp();
    }

    /**
     * 初始状态
     */
    public void initSetState() {
        setState(SetState.Waiting);
    }

    /**
     * 设置setstate
     */
    @Override
    public void setState(SetState setState) {
        this.state = setState;
        this.startMS = CommTime.nowMS();
        this.room.getRoomPosMgr().notify2All(
                SSCJYMJ_ChangeStatus.make(this.getRoom().getRoomID(), this.getRoom().getCurSetID(), setState));
    }

    /**
     * 获取通知当局信息
     */
    @Override
    public SCJYMJRoomSetInfo getNotify_set(long pid) {
        SCJYMJRoomSetInfo ret = (SCJYMJRoomSetInfo) this.getMJRoomSetInfo(pid);
        return ret;
    }

    /**
     * 一局结束的信息
     */
    @Override
    public BaseMJRoom_SetEnd getNotify_setEnd() {
        SCJYMJRoomSetEnd setEndInfo = (SCJYMJRoomSetEnd) this.mRoomSetEnd();
        return setEndInfo;
    }

    /**
     * 麻将当局结算
     *
     * @return
     */
    @Override
    protected SCJYMJRoomSetEnd newMJRoomSetEnd() {
        return new SCJYMJRoomSetEnd();
    }

    /**
     * 创建新的当局麻将信息
     */
    @Override
    protected SCJYMJRoomSetInfo newMJRoomSetInfo() {
        return new SCJYMJRoomSetInfo();
    }

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
                if (this.checkAllHu() || !this.startNewRound()) {
                    this.endSet();
                }
            }
        } else if (this.state == SetState.End) {
            isClose = true;
        } else if (this.state == SetState.Waiting) {
            this.goldPiao();
        }
        return isClose;
    }

    @Override
    public void addGameConfig() {
    }

    /**
     * 血战到底结束
     *
     * @return
     */
    private boolean checkAllHu() {
        int count = (int) this.getPosDict().values().stream().filter(k -> k.isHu()).count();
        return count == (this.getPlayerNum() - 1);
    }

    /**
     * 杠Pos
     *
     * @return
     */
    public int getGangPos() {
        return gangPos;
    }

    /**
     * 设置杠位置
     *
     * @param gangPos
     */
    public void setGangPos(int gangPos) {
        this.gangPos = gangPos;
    }

    /**
     * 记录杠上炮
     *
     * @param gangId 杠ID
     */
    public void addGangShangPaoMap(int gangId) {
        int cardType = gangId >= 1000 ? gangId / 100 : gangId;
        if (this.getMHuInfo().checkRoundExistHuPos(this.getCurRound().getRoundID())) {
            this.gangShangPaoMap.put(cardType, this.getMHuInfo().getRoundHuPostList(this.getCurRound().getRoundID()));
        }
    }

    /**
     * 获取杠上炮列表
     *
     * @param cardType 牌类型
     * @return
     */
    public List<Integer> getGangShangPaoList(int cardType) {
        cardType = cardType >= 1000 ? cardType / 100 : cardType;
        return this.gangShangPaoMap.get(cardType);
    }

    /**
     * 记录杠的牌相关的算分
     *
     * @param gangId 牌型
     */
    public void addGangMap(int gangId, int exePos, int fromPos) {
        int cardType = gangId >= 1000 ? gangId / 100 : gangId;
        if (fromPos < 0) {
            List<Integer> gangPosList = this.getPosDict().values().stream().filter(k -> !k.isHu() && k.getPosID() != exePos).map(k -> k.getPosID()).collect(Collectors.toList());
            this.gangCardMap.put(cardType, gangPosList);
        } else {
            List<Integer> fromPosList = new ArrayList<>();
            fromPosList.add(fromPos);
            if (this.room.RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.Cagua)) {
                fromPosList.addAll(this.getPosDict().values().stream().filter(k -> !k.isHu() && k.getPosID() != exePos && k.getPosID() != fromPos).map(k -> k.getPosID()).collect(Collectors.toList()));
            }
            this.gangCardMap.put(cardType, fromPosList);
        }
    }

    /**
     * 移除杠牌
     */
    public void removeGang(int gangId) {
        this.gangCardMap.remove(gangId >= 1000 ? gangId / 100 : gangId);
    }

    /**
     * 获取杠玩家列表
     *
     * @param cardType 牌型
     * @return
     */
    public List<Integer> gangPosList(int cardType) {
        return this.gangCardMap.get(cardType);
    }

    public void setHuang(boolean huang) {
        this.isHuang = huang;
    }

    public boolean isHuang() {
        return isHuang;
    }

    public boolean isLastFourCard() {
        return isLastFourCard;
    }

    public void setLastFourCard(boolean lastFourCard) {
        isLastFourCard = lastFourCard;
    }

    public void setLastOutCard(int lastOutCard) {
        this.lastOutCard = lastOutCard;
    }

    public int getLastOutCard() {
        return lastOutCard;
    }
}
