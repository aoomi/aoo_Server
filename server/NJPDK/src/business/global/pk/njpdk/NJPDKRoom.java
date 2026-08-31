package business.global.pk.njpdk;

import business.global.room.RoomRecordMgr;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.global.room.base.RoomTyepImpl;
import business.global.room.pk.PockerRoom;
import business.njpdk.c2s.cclass.NJPDKRoomSetInfo;
import business.njpdk.c2s.cclass.NJPDK_define;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_WANFA;
import business.njpdk.c2s.iclass.*;
import cenum.ChatType;
import cenum.ClassType;
import cenum.RoomTypeEnum;
import cenum.room.GaoJiTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.BasePocker;
import jsproto.c2s.cclass.pk.PKRoom_Record;
import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.S_GetRoomInfo;
import jsproto.c2s.iclass.room.SBase_Dissolve;
import jsproto.c2s.iclass.room.SBase_PosLeave;

import java.util.ArrayList;
import java.util.List;

/**
 * 安岳跑的快游戏房间
 */
public class NJPDKRoom extends PockerRoom {
    public CNJPDK_CreateRoom cfg;// 开房配置
    private NJPDKConfigMgr configMgr = new NJPDKConfigMgr();
    protected int lastWinPos = -1;//记录上局赢家

    protected NJPDKRoom(BaseRoomConfigure<CNJPDK_CreateRoom> baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
        initShareBaseCreateRoom(CNJPDK_CreateRoom.class, baseRoomConfigure);
        this.cfg = (CNJPDK_CreateRoom) baseRoomConfigure.getBaseCreateRoom();
    }

    @Override
    public String dataJsonCfg() {
        // 获取房间配置
        return new Gson().toJson(this.getRoomCfg());
    }

    /**
     * 获取房间配置
     *
     * @return
     */
    public CNJPDK_CreateRoom getRoomCfg() {
        if (this.cfg == null) {
            initShareBaseCreateRoom(CNJPDK_CreateRoom.class, getBaseRoomConfigure());
            return (CNJPDK_CreateRoom) getBaseRoomConfigure().getBaseCreateRoom();
        }
        return this.cfg;
    }

    /**
     * 房间内每个位置信息 管理器
     */
    @Override
    public AbsRoomPosMgr initRoomPosMgr() {
        return new NJPDKRoomPosMgr(this);
    }


    @Override
    public void startNewSet() {
        this.setCurSetID(this.getCurSetID() + 1);
        // 每个位置，清空准备状态
        this.getRoomPosMgr().clearGameReady();
        this.createSet();
        // 通知局数变化
        this.getRoomTyepImpl().roomSetIDChange();
        this.setAutoDismiss(false);
    }

    @Override
    public void cancelTrusteeship(AbsRoomPos pos) {
        ((NJPDKRoomSet) this.getCurSet()).roomTrusteeship(pos.getPosID());
    }

    @Override
    public boolean isCanChangePlayerNum() {
        return false;
    }

    @Override
    public <T> T getCfg() {
        return (T) getRoomCfg();
    }

    /**
     * 托管时间值
     *
     * @return
     */
    @Override
    public int trusteeshipTimeValue() {
        return NJPDK_define.NJPDKXianShi.valueOf(getBaseRoomConfigure().getBaseCreateRoom().getXianShi()).value();
    }

    /**
     * 清空当前局，创建新小局
     */
    public void createSet() {
        if (null != this.getCurSet()) {
            this.getCurSet().clear();
            this.setCurSet(null);
        }
        this.setCurSet(new NJPDKRoomSet(this));
        //清空包赔信息
        getRoomPosMgr().posList.stream().forEach(pos -> {
            ((NJPDKRoomPos) pos).isBaoPei = false;
            ((NJPDKRoomPos) pos).type = 0;
            ((NJPDKRoomPos) pos).isBaoDan = false;
        });
    }

    public int getNextGuan(int pos) {
        int currentOpPos = pos;
        for (int i = 0; i < getPlayerNum(); i++) {
            currentOpPos = (currentOpPos + 1) % getPlayerNum();
            NJPDKRoomPos tempRoomPos = (NJPDKRoomPos) this.getRoomPosMgr().getPosByPosID(currentOpPos);
            if (tempRoomPos.canGuan) {
                return currentOpPos;
            }
        }
        return -1;
    }

    public void guanpai(NJPDKRoomPos roomPos) {
        //特殊玩法，3A12直接大关
        roomPos.type = 1;
        if (null != getCurSet()) {
            getCurSet().endSet();
        }
    }

    public boolean checkQuanDui(List<Integer> cards) {
        Integer card = null;
        for (Integer integer : cards) {
            if (null == card) {
                card = integer;
            } else {
                int value = BasePocker.getCardValue(card);
                int value1 = BasePocker.getCardValue(integer);
                if (value == value1) {
                    List<Integer> newCards = new ArrayList<>(cards);
                    newCards.remove(card);
                    newCards.remove(integer);
                    if (0 == newCards.size()) {
                        return true;
                    }
                    return checkQuanDui(newCards);
                }
            }
        }
        return false;
    }


    public boolean check10Xiao(List<Integer> cards) {
        for (Integer integer : cards) {
            if (BasePocker.getCardValue(integer) > 10) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void roomTrusteeship(int pos) {
        if (getCurSet() != null && ((NJPDKRoomSet) getCurSet()).getCurRound() != null) {
            ((NJPDKRoomSet) this.getCurSet()).roomTrusteeship(pos);
        }
    }

    @Override
    public boolean needAtOnceOpCard() {
        return true;
    }

    @Override
    public void setEndRoom() {
        if (null != this.getCurSet()) {
            if (getHistorySet().size() > 0) {
                // 增加房局记录
                RoomRecordMgr.getInstance().add(this);
                this.getRoomPosMgr().notify2All(SNJPDK_RoomEnd.make(this.getPKRoomRecordInfo()));
                refererReceiveList();
            }
        }
    }

    /**
     * 构建房间回放返回给客户端
     *
     * @return 通知结构体
     */
    public PKRoom_Record getPKRoomRecordInfo() {
        PKRoom_Record pkRoom_record = new PKRoom_Record();
        pkRoom_record.setCnt = this.getHistorySetSize();
        pkRoom_record.recordPosInfosList = this.getRecordPosInfoList();
        pkRoom_record.roomID = this.getRoomID();
        pkRoom_record.endSec = this.getGameRoomBO().getEndTime();
        pkRoom_record.roomKey = getRoomKey();
        return pkRoom_record;
    }

    /**
     * 获取位置输赢信息
     *
     * @return
     */
    @Override
    protected List<PKRoom_RecordPosInfo> getRecordPosInfoList() {
        List<PKRoom_RecordPosInfo> sRecord = new ArrayList<>();
        for (int i = 0; i < this.getPlayerNum(); i++) {
            PKRoom_RecordPosInfo posInfo = new PKRoom_RecordPosInfo();

            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.getRoomPosMgr().getPosByPosID(i);
            posInfo.flatCount = roomPos.getFlat();
            posInfo.loseCount = roomPos.getLose();
            posInfo.winCount = roomPos.getWin();

            posInfo.point = roomPos.getPoint();
            posInfo.pos = i;
            posInfo.pid = roomPos.getPid();
            posInfo.setMaxPoint = roomPos.maxPoint;
            posInfo.setPoint = roomPos.getPoint() - roomPos.bombPoint;
            posInfo.bombPoint = roomPos.bombPoint;
            posInfo.setSportsPoint(roomPos.sportsPoint());
            sRecord.add(posInfo);
        }
        return sRecord;
    }

    public NJPDKConfigMgr getConfigMgr() {
        return configMgr;
    }

    /**
     * 获取牌型玩法
     *
     * @param wanFa
     * @return
     */
    public boolean isWanFaByType(NJPDK_WANFA wanFa) {
        return this.getRoomCfg().paixing.contains(wanFa.value());
    }

    /**
     * 获取房间人数
     */
    @Override
    public int getPlayerNum() {
        return this.getRoomCfg().getPlayerNum();
    }

    @Override
    public void clearEndRoom() {
        super.clear();
        this.configMgr = null;
        this.cfg = null;
    }

    @Override
    public int getTimerTime() {
        return 500;
    }

    /**
     * 获取上一局赢家
     *
     * @return
     */
    public int getLastWinPos() {
        return this.lastWinPos;
    }

    /**
     * 设置赢家位置
     *
     * @param lastWinPos
     */
    public void setLastWinPos(int lastWinPos) {
        this.lastWinPos = lastWinPos;
    }


    @Override
    public int getPlayingCount() {
        return this.getPlayerNum();
    }

    public boolean isGodCard() {
        return this.getConfigMgr().isGodCard();
    }

    /**
     * 打牌
     *
     * @param request
     * @param opCard
     */
    public void onOpCard(WebSocketRequest request, CNJPDK_OpCard opCard) {
        try {
            lock();
            NJPDKRoomSet set = (NJPDKRoomSet) this.getCurSet();
            if (null == set) {
                request.error(ErrorCode.NotAllow, "onOpCard not set room:" + opCard.roomID);
                return;
            }
            NJPDKRoomSetRound round = set.getCurRound();
            if (null == round) {
                request.error(ErrorCode.NotAllow, "onOpCard not round room:" + opCard.roomID);
                return;
            }
            round.onOpCard(request, opCard);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        } finally {
            unlock();
        }
    }

    /**
     * 神牌消息
     *
     * @param msg
     * @param pid
     */
    @Override
    public void godCardMsg(String msg, long pid) {
    }

    @Override
    public BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship) {
        return SNJPDK_Trusteeship.make(roomID, pid, pos, trusteeship);
    }

    @Override
    public BaseSendMsg PosLeave(SBase_PosLeave posLeave) {
        return SNJPDK_PosLeave.make(posLeave);
    }

    @Override
    public BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect, boolean isShowLeave) {
        return SNJPDK_LostConnect.make(roomID, pid, isLostConnect, isShowLeave);
    }

    @Override
    public BaseSendMsg PosContinueGame(long roomID, int pos) {
        return SNJPDK_PosContinueGame.make(roomID, pos);
    }

    @Override
    public BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom) {
        return SNJPDK_PosUpdate.make(roomID, pos, posInfo, custom);
    }

    @Override
    public BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady) {
        return SNJPDK_PosReadyChg.make(roomID, pos, isReady);
    }

    @Override
    public BaseSendMsg Dissolve(SBase_Dissolve dissolve) {
        return SNJPDK_Dissolve.make(dissolve);
    }

    @Override
    public BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec) {
        return SNJPDK_StartVoteDissolve.make(roomID, createPos, endSec);
    }

    @Override
    public BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve, int endSec) {
        return SNJPDK_PosDealVote.make(roomID, pos, agreeDissolve);
    }

    @Override
    public BaseSendMsg Voice(long roomID, int pos, String url) {
        return SNJPDK_Voice.make(roomID, pos, url);
    }

    @Override
    public BaseSendMsg XiPai(long roomID, long pid, ClassType cType) {
        return SNJPDK_XiPai.make(roomID, pid, cType);
    }

    @Override
    public BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId, int quickID) {
        return SNJPDK_ChatMessage.make(pid, name, content, type, toCId, quickID);
    }

    @Override
    public <T> BaseSendMsg RoomRecord(List<T> records) {
        return SNJPDK_RoomRecord.make(records);
    }

    @Override
    public BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum) {
        return SNJPDK_ChangePlayerNum.make(roomID, createPos, endSec, playerNum);
    }

    @Override
    public BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange) {
        return SNJPDK_ChangePlayerNumAgree.make(roomID, pos, agreeChange);
    }

    @Override
    public BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType) {
        return SNJPDK_ChangeRoomNum.make(roomID, roomKey, createType);
    }

    @Override
    public GetRoomInfo getRoomInfo(long pid) {
        S_GetRoomInfo ret = new S_GetRoomInfo();
        // 设置房间公共信息
        this.getBaseRoomInfo(ret);
        if (null != this.getCurSet()) {
            ret.setSet(this.getCurSet().getNotify_set(pid));
        } else {
            ret.setSet(new NJPDKRoomSetInfo());
        }
        return ret;
    }

    @Override
    public RoomTyepImpl newUnionRoom() {
        return new NJPDKUnionRoom(this);
    }

    /**
     * 继续游戏
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result continueGame(long pid) {
        try {
            lock();
            if (!RoomState.Playing.equals(this.getRoomState())) {
                // 房间不处于游戏阶段
                return SData_Result.make(ErrorCode.NotAllow, "continueGame RoomState Playing :{%s}",
                        this.getRoomState());
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 找不到通过pid获取玩家信息
                return SData_Result.make(ErrorCode.NotAllow, "continueGame null == roomPos");
            }
            if (roomPos.isTrusteeship()) {
                // 如果玩家处于托管状态则，不能手动点击继续游戏。
                return SData_Result.make(ErrorCode.Is_Trusteeship, "continueGame isTrusteeship:{%s}",
                        roomPos.isTrusteeship());
            }
            SData_Result result = unionContinueGame(roomPos);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return SData_Result.make(result.getCode(), result.getMsg());
            }
            roomPos.setContinue();
            roomPos.setTimeSec(0);
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 联赛继续游戏
     *
     * @return
     */
    private SData_Result unionContinueGame(AbsRoomPos roomPos) {
        if (RoomTypeEnum.UNION.equals(this.getRoomTypeEnum())) {
            NJPDKUnionRoom hbmjUnionRoom = (NJPDKUnionRoom) this.getRoomTyepImpl();
            return hbmjUnionRoom.unionContinueGame(roomPos);
        } else {
            return SData_Result.make(ErrorCode.Success);
        }
    }

    /**
     * 立即开始
     *
     * @return
     */
    @Override
    public boolean atOnceStartGame() {
        return true;
    }

    @Override
    public void RobotDeal(int pos) {
        ((NJPDKRoomSet) this.getCurSet()).roomTrusteeship(pos);
    }

    /**
     * 自动准备游戏 玩家加入房间时，自动进行准备。
     */
    @Override
    public boolean autoReadyGame() {
        return getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.ZiDong.getType());
    }

    /**
     * 房主是否需要准备
     *
     * @return
     */
    @Override
    public boolean ownerNeedReady() {
        return true;
    }

    @Override
    public boolean autoStartGame() {
        return true;
    }

    /**
     * 是否禁用魔法表情
     *
     * @return
     */
    public boolean isNotGift() {
        return this.getRoomTyepImpl().getBaseCreateRoom().getGaoji().contains(GaoJiTypeEnum.NOT_GIFT.ordinal() + 1);

    }

    @Override
    public int getJieShanShu() {
        return NJPDK_define.NJPDKJieSanShu.valueOf(this.getRoomCfg().jiesancishu).value();
    }

    /**
     * 存在有玩家离开、踢出清空所有玩家准备状态
     *
     * @return T: 清空,F:不清空
     */
    public boolean existLeaveClearAllPosReady() {
        return true;
    }

    /**
     * 30秒未准备自动退出
     *
     * @return
     */
    @Override
    public boolean is30SencondTimeOut() {
        return checkGaoJiXuanXiang(GaoJiTypeEnum.SECOND_TIMEOUT_30);
    }
}
