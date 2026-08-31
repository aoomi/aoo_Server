package business.global.room.pk;

import business.global.room.base.AbsBaseRoom;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import jsproto.c2s.cclass.RoomEndResult;
import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.util.List;

public abstract class PockerRoom extends AbsBaseRoom {

    private int m_dPos = 0; // 庄位 逆时针排序

    /**
     * 扑克公共父类构造函数
     *
     * @param baseRoomConfigure 公共配置
     * @param roomKey           房间key
     * @param ownerID           房主ID
     */
    @SuppressWarnings("rawtypes")
    protected PockerRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
    }

    @Override
    public void initCreatePos() {
        if (this.getOwnerID() <= 0L) {
            return;
        }
        // 进入房间
        this.enterRoom(this.getOwnerID(), 0, false, null);
        if (null != this.getRoomPosMgr().getPosByPid(this.getOwnerID())) {
            this.getRoomPosMgr().getPosByPid(this.getOwnerID()).setReady(!ownerNeedReady());
        }
    }

    /**
     * 房主需要准备
     *
     * @return T:不准备,F:默认准备
     */
    public boolean ownerNeedReady() {
        return false;
    }

    /**
     * 获取庄家位置
     *
     * @return
     */
    public int getDPos() {
        return this.m_dPos;
    }

    /**
     * 设置庄家位置
     *
     * @param dPos 庄家
     */
    public void setDPos(int dPos) {
        this.m_dPos = dPos;
    }

    /**
     * 加入房间的其他条件 条件不满足不进入
     */
    @Override
    public boolean enterRoomOtherCondition(long pid) {
        return RoomState.Init.equals(this.getRoomState());
    }

    /**
     * 主动离开房间的其他条件 条件不满足不退出
     */
    @Override
    public boolean exitRoomOtherCondition(long pid) {
        return RoomState.Init.equals(this.getRoomState());
    }

    /**
     * 启动定时器
     */
    @Override
    public void startTrusteeShipTime() {
        this.getTrusteeship().startTrusteeShipTime();
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

    /**
     * 继续房间功能 如果有需要的话去游戏那边处理
     */
    @Override
    protected void continueRoom() {

    }

    /**
     * 是否是神牌模式
     */
    public boolean isGodCard() {
        return Config.DE_DEBUG();
    }

    /**
     * 获取记录位置list
     *
     * @return
     */
    protected abstract List<PKRoom_RecordPosInfo> getRecordPosInfoList();

    /**
     * 结算
     */
    @Override
    public void calcEnd() {
        // 更新游戏房间BO和更新玩家个人游戏记录BO
        this.updateGameRoomBOAndPlayerRoomAloneBO();
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public RoomEndResult getRoomEndResult() {
        RoomEndResult sRoomEndResult = this.getRoomEndResultInfo();
        if (null != sRoomEndResult) {
            return sRoomEndResult;
        }
        sRoomEndResult = new RoomEndResult();
        sRoomEndResult.setKey(this.getRoomKey());
        sRoomEndResult.setRoomId(this.getRoomID());
        sRoomEndResult.setSetId(this.getHistorySetSize());
        sRoomEndResult.setEndTime(CommTime.nowSecond());
        sRoomEndResult.setResultsList(this.getRecordPosInfoList());
        sRoomEndResult.setOwnerID(this.getOwnerID());
        this.setRoomEndResultInfo(sRoomEndResult);
        return this.getRoomEndResultInfo();
    }

    @Override
    public long getTrusteeshipUpdateTime() {
        return 1500;
    }

    @Override
    public int getTimerTime() {
        return 1000;
    }

    @Override
    public void RobotDeal(int pos) {

    }

    /**
     * 自动开始游戏 所有玩家准备好自动开始。
     */
    @Override
    public boolean autoStartGame() {
        return true;
    }

    /**
     * 自动准备游戏 玩家加入房间时，自动进行准备。
     */
    @Override
    public boolean autoReadyGame() {
        return false;
    }


    /**
     * 中途解散保存战绩
     *
     * @return T:保存战绩,F:不保存
     */
    @Override
    public boolean isMidwayDisbandmentPreservation() {
        return true;
    }

    @Override
    public boolean startGameOtherCondition(long pid) {
        return this.getRoomPosMgr().isAllReady();
    }
}
