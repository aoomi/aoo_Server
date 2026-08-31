package business.global.room.base;

import BaseCommon.CommLog;
import business.global.config.GameListConfigMgr;
import business.global.redBagActivity.ActivityManager;
import business.global.room.RoomMgr;
import business.global.room.task.AbsBaseTaskRoom;
import business.global.room.task.NoneTaskRoom;
import business.global.room.task.RobotTaskRoom;
import business.global.room.type.ClubRoom;
import business.global.room.type.NormalRoom;
import business.global.room.type.RobotRoom;
import business.global.room.type.UnionRoom;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import cenum.*;
import cenum.room.*;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.KryoUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.db.entity.clarkGame.ClubMemberBO;
import core.db.entity.clarkGame.GameRoomBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.GameRoomBOService;
import core.db.service.clarkGame.GameSetBOService;
import core.db.service.clarkGame.PlayerRoomAloneBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import core.network.proto.ChatMessage;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;
import jsproto.c2s.cclass.room.*;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionRoomSportsBigWinnerConsumeItem;
import jsproto.c2s.iclass.room.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 房间公共接口类
 *
 * @author Administrator
 */
@Data
public abstract class AbsBaseRoom implements RoomImpl, Serializable {
    /**
     * 游戏房间里面的聊天记录
     */
    private List<ChatMessage> chatList = new ArrayList<>();
    /**
     * 房间Key
     */
    private String roomKey = "";
    /**
     * 房间创建者
     */
    private long ownerID = 0;
    /**
     * 当前局数
     */
    private int curSetID = 0;
    /**
     * 当前标识Id
     */
    private int tabId;
    /**
     * 游戏房间数据库
     */
    private GameRoomBO gameRoomBO;
    /**
     * 当局游戏
     */
    private AbsRoomSet curSet;
    /**
     * 环信SDK通信信息
     */
    private RoomHXSDKChatInfo mHXSDKChatInfo = new RoomHXSDKChatInfo();
    /**
     * 房间玩家位置管理
     */
    private AbsRoomPosMgr roomPosMgr = null;
    /**
     * 解散房间管理
     */
    private DissolveRoom dissolveRoom = null;

    /**
     * 相同IP解散房间管理
     */
    private SameIpDissolveRoom sameIpDissolveRoom = null;

    /**
     * 游戏托管管理
     */
    private Trusteeship trusteeship = null;
    /**
     * 游戏的公共配置
     */
    @SuppressWarnings("rawtypes")
    private BaseRoomConfigure baseRoomConfigure;
    /**
     * 历史局
     */
    private List<AbsRoomSet> historySet = new ArrayList<>();
    /**
     * 线程管理
     */
    private AbsBaseTaskRoom task;
    /**
     * 操作切换人数
     */
    private OpChangePlayerRoom opChangePlayerRoom;
    /**
     * 房间战绩结算
     */
    @SuppressWarnings("rawtypes")
    private RoomEndResult roomEndResultInfo;

    /**
     * 特殊房间信息 亲友圈房间，大赛事房间
     */
    private RoomTyepImpl roomTyepImpl;
    /**
     * 洗牌列表
     */
    private List<Long> xiPaiList = new ArrayList<>();
    /**
     * 房间内消耗
     */
    private int consumeValue;
    /**
     * 房间中在玩的游戏人数
     */
    private int playTheGameNumber;
    /**
     * 是否已经回退房卡了
     */
    private boolean isBackRoomCard = false;
    /**
     * 是否自动解散
     */
    private boolean isAutoDismiss;
    /**
     * 是否托管解散
     */
    private boolean isTrusteeshipDissolve = false;

    /**
     * 房间解散状态
     */
    private RoomDissolutionState roomDissolutionState = RoomDissolutionState.Normal;

    /**
     * 房间里结束
     */
    private boolean isRoomEnd = false;
    /**
     * 房费实际消耗 防止大赢家出现除不尽
     */
    private double factRoomSportsConsume;
    /**
     * 存在房间结束
     */
    private AtomicBoolean existEndRoom = new AtomicBoolean(false);

    /**
     * 赛事成本
     * 大赢家区间确定
     */
    private double sportsPointCost;

    /**
     * 中至时间
     * 6.00-6.00
     */
    private String dateTimeZhongZhi;


    private double minPoint;

    private Map<Long, Double> winOrLose = new HashMap<>();
    private Map<Long, Integer> totalCount = new HashMap<>();
    private Map<Long, Integer> winCount = new HashMap<>();

    public double getMinPoint() {
        return minPoint;
    }

    public void setMinPoint(double minPoint) {
        this.minPoint = minPoint;
    }

    public Map<Long, Double> getWinOrLose() {
        return winOrLose;
    }

    public void setWinOrLose(Map<Long, Double> winOrLose) {
        this.winOrLose = winOrLose;
    }

    public Map<Long, Integer> getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Map<Long, Integer> totalCount) {
        this.totalCount = totalCount;
    }

    public Map<Long, Integer> getWinCount() {
        return winCount;
    }

    public void setWinCount(Map<Long, Integer> winCount) {
        this.winCount = winCount;
    }

    public double initMinPoint() {
        return 0;
    }

    /**
     * 锁
     */
    public void lock() {
        if (Objects.isNull(this.getTask())) {
            return;
        }
        this.getTask().lock();
    }

    /**
     * 解锁
     */
    public void unlock() {
        if (Objects.isNull(this.getTask())) {
            return;
        }
        this.getTask().unlock();
    }

    /**
     * 清除记录。
     */
    public void clear() {
        // 清空聊天记录
        if (null != this.chatList) {
            this.chatList.clear();
            this.chatList = null;
        }
        // 清空历史局
        if (null != this.historySet) {
            this.historySet.clear();
            this.historySet = null;
        }
        // 游戏房间数据库
        if (null != this.gameRoomBO) {
            this.gameRoomBO.clear();
            this.gameRoomBO = null;
        }
        // 环信SDK通信信息
        this.mHXSDKChatInfo = null;
        if (null != this.task) {
            this.task.clear();
            this.task = null;
        }
        // 清理结算
        if (null != this.roomEndResultInfo) {
            this.roomEndResultInfo.clear();
            this.roomEndResultInfo = null;
        }
        // 清空公共配置
        if (null != this.baseRoomConfigure) {
            this.baseRoomConfigure.clear();
            this.baseRoomConfigure = null;
        }
        // 清空当局
        if (null != this.curSet) {
            this.curSet.clear();
            this.curSet.clearBo();
            this.curSet = null;
        }
        // 清空房间位置管理
        if (null != this.roomPosMgr) {
            this.roomPosMgr.clear();
            this.roomPosMgr = null;
        }
        // 清空解散房间
        if (null != this.dissolveRoom) {
            this.dissolveRoom.clear();
            this.dissolveRoom = null;
        }
        // 清空相同IP解散房间管理
        if (null != this.sameIpDissolveRoom) {
            this.sameIpDissolveRoom.clear();
            this.sameIpDissolveRoom = null;
        }


        // 操作切换人数
        if (null != this.opChangePlayerRoom) {
            this.opChangePlayerRoom.clear();
            this.opChangePlayerRoom = null;
        }
        // 清房间类型信息
        if (null != roomTyepImpl) {
            this.roomTyepImpl.clear();
            this.roomTyepImpl = null;
        }
    }

    /**
     * 存在解散房间
     *
     * @return
     */
    public boolean existDissolveRoom() {
        return Objects.nonNull(this.getDissolveRoom());
    }


    /**
     * 增加标识Id记录
     */
    public void addTabId() {
        this.setTabId(this.getTabId() + 1);
    }

    /**
     * 设置当前局
     *
     * @param curSet
     */
    public void setCurSet(AbsRoomSet curSet) {
        if (Objects.nonNull(curSet)) {
            this.addTabId();
        }
        this.curSet = curSet;
        //更新缓存的局数
        if (Config.isShare() && this.curSet != null) {
            ShareRoomMgr.getInstance().updateSetId(this.getRoomKey(), this.curSet.getSetID());
        }
    }

    @Override
    public boolean isEndRoom() {
        return Objects.isNull(getTask()) ? true : RoomState.End.equals(this.getTask().getRoomState());
    }

    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        if (null == this.getRoomTyepImpl()) {
            return RoomTypeEnum.NORMAL;
        }
        return this.getRoomTyepImpl().getRoomTypeEnum();
    }

    /**
     * 自动开始游戏 所有玩家准备好自动开始。
     *
     * @return
     */
    public abstract boolean autoStartGame();

    /**
     * 自动准备游戏 玩家加入房间时，自动进行准备。
     *
     * @return
     */
    public abstract boolean autoReadyGame();

    /**
     * 是否立即开始。
     *
     * @return
     */
    public boolean atOnceStartGame() {
        return false;
    }

    /**
     * 检查是否解散房间
     *
     * @return T:解散,F:不解散
     */
    public boolean checkDissolveRoom(int curSec) {
        return checkDissolveRoom(curSec, DissolveType.ALL);
    }


    /**
     * 检查是否解散房间
     *
     * @return T:解散,F:不解散
     */
    public boolean checkDissolveRoom(int curSec, DissolveType type) {
        if (null == this.getDissolveRoom()) {
            // 没有玩家发起解散。
            return false;
        }
        // 设置托管状态，发起解散
        this.getTrusteeship().setTrusteeshipState(TrusteeshipState.Dissolve);
        // 是否解散房间
        boolean needDissolve = this.getDissolveRoom().isDelay(curSec)
                || this.getDissolveRoom().isAllAgree(type);
        if (needDissolve) {
            this.getTrusteeship().setTrusteeshipState(TrusteeshipState.Wait);
            // 解散房间
            this.doDissolveRoom(false);
            CommLog.info("checkDissolveRoom RoomId:{},RoomKey:{},GameId:{},curSetID:{},DissolveMsg:{}", getRoomID(), getRoomKey(), getGameRoomBO().getGameType(), getCurSetID(), this.getDissolveRoom().getDissolveInfoLog());
        }
        return true;
    }

    /**
     * 构成函数
     *
     * @param baseRoomConfigure 公共配置
     * @param roomKey           房间key
     * @param ownerID           房主ID
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long ownerID) {
        super();
        // 设置房间key
        this.setRoomKey(roomKey);
        // 设置房主ID
        this.setOwnerID(ownerID);
        // 深度拷贝房间配置
        this.setBaseRoomConfigure(baseRoomConfigure.deepClone());
        // 初始化房间类型
        this.initRoomType();
        // 初始化房间玩家位置管理
        this.setRoomPosMgr(this.initRoomPosMgr());
        // 初始切换人数
        this.initChangePlayerRoom();
        // 初始化房间线程
        this.initTastRoom();
        // 初始化房间BO
        this.initGameRoomBO();
        // 初始化打开房间的位置
        this.initCreatePos();
        // 初始化托管
        this.initTrusteeship();
        // 通知新房间创建
        this.getRoomTyepImpl().createNewSetRoom();

    }

    /**
     * 查询房间内的所有玩家PID
     */
    @Override
    public List<Long> getRoomPidAll() {
        return this.getRoomPosMgr().getRoomPidAll();
    }

    /**
     * 初始切换人数
     */
    private void initChangePlayerRoom() {
        this.setOpChangePlayerRoom(new OpChangePlayerRoom(this));
    }

    /**
     * 初始化房间类型
     */
    public void initRoomType() {
        if (null != this.getBaseRoomConfigure().getClubRoomCfg()) {
            // 亲友圈房间
            this.setRoomTyepImpl(new ClubRoom(this));
        } else if (null != this.getBaseRoomConfigure().getUnionRoomCfg()) {
            // 大赛事房间
            this.setRoomTyepImpl(newUnionRoom());
        } else if (null != this.getBaseRoomConfigure().getRobotRoomCfg()) {
            // 练习场房间
            this.setRoomTyepImpl(new RobotRoom(this));
        } else {
            // 正常普通房间
            this.setRoomTyepImpl(new NormalRoom(this));
        }
    }


    /**
     * 联赛房间
     *
     * @return
     */
    public RoomTyepImpl newUnionRoom() {
        return new UnionRoom(this);
    }

    /**
     * 是否重新设置解散时间
     *
     * @return
     */
    public boolean isReSetDissolve() {
        return false;
    }

    @Override
    public long getConfigId() {
        return this.getRoomTyepImpl().getConfigId();
    }

    @Override
    public boolean checkExistEmptyPos() {
        return this.getRoomPosMgr().checkExistEmptyPos();
    }

    @Override
    public ClassType getClassType() {
        return this.getRoomTyepImpl().getClassType();
    }


    @Override
    public int sorted() {
        //空配置房间
        if (isNoneRoom()) {
            return RoomSortedEnum.NONE_CONFIG.ordinal();
        }
        if (RoomState.Playing.equals(this.getRoomState()) || RoomState.End.equals(this.getRoomState())) {
            // 游戏中 或者结束
            return RoomSortedEnum.GAME_PLAYING.ordinal() + 5;
        }
        if (this.getRoomPosMgr().getFullPosCount() > 0 && this.getRoomPosMgr().getFullPosCount() < this.getPlayerNum()) {
            // 房间中有人
            return RoomSortedEnum.NONE_ROOM.ordinal();
        } else if (this.getRoomPosMgr().getFullPosCount() == this.getPlayerNum()) {
            // 房间满人
            return RoomSortedEnum.GAME_INIT.ordinal() + 3;
        } else {
            // 空房间
            return RoomSortedEnum.NONE_CONFIG.ordinal();
        }
    }

    /**
     * 获取亲友圈ID
     *
     * @return
     */
    @Override
    public long getSpecialRoomId() {
        return this.getRoomTyepImpl().getSpecialRoomId();
    }

    /**
     * 初始化房间线程
     */
    private void initTastRoom() {
        this.task = null;
        if (null != this.getBaseRoomConfigure().getRobotRoomCfg()) {
            this.task = new RobotTaskRoom(this, this.getTimerTime());
        } else {
            this.task = new NoneTaskRoom(this, this.getTimerTime());
        }

    }

    /**
     * 初始化房间BO
     */
    public void initGameRoomBO() {
        if (null == this.getGameRoomBO()) {
            this.setGameRoomBO(new GameRoomBO());
        }
        BaseCreateRoom baseCreateRoom = this.getBaseRoomConfigure().getBaseCreateRoom();
        this.getGameRoomBO().setCreateTime(this.getTask().getCreateSec());
        this.getGameRoomBO().setSetCount(baseCreateRoom.getSetCount());
        this.getGameRoomBO().setPlayerNum(baseCreateRoom.getPlayerNum());
        this.getGameRoomBO().setDataJsonCfg(this.dataJsonCfg());
        this.getGameRoomBO().setRoomKey(this.roomKey);
        this.getGameRoomBO().setOwnner(getOwnerID());
        this.getGameRoomBO().setGameType(getBaseRoomConfigure().getGameType().getId());
        this.getGameRoomBO().setType(this.getBaseRoomConfigure().getGameType().getType().value());
        this.getGameRoomBO().setDateTime(Integer.parseInt(CommTime.getNowTimeStringYMD()));
        this.setDateTimeZhongZhi(String.valueOf(CommTime.getCycleNowTime6YMD()));
        if (RoomTypeEnum.UNION.equals(getRoomTypeEnum())) {
            this.getGameRoomBO().setUnionId(baseCreateRoom.getUnionId());
        } else if (RoomTypeEnum.CLUB.equals(getRoomTypeEnum())) {
            this.getGameRoomBO().setClubID(baseCreateRoom.getClubId());
        }
        this.getGameRoomBO().setConfigId(baseCreateRoom.getGameIndex());
        this.getGameRoomBO().setConfigName(baseCreateRoom.getRoomName());
        this.getGameRoomBO().setRoomState(RoomState.Init.value());
        this.getGameRoomBO().setRoomSportsType(baseCreateRoom.getRoomSportsType());
        this.getGameRoomBO().getBaseService().saveOrUpDate(this.getGameRoomBO());
    }

    /**
     * 消耗值类型
     *
     * @return
     */
    public PrizeType getValueType() {
        if (this.getSpecialRoomId() <= 0L) {
            // 不是亲友圈，消耗什么类型就是什么类型
            return this.getBaseRoomConfigure().getPrizeType();
        }
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            // 亲友圈模式下：房主付就是房卡
            return PrizeType.RoomCard;
        } else {
            // 亲友圈模式下：大赢家付和平分付就是圈卡
            return PrizeType.ClubCard;
        }
    }

    /**
     * 初始化托管
     */
    public void initTrusteeship() {
        this.trusteeship = new Trusteeship(this);
    }

    /**
     * 设置房间公共信息
     *
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getBaseRoomInfo(GetRoomInfo info) {
        info.setPrizeType(this.getBaseRoomConfigure().getPrizeType());
        info.setState(this.getRoomState());
        info.setRoomID(this.getRoomID());
        info.setOwnerID(this.getOwnerID());
        info.setKey(this.getRoomKey());
        info.setCreateSec(this.getTask().getCreateSec());
        info.setSetID(this.getCurSetID());
        info.setArenaCfg(this.getBaseRoomConfigure().getArenaRoomCfg());
        info.setClubCfg(this.getBaseRoomConfigure().getClubRoomCfg());
        info.setUnionCfg(this.getBaseRoomConfigure().getUnionRoomCfg());
        info.setChatInfo(this.getMHXSDKChatInfo());
        info.setPosList(this.getRoomPosMgr().getNotify_PosList());
        info.setCfg(this.getBaseRoomConfigure().getBaseCreateRoom());
        if (null != this.getDissolveRoom()) {
            info.setDissolve(this.getDissolveRoom().getNotify());
        } else {
            info.setDissolve(new Room_Dissolve());
        }
        if (null != this.getOpChangePlayerRoom().getChangePlayerNumRoom()) {
            info.setChangePlayerNum(this.getOpChangePlayerRoom().getChangePlayerNumRoom().getNotify(false));
        } else {
            info.setChangePlayerNum(new Room_ChangePlayerNum());
        }

    }

    /**
     * 房间游戏配置
     *
     * @return
     */
    public abstract String dataJsonCfg();

    /**
     * 初始化房间玩家位置管理
     *
     * @return
     */
    public abstract AbsRoomPosMgr initRoomPosMgr();

    /**
     * 初始化打开房间的位置
     */
    public abstract void initCreatePos();

    /**
     * 进入房间
     *
     * @param pid     玩家Pid
     * @param posID   位置ID
     * @param isRobot T:机器人,F:玩家
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result enterRoom(long pid, int posID, boolean isRobot, ClubMemberBO clubMemberBO) {
        return this.enterRoom(pid, posID, isRobot, false, initPoint(), clubMemberBO);
    }

    @SuppressWarnings("rawtypes")
    public SData_Result seatRoom(long pid, int posID, boolean isRobot, ClubMemberBO clubMemberBO) {
        return this.seatRoom(pid, posID, isRobot, false, initPoint(), clubMemberBO);
    }

    public SData_Result enterRoom(long pid, int posID, boolean isRobot, boolean isReady, int initPoint,
                                  ClubMemberBO clubMemberBO) {
        try {
            lock();
            // 玩家ID不存在。
            if (pid <= 0L) {
                return SData_Result.make(ErrorCode.NotAllow, "pid <= 0L PID:{%d},posID:{%d}", pid, posID);
            }

            if (!isWatchRoom()) {
                // 加入房间的其他条件 条件不满足不进入
                if (!this.enterRoomOtherCondition(pid)) {
                    return SData_Result.make(ErrorCode.Room_STATUS_ERROR, "enterRoomOtherCondition RoomState:{%s}",
                            this.getRoomState());
                }
            }

            // 通过pid获取玩家信息
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos && isWatchRoom()) {
                roomPos = this.getRoomPosMgr().getWatchPosByPid(pid);
            }
            if (null != roomPos) {
                // 该玩家已经加入房间.
                return SData_Result.make(ErrorCode.NotAllow, "getPosByPid null != roomPos pid:{%d}", pid);
            }
            if (posID >= 0) {
                // 通过PosID获取玩家信息
                roomPos = this.getRoomPosMgr().getPosByPosID(posID);
                if (null == roomPos) {
                    // 找不到位置数据
                    return SData_Result.make(ErrorCode.NotAllow, "null == roomPos posID:{%d}", posID);
                }
                // 指定的位置有人了。
                if (roomPos.getPid() > 0) {
                    return SData_Result.make(ErrorCode.NotAllow, "roomPos.getPid() > 0 PID:{%d}", roomPos.getPid());
                }
            } else {
                // 获取一个空的位置
                roomPos = this.getRoomPosMgr().getEmptyPos();
                if (null == roomPos) {
                    // 没有空的位置
                    return SData_Result.make(ErrorCode.ErrorSysMsg, "MSG_ROOM_FULL_PLAYER");
                }
            }
            if (!isWatchRoom()) {
                // 设置玩家位置
                if (!roomPos.seat(pid, initPoint, isRobot, clubMemberBO)) {
                    return SData_Result.make(ErrorCode.NotAllow, "enterRoom seat");
                }
                if (isReady) {
                    roomPos.setReady(true);
                }
                if (!isRobot) {
                    roomPos.getPlayer().setCurrentGameType(this.getGameRoomBO().getGameType());
                    // 玩家进入房间 标识进入的房间ID
                    roomPos.getPlayer().onEnterRoom(this.getRoomID());
                    //更新所有人计时时间
                    if (this.getRoomPosMgr().getEmptyPos() == null) {
                        if (RoomTypeEnum.checkUnionOrClub(getRoomTypeEnum())) {
                            this.getRoomPosMgr().notify2AllNotExistRoom(SBase_RoomCrammed.make(this.getRoomID(), this.getRoomKey(), this.getBaseRoomConfigure().getBaseCreateRoom().getRoomName(), this.getBaseRoomConfigure().getGameType().getId()));
                        }
                    }
                }
                this.getOpChangePlayerRoom().changePlayerNumEnterRoom(roomPos.getPosID());
            } else {
                roomPos = this.getRoomPosMgr().addWatch();

                if (!roomPos.doWatch(pid, clubMemberBO)) {
                    return SData_Result.make(ErrorCode.NotAllow, "enterRoom watch");
                }
                roomPos.getPlayer().setCurrentGameType(this.getGameRoomBO().getGameType());
                // 玩家进入房间 标识进入的房间ID
                roomPos.getPlayer().onEnterRoom(this.getRoomID());
            }
        } finally {
            unlock();
        }
        //共享更新
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this);
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 进入房间
     *
     * @param pid     玩家Pid
     * @param posID   位置ID
     * @param isRobot T:机器人,F:玩家
     * @param isReady T:准备,F:没准备
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result seatRoom(long pid, int posID, boolean isRobot, boolean isReady, int initPoint,
                                 ClubMemberBO clubMemberBO) {
        try {
            lock();
            // 玩家ID不存在。
            if (pid <= 0L) {
                return SData_Result.make(ErrorCode.NotAllow, "pid <= 0L PID:{%d},posID:{%d}", pid, posID);
            }
//			// 加入房间的其他条件 条件不满足不进入
//			if (!this.enterRoomOtherCondition(pid)) {
//				return SData_Result.make(ErrorCode.Room_STATUS_ERROR, "enterRoomOtherCondition RoomState:{%s}",
//						this.getRoomState());
//			}
            // 通过pid获取玩家信息
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null != roomPos) {
                if (null == roomPos.getLiuzuoSec()) {
                    return SData_Result.make(ErrorCode.NotAllow, "getPosByPid null == liuzuo pid:{%d}", pid);
                } else if (roomPos.getPlayer().getPid() != pid) {
                    return SData_Result.make(ErrorCode.NotAllow, "getPosByPid pid != roomPos.pid pid:{%d}", pid);
                }
                // 该玩家已经加入房间.
                return SData_Result.make(ErrorCode.NotAllow, "getPosByPid null != roomPos pid:{%d}", pid);
            }
            if (posID >= 0) {
                // 通过PosID获取玩家信息
                roomPos = this.getRoomPosMgr().getPosByPosID(posID);
                if (null == roomPos) {
                    // 找不到位置数据
                    return SData_Result.make(ErrorCode.NotAllow, "null == roomPos posID:{%d}", posID);
                }
                // 指定的位置有人了。
                if (roomPos.getPid() > 0) {
                    return SData_Result.make(ErrorCode.NotAllow, "roomPos.getPid() > 0 PID:{%d}", roomPos.getPid());
                }
            } else {
                // 获取一个空的位置
                roomPos = this.getRoomPosMgr().getEmptyPos();
                if (null == roomPos) {
                    // 没有空的位置
                    return SData_Result.make(ErrorCode.ErrorSysMsg, "MSG_ROOM_FULL_PLAYER");
                }
            }
            if (getRoomTypeEnum() == RoomTypeEnum.UNION) {
                if (clubMemberBO.getSportsPoint() < minPoint) {
                    return SData_Result.make(ErrorCode.NotAllow, "enterRoom seat");
                }
            }
            // 设置玩家位置
            if (!roomPos.seat(pid, initPoint, isRobot, clubMemberBO)) {
                return SData_Result.make(ErrorCode.NotAllow, "enterRoom seat");
            }
            if (isReady) {
                roomPos.setReady(true);
            }
            // 开始未发牌时需要重新加载玩家
            if (null != getCurSet() && getCurSet().getState() == SetState.Init) {
                getCurSet().reInit();
            }
            if (!isRobot) {
                roomPos.getPlayer().setCurrentGameType(this.getGameRoomBO().getGameType());
                // 玩家进入房间 标识进入的房间ID
                roomPos.getPlayer().onEnterRoom(this.getRoomID());
                //更新所有人计时时间
                if (this.getRoomPosMgr().getEmptyPos() == null) {
                    if (RoomTypeEnum.checkUnionOrClub(getRoomTypeEnum())) {
                        this.getRoomPosMgr().notify2AllNotExistRoom(SBase_RoomCrammed.make(this.getRoomID(), this.getRoomKey(), this.getBaseRoomConfigure().getBaseCreateRoom().getRoomName(), this.getBaseRoomConfigure().getGameType().getId()));
                    }
                }
            }
            this.getOpChangePlayerRoom().changePlayerNumEnterRoom(roomPos.getPosID());
        } finally {
            unlock();
        }
        //共享更新
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this);
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 位置初始分分数
     * 需要时可以重写
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public int initPoint() {
        return 0;
    }

    /**
     * 加入房间的其他条件 条件不满足不进入
     */
    public abstract boolean enterRoomOtherCondition(long pid);

    /**
     * 开始游戏
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result startGame(long pid) {
        try {
            lock();
            if (this.getOwnerID() <= 0L) {
                // 没有房主
                return SData_Result.make(ErrorCode.NotAllow, "ownerID <= 0L");
            }
            if (this.getOwnerID() != pid) {
                // 操作玩家不是房主
                return SData_Result.make(ErrorCode.NotAllow, "ownerID:{%d},Pid:{%d}", this.getOwnerID(), pid);
            }
            if (!RoomState.Init.equals(this.getRoomState())) {
                // 房间不是初始状态
                return SData_Result.make(ErrorCode.NotAllow, "RoomState not Init {%s}", this.getRoomState().name());
            }
            if (!startGameOtherCondition(pid)) {
                // 有玩家没准备
                return SData_Result.make(ErrorCode.NotAllow, "startGame not isAllready");
            }
            // 通知新房间创建
            this.getRoomTyepImpl().createNewSetRoom();
            // 进入游戏阶段
            this.setRoomState(RoomState.Playing);
            // 游戏开始操作
            this.startGameBase();
            //共享更新
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(this);
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 开始游戏条件
     *
     * @param pid
     * @return
     */
    public abstract boolean startGameOtherCondition(long pid);


    /**
     * 游戏开始操作
     */
    public void startGameBase() {
        if (PrizeType.RoomCard.equals(this.getBaseRoomConfigure().getPrizeType())) {
            // 设置当局状态
            this.getGameRoomBO().setRoomState(RoomState.Playing.value());
            // 消耗值类型
            this.getGameRoomBO().setValueType(this.getValueType().value());
            // 消耗值
            this.getGameRoomBO().setConsumeValue(this.getConsumeValue());
            // 结束时间
            this.getGameRoomBO().setEndTime(CommTime.nowSecond());
            // 保存玩家信息
            this.getGameRoomBO().setPlayerList(new Gson().toJson(this.getRoomPosMgr().getRoomPlayerPosList()));
            // 保存数据
            this.getGameRoomBO().getBaseService().update(this.getGameRoomBO().getUpdatePlayerListConsumeValue(),
                    this.getGameRoomBO().getId());
            // 添加玩家个人游戏记录
            this.getRoomPosMgr().insertPlayerRoomAloneBO();
            //共享更新
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(this);
            }
        }
    }

    /**
     * 主动离开房间
     *
     * @param pid 用户ID
     * @return
     */

    @SuppressWarnings("rawtypes")
    public SData_Result exitRoom(long pid) {
        try {
            lock();
            // 房主不能主动离开房间
            if (pid == this.getOwnerID()) {
                return SData_Result.make(ErrorCode.NotAllow, "exitRoom pid:{%d} == ownerID:{%d}", pid,
                        this.getOwnerID());
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                roomPos = this.getRoomPosMgr().getWatchPosByPid(pid);
            } else if (roomPos.getLiuzuoSec() == null && roomPos.isPlaying() && null != getCurSet() && getCurSet().getState() == SetState.Playing) {
                return SData_Result.make(ErrorCode.NotAllow, "exitRoom is playing ");
            }
            if (null == roomPos) {
                // 找不到指定pid玩家信息。
                return SData_Result.make(ErrorCode.NotAllow, "exitRoom null == roomPos Pid:{%d}", pid);
            }
            if (winOrLose.containsKey(pid) && winOrLose.get(pid) > 0) {
                return SData_Result.make(ErrorCode.NotAllow, "winOrLose > 0 PID:{%d}", pid);
            }
            if (null != getCurSet() && !getCurSet().isEnd()) {
                return SData_Result.make(ErrorCode.NotAllow, "playing connot leave PID:{%d}", pid);
            }
            if (!isWatchRoom() && getRoomState() != RoomState.Init) {
                return SData_Result.make(ErrorCode.NotAllow, "exitRoom is playing ");
            }

            roomPos.leave(false, this.getOwnerID(), CKickOutType.None);
            //共享更新
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().updateShareRoom(this);
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 主动离开房间的其他条件 条件不满足不退出
     *
     * @return
     */

    public abstract boolean exitRoomOtherCondition(long pid);

    /**
     * 房间通知指定玩家是否失去连接
     *
     * @param pid           玩家PID
     * @param isLostConnect T:失去连接，F:建立连接
     */
    public void lostConnect(long pid, boolean isLostConnect) {
        AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
        if (Objects.nonNull(roomPos)) {
            // 是否掉线
            roomPos.setLostConnect(isLostConnect);
            // 离线定时托管
            if (isConnectClearTrusteeship()) {
                getTrusteeship().lostConnectTrusteeship(roomPos, isLostConnect);
            }
            // 通知所有玩家
            this.getRoomPosMgr().notify2AllPublic(this.LostConnect(this.getRoomID(), pid, isLostConnect, roomPos.isShowLeave()));
            // 房间人员状态发生改变
            this.getRoomTyepImpl().roomPlayerChange(roomPos);
            //共享更新
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(this);
            }
        }
    }

    /**
     * 操作显示离开
     *
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result opShowLeave(long pid, boolean isShowLeave) {
        this.lock();
        try {
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                return SData_Result.make(ErrorCode.NotAllow, "null == roomPos pid:{%d}", pid);
            }
            roomPos.setShowLeave(isShowLeave);
            this.getRoomPosMgr().notify2All(
                    this.LostConnect(this.getRoomID(), pid, roomPos.isLostConnect(), roomPos.isShowLeave()));
            //共享更新
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(this);
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.unlock();
        }
    }

    /**
     * 继续游戏
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result continueGame(long pid) {
        return this.getRoomTyepImpl().continueGame(pid);
    }

    public DissolveRoom initDissolveRoom(int posID, int WaitSec) {
        return new DissolveRoom(this, posID, WaitSec);
    }

    public SameIpDissolveRoom initSameIpDissolveRoom(List<Long> sameIpPidList, int WaitSec) {
        return new SameIpDissolveRoom(this, sameIpPidList, 180);
    }


    /**
     * 解散房间
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result dissolveRoom(long pid) {
        try {
            lock();
            if (RoomState.Init.equals(this.getRoomState())) {
                if (this.getOwnerID() != pid) {
                    // 不是房主
                    return SData_Result.make(ErrorCode.NotAllow, "RoomState Init OwnerID:{%d} != PID:{%d}",
                            this.getOwnerID(), pid);
                }
                // 解散房间
                this.doDissolveRoom(true);
                return SData_Result.make(ErrorCode.Success);
            } else if (RoomState.Playing.equals(this.getRoomState())) {
                // 获取房间解散配置
                int waitSec = this.dissolverWaitSec();
                if (waitSec <= 0) {
                    // 不可解散
                    return SData_Result.make(ErrorCode.NotAllow, "DissolveConfigEnum:{NotDissolve}");
                }
                if (null == this.getDissolveRoom()) {
                    AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
                    // 玩家存在，并且处于游戏中。
                    if (null != roomPos && roomPos.isPlayTheGame()) {
                        if (!checkDissolveCount(roomPos)) {//解散次数不足
                            return SData_Result.make(ErrorCode.DissolveRoom_Error, "DissolveConfigEnum:{DissolveRoom_Error}");
                        }
                        // 发起人默认同意
                        roomPos.setDissolveRoom(true);
                        // 设置解散房间
                        this.setDissolveRoom(initDissolveRoom(roomPos.getPosID(), waitSec));
                        BaseSendMsg baseSendMsg = this.StartVoteDissolve(this.getRoomID(), roomPos.getPosID(), this.getDissolveRoom().getEndSec());
                        // 开始发起解散(只通知在游戏中的玩家)
                        this.getRoomPosMgr().notify2AllPlaying(baseSendMsg);
                        // 游戏中记录谁发起解散
                        this.startVoteDissolveRoom(baseSendMsg);
                        CommLog.info("startVoteDissolveRoom RoomId:{},RoomKey:{},GameId:{},curSetID:{},DissolveMsg:{}", getRoomID(), getRoomKey(), getGameRoomBO().getGameType(), getCurSetID(), this.getDissolveRoom().getDissolveInfoLog());
                    } else {
                        return SData_Result.make(ErrorCode.NotAllow, "not in any pid:{%d}", pid);
                    }
                } else {
                    return SData_Result.make(ErrorCode.DissolveRoom_Already_Exists, "dissolveRoom Already exists");
                }
            } else if (RoomState.End.equals(this.getRoomState())) {
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoom RoomState End");
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 解散倒计时时间
     *
     * @return
     */
    public int dissolverWaitSec() {
        return DissolveConfigEnum.valueOf(this.getBaseRoomConfigure().getBaseCreateRoom().getJiesan()).getValue();
    }

    /**
     * 游戏中记录谁发起解散
     */
    public void startVoteDissolveRoom(BaseSendMsg baseSendMsg) {
        if (null != this.getCurSet()) {
            // 回放记录谁发起解散
            this.getCurSet().addDissolveRoom(baseSendMsg);
        }
    }

    /**
     * 解散房间同意
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result dissolveRoomAgree(long pid) {
        try {
            lock();
            if (null == this.getDissolveRoom()) {
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomAgree null == this.getDissolveRoom()");
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 玩家不存在
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomAgree null == roomPos PID:{%d}", pid);
            }
            if (!roomPos.isPlayTheGame()) {
                // 玩家不在游戏中。
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomAgree not isPlayTheGame PID:{%d}", pid);
            }
            // 同意解散
            if (!this.getDissolveRoom().deal(roomPos.getPosID(), true)) {
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomRefuse not deal PID:{%d}", pid);
            }
            // 同意解散
            roomPos.setDissolveRoom(true);
            this.getRoomPosMgr().notify2AllPlaying(PosDealVote(this.getRoomID(), roomPos.getPosID(), true, this.getDissolveRoom().reSetDissolve(this.getPlayingCount(), this.getClassType())));
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 解散房间拒绝
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result dissolveRoomRefuse(long pid) {
        try {
            lock();
            if (null == this.getDissolveRoom()) {
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomRefuse null == this.getDissolveRoom()");
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 玩家不存在
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomRefuse null == roomPos PID:{%d}", pid);
            }
            if (!roomPos.isPlayTheGame()) {
                // 玩家不在游戏中。
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomRefuse not isPlayTheGame PID:{%d}", pid);
            }
            // 拒绝
            if (!this.getDissolveRoom().deal(roomPos.getPosID(), false)) {
                return SData_Result.make(ErrorCode.NotAllow, "dissolveRoomRefuse not deal PID:{%d}", pid);
            }
            this.getRoomPosMgr().notify2AllPlaying(PosDealVote(this.getRoomID(), roomPos.getPosID(), false, 0));
            if (this.getDissolveRoom().isRefused()) {
                // 确认拒绝解散。
                this.setDissolveRoom(null);
            }
            this.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
            // 所有玩家设置拒绝解散状态
            this.getRoomPosMgr().allRefuseDissolve();
        } finally {
            unlock();
        }
        return SData_Result.make(ErrorCode.Success);

    }


    /**
     * 特殊踢出房间
     *
     * @param pid      用户ID
     * @param posIndex 位置
     */
    @SuppressWarnings("rawtypes")
    public SData_Result specialKickOut(long pid, int posIndex, String msg) {
        return this.getRoomTyepImpl().kickOut(pid, posIndex, msg);
    }


    /**
     * 踢出房间
     *
     * @param pid      用户ID
     * @param posIndex 位置
     */
    @SuppressWarnings("rawtypes")
    public SData_Result kickOut(long pid, int posIndex) {
        if (RoomTypeEnum.NORMAL.equals(this.getRoomTypeEnum())) {
            return this.getRoomTyepImpl().kickOut(pid, posIndex, null);
        }
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
    }

    /**
     * 玩家准备
     *
     * @param isReady 是否准备
     * @param pid     用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result playerReady(boolean isReady, long pid) {
        try {
            lock();
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (roomPos == null) {
                // 找不到指定的位置信息
                return SData_Result.make(ErrorCode.NotAllow, "not in pos");
            }
//			if (!RoomState.Init.equals(this.getRoomState())) {
//				// 房间不处于初始阶段
//				return SData_Result.make(ErrorCode.NotAllow, "playerReady RoomState Init :{%s}", this.getRoomState());
//			}
            roomPos.setReady(isReady);
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 房间托管
     *
     * @param pid
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opRoomTrusteeship(long pid, boolean trusteeship) {
        try {
            lock();
            return this.getTrusteeship().roomTrusteeship(pid, trusteeship, false);
        } finally {
            unlock();
        }

    }

    /**
     * 托管时间值
     *
     * @return
     */
    public int trusteeshipTimeValue() {
        return XianShiConfigEnum.valueOf(getBaseRoomConfigure().getBaseCreateRoom().getXianShi()).getValue();
    }

    /**
     * 房间语音
     *
     * @param pid 用户ID
     * @param url 语音地址
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opRoomVoice(long pid, String url) {
        try {
            lock();
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (roomPos == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not in pos");
            }
            if (isDisAbleVoice()) {
                return SData_Result.make(ErrorCode.NotAllow, "can not voice, is ke xuan wan fa : ");
            }
            this.getRoomPosMgr().notify2AllPublic(this.Voice(this.getRoomID(), roomPos.getPosID(), url));
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 房间洗牌
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opXiPai(long pid) {
        return getRoomTyepImpl().opXiPai(pid);
    }

    /**
     * 房间聊天
     * <p>
     * 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opChat(Player sender, String content, ChatType type, long toCId, int quickID) {
        try {
            lock();
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(sender.getPid());
            if (roomPos == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not in pos");
            }
            this.godCardMsg(content, sender.getPid());
            if (getChatList().size() >= 20) {
                getChatList().remove(0);
            }
            ChatMessage chatMessage = (ChatMessage) this.ChatMessage(sender.getPid(), sender.getName(), content, type,
                    toCId, quickID);
            getChatList().add(chatMessage);
            this.getRoomPosMgr().notify2AllPublic(chatMessage);
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 获取所有人的定位信息
     *
     * @param pid
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opGetAllLoaction(long pid) {
        try {
            lock();
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (roomPos == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not in pos");
            }
            return SData_Result.make(ErrorCode.Success,
                    SGet_LocationEx.make(this.getRoomPosMgr().getLocationInfoList()));
        } finally {
            unlock();
        }
    }

    /**
     * 解散房间
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result specialDissolveRoom(long roomTypeId, RoomTypeEnum roomTypeEnum, int minister, String msg) {
        try {
            lock();
            if (roomTypeId > 0 && this.getSpecialRoomId() == roomTypeId && roomTypeEnum.equals(this.getRoomTypeEnum())) {
                this.doSpecialDissolveRoom(DissolveNoticeTypeEnum.SPECIAL_DISSOLVE, msg);
                return SData_Result.make(ErrorCode.Success);
            }
        } finally {
            unlock();
        }
        return SData_Result.make(ErrorCode.NotAllow, "error specialDissolveRoom");
    }

    /**
     * 获取历史局数
     */
    public int getHistorySetSize() {
        return this.historySet.size();
    }

    /**
     * 获取历史局数
     */
    public AbsRoomSet getHistorySet(int i) {
        return this.historySet.get(i);
    }

    private final synchronized void lockEndRoom() {
        if (RoomState.End.equals(this.getRoomState())) {
            // 房间已经结算过。
            return;
        }// 设置房间结算状态。
        this.setRoomState(RoomState.End);
    }

    /**
     * 房间结算
     */
    public void endRoom() {
        if (RoomState.End.equals(this.getRoomState()) || this.existEndRoom.get()) {
            // 房间已经结算过。
            return;
        }
        if (!RoomState.Init.equals(this.getRoomState())
                && !business.global.billing.LegacySettlementGuard.beginFinal(this.getRoomID(), "room-common-v1")) {
            return;
        }
        // 标记房间已经结算过了
        this.existEndRoom.set(true);
        // 标志房间结束
        this.isRoomEnd = true;
        // 中途解散保存战绩
        if (this.isMidwayDisbandmentPreservation()) {
            AbsRoomSet curSet = this.getCurSet();
            if (null != curSet) {
                // 小局结算
                curSet.endSet();
            }

        }
        if (RoomState.Init.equals(this.getRoomState())) {
            // 游戏还没有开始,删除数据
            this.deleteRecordRelated();
        } else if (RoomState.Playing.equals(this.getRoomState())) {
            // 结算
            this.calcEnd();
        }
        // 设置房间结算状态。
        this.setRoomState(RoomState.End);
        // 设置结束房间
        this.setEndRoom();
        // 操作大赢家付
        this.getRoomPosMgr().onWinnerPay();
        // 赢家抽水
        this.getRoomPosMgr().onWinnerC(true);
        //房间继续功能相关信息
        this.continueRoom();
        // 房间管理注销
        this.forceCloseRoom();
    }


    /**
     * 继续房间相关信息
     */
    protected abstract void continueRoom();

    /**
     * 中途解散保存战绩
     *
     * @return T:保存战绩,F:不保存
     */
    public abstract boolean isMidwayDisbandmentPreservation();

    /**
     * 强制关闭房间
     */
    public void forceCloseRoom() {

        if (Objects.isNull(this.getTask()) || this.getTask().getEndSec() > 0) {
            // 已经强制关闭过房间。
            return;
        }
        // 移除房间ID和房间Key
        RoomMgr.getInstance().removeRoom(this.getRoomID(), this.getBaseRoomConfigure().getPrizeType());
        // 托管
        trusteeshipCancelTimer();
        // 清除玩家身上的房间状态
        this.getRoomPosMgr().onClose();
        // 设置房间结束时间
        this.getTask().setEndSec(CommTime.nowSecond());
    }

    /**
     * 托管关闭
     */
    private void trusteeshipCancelTimer() {
        if (Objects.isNull(this.getTrusteeship())) {
            return;
        }
        // 托管
        this.getTrusteeship().setTrusteeshipState(TrusteeshipState.End);
        this.getTrusteeship().cancelTimer();
    }

    /**
     * 更新游戏房间BO和更新玩家个人游戏记录BO
     */
    public void updateGameRoomBOAndPlayerRoomAloneBO() {
        if (PrizeType.Gold.equals(this.getBaseRoomConfigure().getPrizeType()) || this.getGameRoomBO().getRoomState() == RoomState.Init.value()) {
            this.deleteRecordRelated();
            return;
        }
        // 房间配置
        BaseCreateRoom baseCreateRoom = this.getBaseRoomConfigure().getBaseCreateRoom();
        // 获取房间内的竞技点消耗
        double roomSportsPointConsume = this.getRoomSportsPointConsume(baseCreateRoom);
        // 回退所有未玩过游戏人的数据
        this.getRoomPosMgr().backConsumeNotPlayTheGame();
        // 消耗值
        this.setConsumeValue(this.getRoomTyepImpl().sumConsume());
        // 日期时间
        int dateTime = Integer.parseInt(CommTime.getNowTimeStringYMD());
        // 房间公共结算信息
        this.getGameRoomBO().setDataJsonRes(new Gson().toJson(this.getRoomEndResult()));
        // 设置结束时间
        this.getGameRoomBO().setEndTime(CommTime.nowSecond());
        // 时间
        this.getGameRoomBO().setDateTime(dateTime);
        //设置中至的时间
        this.setDateTimeZhongZhi(String.valueOf(CommTime.getCycleNowTime6YMD()));
        // 消耗值类型
        this.getGameRoomBO().setValueType(this.getValueType().value());
        // 消耗值
        this.getGameRoomBO().setConsumeValue(this.getConsumeValue());
        // 保存玩家头像信息和分数
        this.getGameRoomBO().setPlayerList(new Gson().toJson(this.getRoomPosMgr().getRoomPlayerPosList()));
        // 保存房间状态
        this.getGameRoomBO().setRoomState(RoomState.End.value());
        // 更新游戏人数
        this.getGameRoomBO().setPlayerNum(this.getPlayingCount());
        // 添加玩家个人游戏记录
        this.getRoomPosMgr().insertPlayerRoomAloneBO();
        // 更新玩家个人游戏记录
        this.getRoomPosMgr().updatePlayerRoomAloneBO(this.getConsumeValue(), dateTime, this.getGameRoomBO().getEndTime(), roomSportsPointConsume);
        // 赢家抽水
        this.addFactRoomSportsConsume(this.getRoomPosMgr().onWinnerC(false));
        // 竞技点消耗
        this.getGameRoomBO().setRoomSportsConsume(this.getFactRoomSportsConsume());
        // 更新
        this.getGameRoomBO().getBaseService().update(this.getGameRoomBO().getUpdateKeyValue(), this.getGameRoomBO().getId());
        // 竞技点收益分成点
        this.getRoomPosMgr().scorePoint(this.getFactRoomSportsConsume(), baseCreateRoom, String.valueOf(this.getGameRoomBO().getDateTime()), this.getGameRoomBO().getDataJsonCfg(), this.getGameRoomBO().getConsumeValue(), this.getHistorySetSize(), getGameRoomBO().getGameType());
        // 房间推广员活跃度计算
//		this.getRoomPosMgr().roomPromotionActiveEvent();
        // 抽奖任务
        this.getRoomTyepImpl().execLuckDrawCondition();
        //共享更新
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this);
        }
    }


    /**
     * 获取房间内的竞技点消耗
     */
    public double getRoomSportsPointConsume(BaseCreateRoom baseCreateRoom) {
        if (!RoomTypeEnum.UNION.equals(this.getRoomTypeEnum()) || this.existOneSetNotFinished()) {
            // 不是赛事房间
            return 0D;
        }
        if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
            //大局分数或者如果有一考的话 不用乘房间竞技点倍数
            if (this.checkDaJuFenShu() || this.calcFenUseYiKao()) {
                return this.bigWinnerConsume(baseCreateRoom, this.getRoomPosMgr().maxPoint());
            }

            // 大赢家消耗
            return this.bigWinnerConsume(baseCreateRoom, CommMath.mul(this.getRoomPosMgr().maxPoint(), Math.max(0, this.getRoomTyepImpl().getSportsDouble())));
        } else {
            //奖金池设置
            this.setSportsPointCost(baseCreateRoom.getPrizePool());
            // 房间竞技点每人消耗
            return CommMath.mul(this.getPlayingCount(), baseCreateRoom.getRoomSportsEveryoneConsume());
        }
    }


    /**
     * 大赢家消耗
     *
     * @param baseCreateRoom 创建房间配置
     * @param sportsPoint    赢的比赛分
     * @return
     */
    private double bigWinnerConsume(BaseCreateRoom baseCreateRoom, double sportsPoint) {
        List<UnionRoomSportsBigWinnerConsumeItem> bigWinnerConsumeList = baseCreateRoom.getBigWinnerConsumeList();
        if (CollectionUtils.isEmpty(bigWinnerConsumeList)) {
            //奖金池设置
            this.setSportsPointCost(baseCreateRoom.getPrizePool());
            // 没有区间列表，就直接用设置的固定值
            if (sportsPoint >= CommMath.FormatDouble(baseCreateRoom.getGeWinnerPoint())) {
                // 房间竞技点大赢家消耗
                return CommMath.FormatDouble(baseCreateRoom.getRoomSportsBigWinnerConsume());
            }
        } else {
            bigWinnerConsumeList = bigWinnerConsumeList.stream().sorted(Comparator.comparing(UnionRoomSportsBigWinnerConsumeItem::getWinScore).reversed()).limit(10).collect(Collectors.toList());
            UnionRoomSportsBigWinnerConsumeItem minItem = bigWinnerConsumeList.get(bigWinnerConsumeList.size() - 1);
            if (sportsPoint >= minItem.getWinScore()) {
                //奖金池设置
                this.setSportsPointCost(bigWinnerConsumeList.stream().filter(k -> sportsPoint >= k.getWinScore()).map(k -> k.getSportsPointCost()).findFirst().orElse(0D));
                return CommMath.FormatDouble(bigWinnerConsumeList.stream().filter(k -> sportsPoint >= k.getWinScore()).map(k -> k.getSportsPoint()).findFirst().orElse(0D));
            }
        }
        return 0D;
    }

    /**
     * 获取房间ID
     *
     * @return
     */
    public long getRoomID() {
        return this.gameRoomBO.getId();
    }

    /**
     * 添加房间内聊天记录
     *
     * @param chat 聊天记录
     */
    public void addChatList(ChatMessage chat) {
        this.chatList.add(chat);
    }

    /**
     * 房间内聊天记录数量
     *
     * @return
     */
    public int sizeChatList() {
        return this.chatList.size();
    }

    /**
     * 新一局
     */
    public abstract void startNewSet();

    /**
     * 获取当前局数的状况
     *
     * @param sec 秒
     * @return
     */
    public boolean getCurSetUpdate(int sec) {
        return this.curSet.update(sec);
    }

    /**
     * 添加历史局
     */
    public void addHistorySet() {
        if (!this.historySet.contains(this.getCurSet())) {
            this.historySet.add(this.getCurSet());
            // 房卡消费可以记录。
            if (PrizeType.RoomCard.equals(this.getBaseRoomConfigure().getPrizeType())) {
                String roomPlayerPosList = new Gson().toJson(this.getRoomPosMgr().getRoomPlayerPosList());
                this.setConsumeValue(this.getRoomTyepImpl().sumConsume());
                // 保存游戏玩家列表
                this.getGameRoomBO()
                        .getBaseService().update(
                                this.getGameRoomBO().getUpdateSet(CommTime.nowSecond(), roomPlayerPosList,
                                        this.getConsumeValue()),
                                this.getGameRoomBO().getId(), new AsyncInfo(this.getRoomID()));
                // 添加玩家个人游戏记录
                this.getRoomPosMgr().insertPlayerRoomAloneBO();
                // 保存每小局分数
                this.getRoomPosMgr().savaPlayerRoomAlonePoint(this.getGameRoomBO().getEndTime());
                // 设置当局在玩人数
                this.setPlayTheGameNumber(this.getPlayingCount());
            }
        }
    }

    /**
     * 获取总局数
     *
     * @return
     */
    public int getCount() {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getSetCount();
    }

    /**
     * 获取结束局数
     *
     * @return
     */
    public int getEndCount() {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getSetCount();
    }

    /**
     * 房间解散
     */
    public void doDissolveRoom() {
        // 房间解散
        this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
        this.giveBackRoomCard();
        // 丢弃当前牌局
        this.endRoom();
        this.getRoomPosMgr().notify2AllPublic(this.Dissolve(SBase_Dissolve.make(this.getRoomID(), false)));
    }

    /**
     * 解散房间
     *
     * @param ownnerForce T:房主解散,F:发起解散
     */
    public void doDissolveRoom(boolean ownnerForce) {
        // 房间解散
        this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
        this.exeDissolveRoom();
        // 通知所有人
        this.getRoomPosMgr().notify2AllPublic(this.Dissolve(SBase_Dissolve.make(this.getRoomID(), ownnerForce)));
    }

    /**
     * 解散房间
     */
    public void doSpecialDissolveRoom(DissolveNoticeTypeEnum dissolveNoticeTypeEnum, String msg) {
        // 房间解散
        this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
        this.exeDissolveRoom();
        // 通知所有人
        this.getRoomPosMgr().notify2AllPublic(this.Dissolve(SBase_Dissolve.make(this.getRoomID(), dissolveNoticeTypeEnum.ordinal(), msg)));
        if (Config.isShare()) {
            // 通知所有人
            this.getRoomPosMgr().notify2AllMq(SBase_Dissolve.make(this.getRoomID(), dissolveNoticeTypeEnum.ordinal(), msg));
        }
    }

    /**
     * 执行解散
     * 第一局中途或者没开始前解散回退
     */
    public void exeDissolveRoom() {
        if (getHistorySetSize() < 1 && PrizeType.RoomCard.equals(this.getBaseRoomConfigure().getPrizeType())) {
            this.giveBackRoomCard();
        }
        // 房间结算
        this.endRoom();
    }


    /**
     * 亲友圈操作房间解散
     */
    @Override
    public void doDissolveRoom(int dissolveNoticeType) {
        // 房间解散
        this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
        this.giveBackRoomCard();
        // 丢弃当前牌局
        this.endRoom();
        this.getRoomPosMgr().notify2All(this.Dissolve(SBase_Dissolve.make(this.getRoomID(), DissolveNoticeTypeEnum.CHANGE_ROOMCRG.ordinal(), false)));
    }

    /**
     * 获取房间公共配置
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public BaseRoomConfigure getBaseRoomConfigure() {
        return baseRoomConfigure;
    }

    /**
     * 修改房间公共配置
     *
     * @param baseRoomConfigure 房间公共配置
     */
    @Override
    public void setBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        this.baseRoomConfigure = baseRoomConfigure;
    }

    /**
     * 获取房间位置管理
     *
     * @return
     */
    public AbsRoomPosMgr getRoomPosMgr() {
        return roomPosMgr;
    }

    /**
     * 获取房主PID
     *
     * @return
     */
    public long getOwnerID() {
        return ownerID;
    }

    /**
     * 获取房间状态
     *
     * @return
     */
    @Override
    public RoomState getRoomState() {
        if (this.isEndRoom()) {
            return RoomState.End;
        }
        return this.getTask().getRoomState();
    }

    /**
     * 设置房间状态
     *
     * @param roomState 状态参数
     */
    public void setRoomState(RoomState roomState) {
        if (Objects.isNull(this.getTask())) {
            return;
        }
        this.getTask().setRoomState(roomState);
    }

    /**
     * 获取游戏人数
     *
     * @return
     */
    public int getPlayerNum() {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum();
    }

    /**
     * 清空结束房间
     */
    public abstract void clearEndRoom();

    /**
     * 设置定时器时间
     *
     * @return
     */
    public abstract int getTimerTime();

    /**
     * 游戏阶段人数
     *
     * @return
     */
    public abstract int getPlayingCount();

    /**
     * 聊天窗口设置牌
     */
    public abstract void godCardMsg(String msg, long pid);

    /**
     * 托管通知
     *
     * @param roomID      房间ID
     * @param pid         玩家PID
     * @param pos         位置
     * @param trusteeship T:托管
     * @return
     */
    public abstract BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship);

    /**
     * 位置离开
     *
     * @return
     */
    public abstract BaseSendMsg PosLeave(SBase_PosLeave posLeave);

    /**
     * 位置失去连接通知
     *
     * @param roomID        房间ID
     * @param pid           PID
     * @param isLostConnect T:失去连接,F:建立连接
     * @param isShowLeave   T:显示离开,F:正常
     * @return
     */
    public abstract BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect, boolean isShowLeave);

    /**
     * 位置继续游戏操作
     *
     * @param roomID 房间ID 位置
     * @return
     */
    public abstract BaseSendMsg PosContinueGame(long roomID, int pos);

    /**
     * 房间内位置更新
     *
     * @param roomID  房间ID
     * @param pos     位置
     * @param posInfo 玩家信息
     * @param custom  自定义数据
     * @return
     */
    public abstract BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom);

    /**
     * 位置准备操作通知
     *
     * @param roomID  房间ID
     * @param pos     位置
     * @param isReady T:准备成功,F:准备失败
     * @return
     */
    public abstract BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady);

    /**
     * 解散通知
     *
     * @param
     * @param
     * @return
     */
    public abstract BaseSendMsg Dissolve(SBase_Dissolve dissolve);

    /**
     * 特殊解散
     * -如联赛竞技点过低自动解散
     *
     * @param dissolve
     * @return
     */
    public BaseSendMsg SpecialDissolve(SBase_Dissolve dissolve) {
        return Dissolve(dissolve);
    }

    /**
     * 开始发起解散
     *
     * @param roomID    房间ID
     * @param createPos 发起人位置
     * @param endSec    结束时间
     * @return
     */
    public abstract BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec);

    /**
     * 位置操作解散
     *
     * @param roomID        房间ID
     * @param pos           位置ID
     * @param agreeDissolve T:同意，F：拒接
     * @return
     */
    public abstract BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve, int endSec);

    /**
     * 语音通知
     *
     * @param roomID 房间ID
     * @param pos    位置
     * @param url    地址
     * @return
     */
    public abstract BaseSendMsg Voice(long roomID, int pos, String url);

    /**
     * 洗牌通知
     *
     * @param roomID 房间ID
     * @param pid    玩家
     * @param cType  扑克，麻将
     * @return
     */
    public abstract BaseSendMsg XiPai(long roomID, long pid, ClassType cType);

    /**
     * 聊天消息
     *
     * @return
     */
    public abstract BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId,
                                            int quickID);

    /**
     * 房间记录
     *
     * @param records
     * @return
     */
    public abstract <T> BaseSendMsg RoomRecord(List<T> records);

    /**
     * 切换人数通知
     *
     * @param roomID    房间ID
     * @param createPos 发起者位置ID
     * @param endSec    结束时间
     * @param playerNum 人数
     * @return
     */
    public abstract BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum);

    /**
     * 切换人数同意、拒绝切换。
     *
     * @param roomID      房间ID
     * @param pos         位置ID
     * @param agreeChange 操作类型
     * @return
     */
    public abstract BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange);

    /**
     * 切换房间人数
     *
     * @param roomID     房间ID
     * @param roomKey    房间key
     * @param createType 创建类型
     * @return
     */
    public abstract BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType);

    /**
     * 获取房间信息
     *
     * @param pid 玩家PID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public abstract GetRoomInfo getRoomInfo(long pid);

    /**
     * 进入房间信息
     *
     * @return
     */
    public SRoom_EnterRoom getEnterRoomInfo() {
        if (Config.isShare()) {
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(this.roomKey);
            return SRoom_EnterRoom.make(this.getRoomID(), this.roomKey, this.getBaseRoomConfigure().getGameType().getId(), GameListConfigMgr.getInstance().getByRoom(shareRoom));
        } else {
            return SRoom_EnterRoom.make(this.getRoomID(), this.getBaseRoomConfigure().getGameType().getId());
        }
    }

    /**
     * 设置结束房间
     */
    public abstract void setEndRoom();

    /**
     * 结算
     */
    public abstract void calcEnd();

    /**
     * 机器人处理
     */
    public abstract void RobotDeal(int pos);

    @SuppressWarnings({"unchecked"})
    public <T> T getRecord() {
        return (T) RoomRecord(
                this.getHistorySet().stream().map(k -> k.getNotify_setEnd()).collect(Collectors.toList()));
    }

    /**
     * 房间公共结算
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomEndResult getRoomEndResult() {
        RoomEndResult sRoomEndResult = this.getRoomEndResultInfo();
        if (null != sRoomEndResult) {
            sRoomEndResult.setResultsList(Arrays.asList(sRoomEndResult.getResultsList().stream().filter(Objects::nonNull).toArray()));
            return sRoomEndResult;
        }
        sRoomEndResult = new RoomEndResult();
        sRoomEndResult.setKey(getRoomKey());
        sRoomEndResult.setRoomId(getRoomID());
        sRoomEndResult.setSetId(this.getHistorySetSize());
        sRoomEndResult.setEndTime(CommTime.nowSecond());
        sRoomEndResult.setResultsList(
                this.getRoomPosMgr().getPosList().stream().filter(k -> k.getPid() > 0L && k.isPlayTheGame() && null != k.getResults())
                        .map(k -> k.getResults()).collect(Collectors.toList()));
        sRoomEndResult.setOwnerID(this.getOwnerID());
        this.setRoomEndResultInfo(sRoomEndResult);
        return this.getRoomEndResultInfo();
    }

    /**
     * 获取练习场的基础分
     *
     * @return
     */
    public int getBaseMark() {
        if (PrizeType.Gold.equals(this.getBaseRoomConfigure().getPrizeType())) {
            return this.getBaseRoomConfigure().getRobotRoomCfg().getBaseMark();
        } else if (PrizeType.RoomCard.equals(this.getBaseRoomConfigure().getPrizeType())) {
            return 1;
        }
        return 0;
    }

    /**
     * 房间托管设置
     */
    public abstract void roomTrusteeship(int pos);

    /**
     * 获取托管循环时间
     */
    public abstract long getTrusteeshipUpdateTime();

    /**
     * 玩家取消托管
     */
    public abstract void cancelTrusteeship(AbsRoomPos pos);

    /**
     * 启动定时器
     */
    public abstract void startTrusteeShipTime();

    /**
     * 是否可以修改房间人数
     */
    public abstract boolean isCanChangePlayerNum();

    /**
     * 返回房卡
     **/
    public void giveBackRoomCard() {
        if (PrizeType.Gold.equals(this.getBaseRoomConfigure().getPrizeType())) {
            // 练习场模式下，不返回房卡
            return;
        }
        if (this.isBackRoomCard()) {
            // 已回退房卡，不能重复回退
            return;
        }
        // 已回退房卡
        this.setBackRoomCard(true);
        // 房卡回退
        this.getRoomTyepImpl().giveBackRoomCard();

    }

    /**
     * 推广领取列表
     *
     * @remark 待整理 到时候剥离此处重新生成新类
     */
    public void refererReceiveList() {
        if (getCurSetID() < 4) {
            return;
        }
        // TODO 2019/7/26
        // HashMap<String, Object> conditions = null;
        // for (AbsRoomPos pos : getRoomPosMgr().posList) {
        // RefererReceiveListBO rListBO =
        // refererReceiveListBOService.findOne(Restrictions.and(Restrictions.eq("pid",
        // pos.getPid()),Restrictions.eq("receive",
        // RefererEnum.RefererListState.MustNot.value())),false,null);
        // if (rListBO == null) {
        // continue;
        // }
        // rListBO.setCompleteCount(getCurSetID());
        // RankMgr.getInstance().addRefererRank(rListBO);
        // rListBO.setReceive(RefererEnum.RefererListState.Allow.value());
        // // 玩家在比赛场中点击“分享”按钮，邀请到新的玩家且该玩家在游戏中游戏局数超过4局后，赠送一张；
        // this.emailInfo(rListBO.getRefererId());
        // rListBO.getBaseService().saveOrUpDate(rListBO, new
        // AsyncInfo(rListBO.getId()));
        // }
    }


    /**
     * 删除回放
     */
    public void deleteRecordRelated() {
        long roomID = this.getRoomID();
        ContainerMgr.get().getComponent(GameRoomBOService.class).delete(roomID, new AsyncInfo(roomID));
        ContainerMgr.get().getComponent(GameSetBOService.class).delete(Restrictions.eq("roomID", roomID),
                new AsyncInfo(roomID));
        ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).delete(Restrictions.eq("roomID", roomID),
                new AsyncInfo(roomID));
    }

    /**
     * 检查是否有红包
     */
    public void checkHaveHongBao() {
        // 小于8局没有红包奖励
        if (getHistorySetSize() < 8) {
            return;
        }
        ActivityManager.getInstance().checkHaveHongBao(getRoomPosMgr().getRoomPidAll(),
                getBaseRoomConfigure().getGameType(), getRoomID());
    }

    /**
     * 获取房间配置
     *
     * @param <T>
     * @return
     */
    public abstract <T> T getCfg();

    /**
     * 房间解散状态
     *
     * @return
     */
    public RoomDissolutionState getRoomDissolutionState() {
        return Objects.isNull(this.getDissolveRoom()) ? this.roomDissolutionState : this.getDissolveRoom().getRoomDissolutionState();
    }

    public RoomDissolutionState getRoomSelfDissolutionState() {
        return this.roomDissolutionState;
    }

    /**
     * Compatibility API used by the standard game framework.
     */
    public String getGameName() {
        return this.getBaseRoomConfigure().getGameType().getName();
    }

    /**
     * Compatibility API: expose the effective dissolution state.
     */
    public RoomDissolutionState getRoomRealDissolutionState() {
        return this.getRoomDissolutionState();
    }

    /**
     * Whether the current set will finish the room.
     */
    public boolean checkIsRoomEnd() {
        return this.isRoomEnd
                || RoomDissolutionState.Dissolution.equals(this.getRoomDissolutionState())
                || this.getCurSetID() >= this.getCount();
    }

    /**
     * 深度拷贝
     * 房间战绩结算
     *
     * @param roomEndResultInfo
     */
    public void setRoomEndResultInfo(RoomEndResult roomEndResultInfo) {
        this.roomEndResultInfo = KryoUtil.Deserializer(KryoUtil.Serializer(roomEndResultInfo));
    }

    /**
     * 是否禁用魔法表情
     *
     * @return
     */
    public boolean isNotGift() {
        return this.getRoomTyepImpl().isNotGift();
    }

    /**
     * 房间是否结束
     *
     * @return
     */
    public boolean isEnd() {
        return false;
    }

    /**
     * 是否需要解散次数
     *
     * @return
     */
    public boolean needDissolveCount() {
        return false;
    }

    /**
     * 解散次数
     *
     * @return
     */
    public int getJieShanShu() {
        return 0;
    }

    /**
     * 检测解散次数
     *
     * @return
     */
    public boolean checkDissolveCount(AbsRoomPos roomPos) {
        if (needDissolveCount()) {
            boolean overCount = roomPos.getDissolveCount() >= getJieShanShu();
            if (!overCount) {
                roomPos.addDissolveCount();
            }
            return !overCount;
        } else {
            return true;
        }
    }

    /**
     * 检测需要解散
     *
     * @return
     */
    public boolean checkNeedDissolve() {
        return this.isTrusteeshipDissolve();
    }

    /**
     * 存在有玩家离开、踢出清空所有玩家准备状态
     *
     * @return T: 清空,F:不清空
     */
    public boolean existLeaveClearAllPosReady() {
        return false;
    }


    /**
     * 存在第一个局未打完解散不记录大赢家次数
     *
     * @return T:不记录,F:记录
     */
    public boolean existWinnerOneSetDissolve() {
        return false;
    }

    /**
     * 存在第一个未打完
     *
     * @return T:未打完,F:打完
     */
    public boolean existOneSetNotFinished() {
        return getHistorySetSize() < 1;
    }

    /**
     * 不存在第一个未打完
     *
     * @return T:打完,F:未打完
     */
    public boolean notExistOneSetNotFinished() {
        return !existOneSetNotFinished();
    }

    /**
     * 检查高级选项
     *
     * @return 高级选项
     */
    public boolean checkGaoJiXuanXiang(GaoJiTypeEnum typeEnum) {
        return this.getRoomTyepImpl().checkGaoJiXuanXiang(typeEnum);
    }

    /**
     * 30秒未准备自动退出
     *
     * @return
     */
    public boolean is30SencondTimeOut() {
        return false;
    }

    /**
     * 断线回来清除托管
     *
     * @return
     */
    public boolean isConnectClearTrusteeship() {
        return true;
    }


    /**
     * 是否禁止语音
     *
     * @return
     */
    public boolean isDisAbleVoice() {
        return false;
    }

    /**
     * 是否能超时继续
     *
     * @return
     */
    public boolean canContinue() {
        if (Objects.isNull(getCurSet()) || !getCurSet().isEnd()) {
            // 当局游戏还未结束
            return false;
        }
        return true;
    }

    /**
     * 需要立马打牌
     *
     * @return
     */
    public boolean needAtOnceOpCard() {
        return false;
    }

    /**
     * 竞技点能否低于零
     *
     * @return
     */
    public boolean isRulesOfCanNotBelowZero() {
        return false;
    }

    /**
     * 有没有大局分数
     *
     * @return
     */
    public boolean checkDaJuFenShu() {
        if (this.getPlayerNum() == 2 && RoomTypeEnum.UNION.equals(getRoomTypeEnum())) {
            return !RoomEndPointEnum.RoomEndPointEnum_normal.equals(RoomEndPointEnum.valueOf(this.getBaseRoomConfigure().getBaseCreateRoom().getDajusuanfen()));
        }
        return false;
    }

    /**
     * 有没有带多少赢多少
     *
     * @return
     */
    public boolean checkTakeLose() {
        return false;
    }

    /**
     * 算分是否用一考
     *
     * @return
     */
    public boolean calcFenUseYiKao() {
        return checkTakeLose() || checkDaJuFenShu() || isOnlyWinRightNowPoint() || isRulesOfCanNotBelowZero() || (isGuDingSuanFen() && RoomTypeEnum.UNION.equals(getRoomTypeEnum()));
    }

    /**
     * 分服版
     * 初始公共配置
     *
     * @param clazz 类
     */
    public void initShareBaseCreateRoom(Class<?> clazz, BaseRoomConfigure baseRoomConfigure) {
        if (Objects.nonNull(baseRoomConfigure.getShareBaseCreateRoom())) {
            baseRoomConfigure.setBaseCreateRoom(new Gson().fromJson(baseRoomConfigure.getShareBaseCreateRoom(), clazz));
        }
    }

    /**
     * 房间真实竞技点消耗
     *
     * @param factRoomSportsConsume
     */
    public void addFactRoomSportsConsume(double factRoomSportsConsume) {
        this.factRoomSportsConsume = CommMath.addDouble(this.factRoomSportsConsume, factRoomSportsConsume);
    }

    /**
     * 获取思考时间
     *
     * @return
     */
    public int getThinkTime() {
        return CommMath.randomInt(500, 2500);
    }

    /**
     * 是否显示推广员id
     *
     * @return
     */
    public boolean isNeedPromotionUpLevelID() {
        return false;
    }

    /**
     * 能托管继续
     *
     * @return
     */
    public boolean isCanTrusteeshipContinue() {
        return false;
    }

    /**
     * 只能赢当前身上分
     *
     * @return
     */
    public boolean isOnlyWinRightNowPoint() {
        return false;
    }

    /**
     * 是否固定算分
     *
     * @return double
     */
    public double getSuanFenNum() {
        return 0;
    }

    /**
     * 是否固定算分
     *
     * @return boolean
     */
    public boolean isGuDingSuanFen() {
        return false;
    }


    /**
     * 同ip解散房间同意
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result sameIpDissolveRoomAgree(long pid, boolean agreeD) {
        try {
            lock();
            if (Objects.isNull(this.getSameIpDissolveRoom())) {
                return SData_Result.make(ErrorCode.NotAllow, "sameIpDissolveRoomAgree null == this.getDissolveRoom()");
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 玩家不存在
                return SData_Result.make(ErrorCode.NotAllow, "sameIpDissolveRoomAgree null == roomPos PID:{%d}", pid);
            }
            if (this.getSameIpDissolveRoom().getSameIpPidList().contains(roomPos.getPid())) {
                return SData_Result.make(ErrorCode.NotAllow, "sameIpDissolveRoomAgree SameIpPidList PID:{%d}", pid);
            }

            if (this.getSameIpDissolveRoom().isDelay(CommTime.nowSecond())) {
                return SData_Result.make(ErrorCode.NotAllow, "sameIpDissolveRoomAgree Delay PID:{%d}", pid);
            }
            if (this.getSameIpDissolveRoom().deal(pid, agreeD)) {
                this.doDissolveRoom(false);
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    /**
     * 检查同ip解散房间
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkSameIpDissolveRoom(long pid) {
        try {
            lock();
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 玩家不存在
                return SData_Result.make(ErrorCode.NotAllow, "sameIpDissolveRoomAgree null == roomPos PID:{%d}", pid);
            }
            Set<Boolean> booleanSet = new HashSet<>();
            booleanSet.add(true);
            this.getRoomPosMgr().notify2ExistSameIp(new HashSet<>());
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }

    public SData_Result leavePos(long pid) {
        try {
            lock();
            // 玩家ID不存在。
            if (pid <= 0L) {
                return SData_Result.make(ErrorCode.NotAllow, "pid <= 0L PID:{%d}", pid);
            }
            AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 该玩家已经加入房间.
                return SData_Result.make(ErrorCode.NotAllow, "leavepos roomPos == null PID:{%d}", pid);
            }
            if (roomPos.isPlaying() && null == roomPos.getLiuzuoSec() && null != getCurSet() && getCurSet().getState() == SetState.Playing) {
                return SData_Result.make(ErrorCode.NotAllow, "leavepos Playing PID:{%d}", pid);
            }

            if (winOrLose.containsKey(pid) && winOrLose.get(pid) > 0) {
                return SData_Result.make(ErrorCode.NotAllow, "winOrLose > 0 PID:{%d}", pid);
            }

//			if (!getCurSet().isEnd()) {
//				// 一局没结束不能退出.
//				return SData_Result.make(ErrorCode.NotAllow, "null == start cannot leave seat PID:{}", pid);
//			}

            ClubMemberBO clubMemberBO = roomPos.getClubMemberBO();
            roomPos = this.getRoomPosMgr().addWatch();
            if (!roomPos.doWatch(pid, clubMemberBO)) {
                return SData_Result.make(ErrorCode.NotAllow, "enterRoom watch");
            }
            getRoomPosMgr().leavePos(pid);
            if (null != getCurSet() && getCurSet().getState() == SetState.Init) {
                getCurSet().reInit();
            }
            this.getOpChangePlayerRoom().changePlayerNumEnterRoom(roomPos.getPosID());
            this.getRoomPosMgr().notify2All(this.PosUpdate(this.getRoomID(), roomPos.getPosID(), roomPos.getNotify_PosInfo(), 0));
        } finally {
            unlock();
        }
        //共享更新
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this);
        }
        return SData_Result.make(ErrorCode.Success);
    }

    public void clearLiuzuo() {
        getRoomPosMgr().getRoomPosList().forEach(absRoomPos -> {
            if (null != absRoomPos.getLiuzuoSec() && 120 - CommTime.nowSecond() + absRoomPos.getLiuzuoSec() <= 0) {
                leavePos(absRoomPos.getPid());
            }
        });
    }

    /**
     * 是否支持超时后继续下一局，具体玩法可覆盖。
     */
    public boolean checkTimeOutContinue() {
        return true;
    }
}
