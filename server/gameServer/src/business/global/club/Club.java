package business.global.club;

import BaseCommon.CommLog;
import business.global.config.DiscountMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.shareclub.ShareClubListMgr;
import business.global.sharegm.ShareInitMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.Union;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerFamily;
import business.rocketmq.bo.MqDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import cenum.Page;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.redis.DistributedRedisLock;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import core.config.refdata.ref.RefRoomCost;
import core.db.entity.clarkGame.ClubGroupingBO;
import core.db.entity.clarkGame.ClubListBO;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubGroupingBOService;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.club.Club_define.Club_CreateGameSetStatus;
import jsproto.c2s.cclass.club.Club_define.Club_DISSOLVEROOM_STATUS;
import jsproto.c2s.cclass.club.Club_define.Club_OperationStatus;
import jsproto.c2s.cclass.club.Club_define.Club_Status;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionInfo;
import jsproto.c2s.iclass.club.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 具体单个亲友圈信息
 *
 * @author zaf
 */
@Data
public class Club implements Serializable {
    /**
     * 对应的数据库表
     */
    protected ClubListBO clubListBO;
    /**
     * 俱乐部房间配置
     */
    private Map<Long, ClubCreateGameSet> mCreateGamesetMap = new ConcurrentHashMap<>();
    /**
     * 标识配置Id
     */
    private Map<Integer, Long> limitMap = new ConcurrentHashMap<>();

    /**
     * 亲友圈创建者
     */
    private transient Player ownerPlayer = null;

    /**
     * 亲友圈创建者Id
     */
    private long ownerPlayerId = 0;
    /**
     * 亲友圈分组列表
     */
    private List<ClubGroupingBO> clubGroupingList;

    /**
     * 当前配置Id
     */
    private int curTabId = 1;
    /**
     * 钻石消耗通知 全员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean diamondsAttentionAll;
    /**
     * 钻石消耗通知 管理员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean diamondsAttentionMinister;
    /**
     * 亲友圈推广员归属批量修改
     */
    private boolean multiChangePromotionFlag;
    /**
     * 亲友圈推广员区间修改
     */
    private boolean sectionChangePromotionFlag;
    /**
     * 整个亲友圈分成修改
     */
    private boolean clubCreateShareFlag;

    public Club(ClubListBO clubListBO) {
        // 设置亲友圈
        this.setClubListBO(clubListBO);
        // 初始化亲友圈游戏配置
        this.initClubGameConfig();
        // 初始化亲友圈分组
        this.initClubGrouping();
    }


    public boolean updateOwnerPlayer(Player player) {
        // 设置圈主pid
        getClubListBO().saveOwnerID(player.getPid());
        getClubListBO().saveAgentsID(player.getFamiliID());
//        // 设置创建者信息
//        this.setOwnerPlayer(player);
        this.setOwnerPlayerId(player.getPid());
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(this, "ownerPlayerId");
        }
        return true;
    }

    /**
     * 初始亲友圈分组
     */
    private void initClubGrouping() {
        ClubGroupingBOService clubGroupingBOService = ContainerMgr.get().getComponent(ClubGroupingBOService.class);
        this.setClubGroupingList(clubGroupingBOService.findAll(Restrictions.eq("clubId", this.getClubListBO().getId()), ""));
        clubGroupingBOService = null;
    }

    /**
     * 获取亲友圈创建者
     *
     * @return
     */
    public Player getOwnerPlayer() {
        // 亲友圈创造者信息不存在
        if (this.getOwnerPlayerId() == 0) {
            // 设置创建者信息
            this.setOwnerPlayerId(this.getClubListBO().getOwnerID());
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().updateField(this, "ownerPlayerId");
            }
            return PlayerMgr.getInstance().getPlayer(this.getClubListBO().getOwnerID());
        } else {
            return PlayerMgr.getInstance().getPlayer(this.getOwnerPlayerId());
        }
    }

    /**
     * 初始化亲友圈游戏配置
     */
    @SuppressWarnings("rawtypes")
    private void initClubGameConfig() {
        String createGameSet = this.getClubListBO().getCreateGameSet();
        if (StringUtils.isEmpty(createGameSet)) {
            return;
        }
        createGameSet = createGameSet.trim();
        this.setMCreateGamesetMap(new Gson().fromJson(createGameSet,
                new TypeToken<ConcurrentHashMap<Long, ClubCreateGameSet>>() {
                }.getType()));
        // 检查创建房间配置是否存在
        if (MapUtils.isEmpty(this.getMCreateGamesetMap())) {
            this.setMCreateGamesetMap(new ConcurrentHashMap<>(16));
        }
        //共享的情况
        if (Config.isShare()) {
            Gson gson = new Gson();
            // 分配默认创建房间的索引
            for (ClubCreateGameSet clubCreateGameSet : this.getMCreateGamesetMap().values()) {
                if (Objects.isNull(clubCreateGameSet) || Objects.isNull(clubCreateGameSet.getbRoomConfigure()) || Objects.isNull(clubCreateGameSet.getbRoomConfigure().getClubRoomCfg())) {
                    continue;
                }
                //修改原来的房间数据为共享模式数据
                initCreateGameSetToShare(gson, clubCreateGameSet);
                if (ShareInitMgr.getInstance().getShareDataInit()) {
                    // 设置标识Id
                    clubCreateGameSet.setTagId(getCurTabId(clubCreateGameSet.getGameIndex()));
                } else {
                    Club clubShare = ShareClubListMgr.getInstance().getClub(this.clubListBO.getId());
                    this.limitMap = clubShare.limitMap;
                    this.curTabId = clubShare.curTabId;
                    // 设置标识Id
                    clubCreateGameSet.setTagId(getCurTabId(clubCreateGameSet.getGameIndex()));
                }
                ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(clubCreateGameSet.getRoomKey());
                if (shareRoom == null || shareRoom.getClubId() == 0 || ShareInitMgr.getInstance().getShareDataInit()) {
                    // 检查可返还的房卡数量 > 0
                    if (clubCreateGameSet.getRoomCard() > 0) {
//                this.gainClubRoomCard(clubCreateGameSet.getbRoomConfigure(), Club_OperationStatus.CLUB_OPERATION_STATUS_SERVER_RESTART);
                    }
                    // 重新解析房间配置
                    this.resetBaseCreateRoom(clubCreateGameSet, gson);
                    // 初始房卡值
                    clubCreateGameSet.setRoomCard(0);
                    // 初始房间数
                    clubCreateGameSet.setRoomCount(0);
                    // 初始房间号
                    clubCreateGameSet.setRoomKey("");
                    if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == clubCreateGameSet.getStatus()
                            || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == clubCreateGameSet
                            .getStatus()) {
                        // 跳过解散配置和禁止配置
                        continue;
                    }
                    // 初始房间号
                    clubCreateGameSet.setRoomKey("");
                    // 增加房间数
                    clubCreateGameSet.addRoomCount();
                    // 创建时间
                    clubCreateGameSet.setCreateTime(CommTime.nowSecond());
                    // 创建一个空亲友圈房间
                    clubCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneClubRoom(clubCreateGameSet.getbRoomConfigure(), 0));
                } else {
                    // 重新解析房间配置
                    this.resetBaseCreateRoom(clubCreateGameSet, gson);
                    // 初始房卡值
//                    clubCreateGameSet.setRoomCard(0);
                    // 初始房间数
//                    clubCreateGameSet.setRoomCount(0);
//                    if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == clubCreateGameSet.getStatus()
//                            || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == clubCreateGameSet
//                            .getStatus()) {
//                        // 跳过解散配置和禁止配置
//                        continue;
//                    }
                    // 创建一个空亲友圈房间
//                    clubCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneClubRoomShare(clubCreateGameSet.getbRoomConfigure(), clubCreateGameSet.getRoomKey(), 0));
                }
            }
            // 同步保存创建房间配置
            this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
        } else {
            Gson gson = new Gson();
            // 分配默认创建房间的索引
            for (ClubCreateGameSet clubCreateGameSet : this.getMCreateGamesetMap().values()) {
                if (Objects.isNull(clubCreateGameSet) || Objects.isNull(clubCreateGameSet.getbRoomConfigure()) || Objects.isNull(clubCreateGameSet.getbRoomConfigure().getClubRoomCfg())) {
                    continue;
                }
                // 检查可返还的房卡数量 > 0
                if (clubCreateGameSet.getRoomCard() > 0) {
//                this.gainClubRoomCard(clubCreateGameSet.getbRoomConfigure(), Club_OperationStatus.CLUB_OPERATION_STATUS_SERVER_RESTART);
                }
                // 重新解析房间配置
                this.resetBaseCreateRoom(clubCreateGameSet, gson);
                // 初始房卡值
                clubCreateGameSet.setRoomCard(0);
                // 初始房间数
                clubCreateGameSet.setRoomCount(0);
                // 初始房间号
                clubCreateGameSet.setRoomKey("");
                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == clubCreateGameSet.getStatus()
                        || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == clubCreateGameSet
                        .getStatus()) {
                    // 跳过解散配置和禁止配置
                    continue;
                }
                // 设置标识Id
                clubCreateGameSet.setTagId(getCurTabId(clubCreateGameSet.getGameIndex()));
                // 初始房间号
                clubCreateGameSet.setRoomKey("");
                // 增加房间数
                clubCreateGameSet.addRoomCount();
                // 创建时间
                clubCreateGameSet.setCreateTime(CommTime.nowSecond());
                // 创建一个空亲友圈房间
                clubCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneClubRoom(clubCreateGameSet.getbRoomConfigure(), 0));
            }
            // 同步保存创建房间配置
            this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
            //共享亲友圈更新
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().addClub(this);
            }
        }
    }

    /**
     * 设置房间配置数据为共享模式的数据
     *
     * @param gson
     * @param clubCreateGameSet
     */
    private void initCreateGameSetToShare(Gson gson, ClubCreateGameSet clubCreateGameSet) {
        Map<String, Object> clubCreateGameSetMap = gson.fromJson(gson.toJson(clubCreateGameSet), Map.class);
        //判断不是共享模式数据修改成共享模式的数据
        if (StringUtils.isEmpty(clubCreateGameSet.getbRoomConfigure().getShareBaseCreateRoom())) {
            Map<String, Object> bRoomConfigureMap = (Map) clubCreateGameSetMap.get("bRoomConfigure");
            Map<String, Object> baseCreateRoomMap = (Map) bRoomConfigureMap.get("baseCreateRoom");
            String shareBaseCreateRoom = gson.toJson(baseCreateRoomMap);
            clubCreateGameSet.getbRoomConfigure().setShareBaseCreateRoom(shareBaseCreateRoom);
            BaseCreateRoom baseCreateRoom = gson.fromJson(shareBaseCreateRoom, BaseCreateRoom.class);
            clubCreateGameSet.getbRoomConfigure().setBaseCreateRoom(baseCreateRoom);
        }

    }

    /**
     * 获取标识id
     *
     * @return
     */
    public int getCurTabId(long configId) {
        if (limitMap.containsValue(configId)) {
            return limitMap.entrySet().stream().filter(k -> k.getValue() == configId).map(k -> k.getKey()).findAny().orElse(0);
        }
        for (int i = 1; i <= curTabId; i++) {
            if (!this.limitMap.containsKey(i)) {
                this.limitMap.put(i, configId);
                //共享亲友圈更新
                if (Config.isShare()) {
                    ShareClubListMgr.getInstance().updateField(this, "limitMap", "curTabId");
                }
                return i;
            }
        }
        int id = ++curTabId;
        this.limitMap.put(id, configId);
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(this, "limitMap", "curTabId");
        }
        return id;
    }

    /**
     * 重新解析房间配置
     *
     * @param clubCreateGameSet 房间配置
     * @param gson
     */
    @SuppressWarnings({"unchecked"})
    private void resetBaseCreateRoom(ClubCreateGameSet clubCreateGameSet, Gson gson) {
        try {
            clubCreateGameSet.getbRoomConfigure().setBaseCreateRoom(gson.fromJson(gson.toJson(clubCreateGameSet.getbRoomConfigure().getBaseCreateRoomT()), BaseCreateRoom.class));
        } catch (JsonSyntaxException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }


    @SuppressWarnings("rawtypes")
    public void onUpdateClub(ClubListBO clubListBO) {
        this.setClubListBO(clubListBO);
        if (null != getMCreateGamesetMap()) {
            for (ClubCreateGameSet clubCreateGameSet : getMCreateGamesetMap().values()) {
                if (clubCreateGameSet.getStatus() == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() && clubCreateGameSet.getRoomCard() <= 0) {
                    // 初始房间号
                    clubCreateGameSet.setRoomKey("");
                    // 增加房间数
                    clubCreateGameSet.addRoomCount();
                    // 创建时间
                    clubCreateGameSet.setCreateTime(CommTime.nowSecond());
                    // 创建一个空亲友圈房间
                    clubCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance()
                            .createNoneClubRoom(clubCreateGameSet.getbRoomConfigure(), 0));
                }
            }
            // 同步保存创建房间配置
            this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
            //共享亲友圈更新
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().addClub(this);
            }
        }
    }


    /**
     * 返回亲友圈房卡
     *
     * @param baseRoomConfigure 房间公共配置
     * @param status            亲友圈操作状态
     */
    @SuppressWarnings("rawtypes")
    public void gainClubRoomCard(BaseRoomConfigure baseRoomConfigure, Club_OperationStatus status) {
        this.getOwnerPlayer().getFeature(PlayerWallet.class).backClubConsumeRoom(baseRoomConfigure.getClubRoomCfg().getRoomCard(), baseRoomConfigure.getGameType(), this.getClubListBO().getId(), status, this.getClubListBO().getAgentsID(), this.getClubListBO().getLevel());
        baseRoomConfigure.getClubRoomCfg().setRoomCard(0);
    }

    /**
     * 检查房卡配置和玩家房卡
     *
     * @param baseRoomConfigure 房间公共配置
     * @param status            亲友圈操作状态
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public SData_Result checkRefNoneClubRoomCost(BaseRoomConfigure baseRoomConfigure, Club_OperationStatus status) {
        SData_Result result = RefRoomCost.GetCost(baseRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        // 获取消耗
        int roomCard = DiscountMgr.getInstance().calculateRoomCardCost(getOwnerPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), getClubListBO().getClubsign(), 0L, baseRoomConfigure.getGameType().getId(), (int) result.getCustom());
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == baseRoomConfigure.getBaseCreateRoom().getPaymentRoomCardType()) {
            // 检查并亲友圈消耗房卡
            if (roomCard > 0 && !this.getOwnerPlayer().getFeature(PlayerWallet.class).checkAndClubConsumeRoom(roomCard, baseRoomConfigure.getGameType(), baseRoomConfigure.getBaseCreateRoom().getClubId(), status, this.getClubListBO().getAgentsID(), this.getClubListBO().getLevel())) {
                CommLog.error("checkRefNoneClubRoomCost NotEnough_RoomCard Pid:{},ConsumeCard:{},PlayerCard:{}", this.getOwnerPlayer().getPid(), roomCard, this.getOwnerPlayer().getRoomCard());
                return SData_Result.make(ErrorCode.NotEnough_Currency, "club owner room-card balance is insufficient");
            }
        } else {
            roomCard = 0;
        }
        return SData_Result.make(ErrorCode.Success, roomCard);
    }

    /**
     * 获取当前俱乐部自动创建房间的配置个数 正常的
     */
    public int getCreateGameSetNormalCount() {
        if (Config.isShare()) {
            Map<String, ShareRoom> allRooms = ShareRoomMgr.getInstance().allOneClubShareRooms(this.clubListBO.getId());
            return (int) this.getMCreateGamesetMap().values().stream()
                    .filter(k -> k.getStatus() == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value()
                            && ShareRoomMgr.getInstance().getOneRoomCfgCount(allRooms, this.clubListBO.getId(), k.getGameIndex()) - 1 > 0)
                    .count();
        }
        return (int) this.getMCreateGamesetMap().values().stream()
                .filter(k -> k.getStatus() == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value()
                        && k.getRoomCount() > 0)
                .count();
    }

    /**
     * 获取正常的设置
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ClubCreateGameSetInfo> getCreateGameSetNormalShare() {
        final Map<String, ShareRoom> allRooms = ShareRoomMgr.getInstance().allOneClubShareRooms(this.clubListBO.getId());
        return this.getMCreateGamesetMap().values().stream()
                .filter(k -> Objects.nonNull(k) && k.getStatus() != Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value())
                .map(k -> {
                    if (Objects.nonNull(k.getbRoomConfigure())) {
                        int roomCount = ShareRoomMgr.getInstance().getOneRoomCfgCount(allRooms, this.clubListBO.getId(), k.getGameIndex());
                        return new ClubCreateGameSetInfo(new Gson().fromJson(k.getbRoomConfigure().getShareBaseCreateRoom(), Map.class), k.getStatus(), roomCount - 1, k.getCreateTime(), k.getGameType().getName());
                    } else {
                        return null;
                    }
                }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
    }

    /**
     * 获取正常的设置
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ClubCreateGameSetInfo> getCreateGameSetNormal() {
        return this.getMCreateGamesetMap().values().stream()
                .filter(k -> Objects.nonNull(k) && k.getStatus() != Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value())
                .map(k -> {
                    if (Objects.nonNull(k.getbRoomConfigure())) {
                        return new ClubCreateGameSetInfo(k.getbRoomConfigure().getBaseCreateRoom(), k.getStatus(), k.getRoomCount(), k.getCreateTime(), k.getGameType().getName());
                    } else {
                        return null;
                    }
                }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
    }

    /**
     * 获取俱乐部游戏设置字符串
     */
    public String getCreateGameSetString() {
        return MapUtils.isEmpty(this.getMCreateGamesetMap()) ? "" : new Gson().toJson(this.getMCreateGamesetMap());
    }

    /**
     * 获取正常的设置
     *
     * @param player 玩家信息
     */
    @SuppressWarnings("rawtypes")
    public SData_Result getCreateGameSet(Player player) {
        if (Config.isShare()) {
            // 亲友圈房间类型分组
            Map<RoomState, Long> map = ShareRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, this.getClubListBO().getId());
            return SData_Result.make(ErrorCode.Success, SClub_GetCreateGameSet.make(this.getClubListBO().getId(),
                    ShareRoomMgr.Value(map, RoomState.Init),
                    ShareRoomMgr.Value(map, RoomState.Playing), getCreateGameSetNormalShare(),
                    this.getClubListBO().getMemberCreationRoom()));
        } else {
            // 亲友圈房间类型分组
            Map<RoomState, Long> map = NormalRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, this.getClubListBO().getId());
            return SData_Result.make(ErrorCode.Success, SClub_GetCreateGameSet.make(this.getClubListBO().getId(),
                    NormalRoomMgr.Value(map, RoomState.Init),
                    NormalRoomMgr.Value(map, RoomState.Playing), getCreateGameSetNormal(),
                    this.getClubListBO().getMemberCreationRoom()));
        }
    }

    /**
     * 判断是否创建的空房间已超过限制
     */
    public boolean checkCanCreateRoom() {
        return this.getCreateGameSetNormalCount() < GameConfig.CreateClubRoomLimit();
    }

    /**
     * 亲友圈游戏配置状态修改
     *
     * @param gameIndex 索引标记
     * @param status    状态
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createGameSetChange(long gameIndex, int status) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("clubcreateGameSetChange" + this.getClubListBO().getId() + gameIndex, uuid)) {
                // 获取亲友圈创建房间配置
                ClubCreateGameSet createGameSet = this.getMCreateGamesetMap().get(gameIndex);
                if (null == createGameSet) {
                    // 找不到配置
                    return SData_Result.make(ErrorCode.NotAllow, "null == createGameSet gameIndex{%d}", gameIndex);
                }
                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOT.equals(Club_CreateGameSetStatus.valueOf(status))) {
                    // 改变配置状态不正确
                    return SData_Result.make(ErrorCode.NotAllow, "CreateGameSetStatus Error:{%d,%s} ", status,
                            Club_CreateGameSetStatus.valueOf(status));
                }
                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() != status && createGameSet.getStatus() == status) {
                    // 不是设置正常配置状态,并且设置的状态和旧的状态一致。如：新设置的状态是 “解散”，原本的就状态也是“解散”，则返回错误。
//                    return SData_Result.make(ErrorCode.NotAllow,
//                            "not GFCLUB_CRATE_GAME_SET_STATUS_NOMARL And Old_Status:{%d} == New_Status:{%d} ",
//                            createGameSet.getStatus(), status);
                    CommLogD.info("not GFCLUB_CRATE_GAME_SET_STATUS_NOMARL And Old_Status:{} == New_Status:{} ",
                            createGameSet.getStatus(), status);
                    return SData_Result.make(ErrorCode.Success, createGameSet);
                }
                // 获取配置房间key
                String roomKey = createGameSet.getRoomKey();
                // 是否关闭配置
                boolean isClose = true;
                // 是否检查房间（（{解散操作} and 当前配置状态:{禁止}） or （{启动操作}））
                boolean checkRoomResult = (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.equals(Club_CreateGameSetStatus.valueOf(status)) && Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.equals(Club_CreateGameSetStatus.valueOf(createGameSet.getStatus()))) || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.equals(Club_CreateGameSetStatus.valueOf(status));
                if (!checkRoomResult) {
                    SData_Result result = this.roomResult(createGameSet, gameIndex, status);
                    CommLogD.info("禁止房间返回{}", new Gson().toJson(result));
                    if (!ErrorCode.Success.equals(result.getCode())) {
                        return result;
                    }
                }
                int oldStatus = createGameSet.getStatus();
                int tagId = createGameSet.getbRoomConfigure().getTagId();
                createGameSet.setRoomKey("");
                createGameSet.setStatus(status);
                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == status) {
                    // 解散房间配置
                    this.getMCreateGamesetMap().remove(gameIndex);
                    if (oldStatus == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value()) {
                        this.getLimitMap().remove(tagId);
                    }
                } else if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() == status) {
                    if (this.checkCanCreateRoom()) {
                        // 初始房间号
                        createGameSet.setRoomKey("");
                        // 增加房间数
                        createGameSet.addRoomCount();
                        // 创建时间
                        createGameSet.setCreateTime(CommTime.nowSecond());
                        // 创建一个空亲友圈房间
                        createGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneClubRoom(createGameSet.getbRoomConfigure(), 0));
                        createGameSet.setTagId(getCurTabId(createGameSet.getGameIndex()));
                        // 设置新的房间key
                        roomKey = createGameSet.getRoomKey();
                        CommLogD.info("启动时候创建房间{}", new Gson().toJson(createGameSet));
                        isClose = false;
                    }
                } else {
                    this.getLimitMap().remove(tagId);
                }
                this.createSetRoom(createGameSet, roomKey, 0L, isClose, false, false);
                // 同步保存游戏配置
                this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
                //共享亲友圈更新
                if (Config.isShare()) {
                    ShareClubListMgr.getInstance().addClub(this);
                    ShareRoomMgr.getInstance().setShareTagId(roomKey, createGameSet.getbRoomConfigure().getTagId());
                }
                return SData_Result.make(ErrorCode.Success, createGameSet);
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("clubcreateGameSetChange" + this.getClubListBO().getId() + gameIndex, uuid);
        }
    }

    /**
     * 获取操作状态
     *
     * @param status 状态
     * @return
     */
    private Club_OperationStatus getOperationStatus(int status) {
        Club_OperationStatus operationStatus = Club_OperationStatus.CLUB_OPERATION_STATUS_NOMARL;
        if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == status) {
            operationStatus = Club_OperationStatus.CLUB_OPERATION_STATUS_DELETE;
        } else if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() == status) {
            operationStatus = Club_OperationStatus.CLUB_OPERATION_STATUS_RESTART;
        } else if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == status) {
            operationStatus = Club_OperationStatus.CLUB_OPERATION_STATUS_DISABLE;
        }
        return operationStatus;
    }


    /**
     * 房间结果
     *
     * @param createGameSet 配置
     * @param gameIndex     游戏下标
     * @param status        配置状态
     * @return
     */
    @SuppressWarnings("rawtypes")
    private SData_Result roomResult(ClubCreateGameSet createGameSet, long gameIndex, int status) {
        if (Config.isShare()) {
            List<ShareRoom> roomInitList = ShareRoomMgr.getInstance().getRoomInitList(gameIndex, this.getClubListBO().getId(), RoomTypeEnum.CLUB);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (ShareRoom shareRoom : roomInitList) {
                    // 房间已经结束
                    if (RoomState.End.equals(shareRoom.getRoomState())) {
                        continue;
                    }
                    if (RoomState.Init.equals(shareRoom.getRoomState())) {
                        //空配置房间
                        if (shareRoom.isNoneRoom()) {
                            // 房间解散
                            ShareRoomMgr.getInstance().doDissolveRoom(shareRoom);
                        } else {//正常房间通知游戏解散
                            MqDissolveRoomNotifyBo bo = new MqDissolveRoomNotifyBo(shareRoom.getRoomKey(), shareRoom.getCurShareNode());
                            // 通知解散房间
                            MqProducerMgr.get().send(MqTopic.DISSOLVE_ROOM_NOTIFY, bo);
                        }
                    }
                }
            }
        } else {
            // 获取当前配置的空房间
            List<RoomImpl> roomInitList = NormalRoomMgr.getInstance().getRoomInitList(gameIndex, this.getClubListBO().getId(), RoomTypeEnum.CLUB);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (RoomImpl roomImpl : roomInitList) {
                    // 房间已经结束
                    if (roomImpl.isEndRoom()) {
                        continue;
                    }
                    if (RoomState.Init.equals(roomImpl.getRoomState())) {
                        // 房间解散
                        roomImpl.doDissolveRoom(Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
                    }
                }
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 放回预开的房间
     */
    public void gainRoomReservation() {
        // 亲友圈创建房间配置
        ClubCreateGameSet createGameSet = null;
        // 亲友圈预开房列表
        List<RoomImpl> rooms = NormalRoomMgr.getInstance().roomValues(RoomTypeEnum.CLUB, this.getClubListBO().getId());
        for (RoomImpl room : rooms) {
            // 房间初始状态
            if (RoomState.Init.equals(room.getRoomState())) {
                // 房间解散
                room.doDissolveRoom(
                        Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_GAMESERVER_MAINTENACE.value());
                // 获取亲友圈创建房间配置
                createGameSet = this.getMCreateGamesetMap().get(room.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex());
                // 检查是否存在
                if (Objects.nonNull(createGameSet)) {
                    // 更新配置
                    createGameSet.setRoomConfigure(room.getBaseRoomConfigure());
                }
            }
        }
        // 同步保存游戏配置
        this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().addClub(this);
        }
    }

    /**
     * 关闭所有房间
     *
     * @return
     */
    public boolean close() {
        // 清空房间创建配置
        this.getMCreateGamesetMap().clear();
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(this, "mCreateGamesetMap");
        }
        // 并保存
        this.getClubListBO().setCreateGameSet(this.getCreateGameSetString());
        if (Config.isShare()) {
            List<ShareRoom> rooms = ShareRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, this.getClubListBO().getId());
            for (ShareRoom room : rooms) {
                // 房间初始状态
                if (RoomState.Init.equals(room.getRoomState())) {
                    // 房间解散
                    ShareRoomMgr.getInstance().doDissolveRoom(room);
                }
            }
        } else {
            // 亲友圈预开房列表
            List<RoomImpl> rooms = NormalRoomMgr.getInstance().roomValues(RoomTypeEnum.CLUB, this.getClubListBO().getId());
            for (RoomImpl room : rooms) {
                // 房间初始状态
                if (RoomState.Init.equals(room.getRoomState())) {
                    // 房间解散
                    room.doDissolveRoom(Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_CLOSE.value());
                }
            }
        }

        // 解散时间
        this.getClubListBO().setDistime(CommTime.nowSecond());
        // 改变亲友圈状态
        this.getClubListBO().setStatus(Club_Status.CLUB_STATUS_CLOSE.value());
        // 关闭亲友圈状态保存
        this.getClubListBO().closeClub();
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(this, "clubListBO");
        }

        return true;
    }


    /**
     * 游戏开始或者房间解散新增空房间
     */
    public void createNewSetRoom(long gameIndex, BaseRoomConfigure baseRoomConfigure, String roomKey) {
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(this.getClubListBO().getId(), SClub_RoomStatusChange.make(newRoomInfoItem(baseRoomConfigure, roomKey, false)));
        ClubCreateGameSet createGameSet = this.getMCreateGamesetMap().get(gameIndex);
        if (Objects.nonNull(createGameSet)) {
            createGameSet.addRoomCount();
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().addRoomCount(this.clubListBO.getId(), gameIndex);
            }
        }

    }

    /**
     * 俱乐部房间玩家进入或者退出
     *
     * @param room 房间信息
     * @param pos  当前操作玩家位置信息
     */
    public void onClubRoomPlayerChange(AbsBaseRoom room, RoomPosInfo pos, int sort) {
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(this.getClubListBO().getId(), SClub_RoomPlayerChange.make(this.getClubListBO().getId(), room.getRoomID(), room.getRoomKey(), pos, sort));
        if (room.getRoomPosMgr().checkExistNoOne()) {
            // 解散房间
            room.doDissolveRoom(Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_NO_PLAYEER.value());
        }
    }

    /**
     * 亲友圈房间结束移除
     *
     * @param gameIndex 亲友圈配置标识
     * @param roomKey   房间号
     */
    @SuppressWarnings("rawtypes")
    public void onClubRoomRemove(long gameIndex, String roomKey, int sort) {
        ClubCreateGameSet createGameSet = this.getMCreateGamesetMap().get(gameIndex);
        if (Objects.isNull(createGameSet)) {
            return;
        }
        createGameSet.subRoomCount();
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().subRoomCount(this.clubListBO.getId(), gameIndex);
        }
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setId(this.getClubListBO().getId());
        roomInfoItem.setRoomKey(roomKey);
        roomInfoItem.setSort(sort);
        roomInfoItem.setTagId(createGameSet.getbRoomConfigure().getTagId());
        roomInfoItem.setClose(true);
        // 通知俱乐部玩家
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(this.getClubListBO().getId(), SClub_RoomStatusChange.make(roomInfoItem));
        ClubMgr.getInstance().getClubMemberMgr().notifyRoomCountChange(this.getClubListBO().getId(), 0L, getClubCreateGameSetInfo(createGameSet), this.getClubListBO().getMemberCreationRoom(), false);
    }


    /**
     * 添加游戏设置
     *
     * @param pid           玩家PID
     * @param createGameSet 亲友圈创建房间游戏配置
     * @param isCreate      T:创建,F:修改
     */
    public void setCreateGameSet(long pid, ClubCreateGameSet createGameSet, boolean isCreate) {
        // 同步保存游戏配置
        this.getClubListBO().saveSyncCreateGameSet(this.getCreateGameSetString());
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().addClub(this);
        }
        // 开启新房间
        this.createSetRoom(createGameSet, pid, false, true, isCreate);
    }

    /**
     * @param createGameSet
     * @param pid
     * @param isClose
     * @param isNotifyCreateChange
     * @param isCreate
     * @return
     */
    public boolean createSetRoom(ClubCreateGameSet createGameSet, long pid, boolean isClose, boolean isNotifyCreateChange, boolean isCreate) {
        return createSetRoom(createGameSet, createGameSet.getbRoomConfigure().getClubRoomCfg().getRoomKey(), pid, isClose, isNotifyCreateChange, isCreate);
    }

    /**
     * 开启新房间
     **/
    @SuppressWarnings({"rawtypes"})
    public boolean createSetRoom(ClubCreateGameSet createGameSet, String roomKey, long pid, boolean isClose, boolean isNotifyCreateChange, boolean isCreate) {
        if (Objects.isNull(createGameSet)) {
            return false;
        }
        // 通知俱乐部玩家
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(this.getClubListBO().getId(), SClub_RoomStatusChange.make(newRoomInfoItem(createGameSet.getbRoomConfigure(), roomKey, isClose)));
        // 通知修改设置
        if (isNotifyCreateChange) {
            ClubMgr.getInstance().getClubMemberMgr().notifyRoomCountChange(this.getClubListBO().getId(), pid, getClubCreateGameSetInfo(createGameSet), this.getClubListBO().getMemberCreationRoom(), isCreate);
            if (isCreate) {
                // 创建房间
                UnionDynamicBO.insertClubGameConfig(pid, getClubListBO().getId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_CREATE_ROOM.value());
            } else {
                // 修改房间
                UnionDynamicBO.insertClubGameConfig(pid, getClubListBO().getId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_UPDATE_ROOM.value());
            }
        }
        return true;
    }

    /***
     * 新创建房间信息配置
     * @param baseRoomConfigure
     * @param roomKey
     * @param isClose
     * @return
     */
    public RoomInfoItem newRoomInfoItem(BaseRoomConfigure baseRoomConfigure, String roomKey, boolean isClose) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName(baseRoomConfigure.getBaseCreateRoom().getRoomName());
        roomInfoItem.setId(this.getClubListBO().getId());
        roomInfoItem.setRoomKey(roomKey);
        roomInfoItem.setGameId(baseRoomConfigure.getGameType().getId());
        roomInfoItem.setSetCount(baseRoomConfigure.getBaseCreateRoom().getSetCount());
        roomInfoItem.setPlayerNum(baseRoomConfigure.getBaseCreateRoom().getPlayerNum());
        roomInfoItem.setSort(RoomSortedEnum.NONE_ROOM.ordinal());
        roomInfoItem.setTagId(baseRoomConfigure.getTagId());
        roomInfoItem.setClose(isClose);
        roomInfoItem.setPassword(baseRoomConfigure.getBaseCreateRoom().getPassword());
        return roomInfoItem;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClubCreateGameSetInfo getClubCreateGameSetInfo(ClubCreateGameSet createGameSet) {
        if (Config.isShare()) {
            Map<String, ShareRoom> allRooms = ShareRoomMgr.getInstance().allOneClubShareRooms(this.clubListBO.getId());
            return new ClubCreateGameSetInfo(createGameSet.getbRoomConfigure().getBaseCreateRoom(), createGameSet.getStatus(), ShareRoomMgr.getInstance().getOneRoomCfgCount(allRooms, this.clubListBO.getId(), createGameSet.getGameIndex()) - 1, createGameSet.getCreateTime(), createGameSet.getGameType().getName());
        } else {
            return new ClubCreateGameSetInfo(createGameSet.getbRoomConfigure().getBaseCreateRoom(), createGameSet.getStatus(), createGameSet.getRoomCount(), createGameSet.getCreateTime(), createGameSet.getGameType().getName());
        }

    }

    /**
     * 获取指定亲友圈分组成员列表
     *
     * @param groupingID
     */
    public List<ClubGroupingMemberInfo> getClubGroupingMemberList(long groupingID) {
        // 获取指定分组
        ClubGroupingBO result = this.clubGroupingList.stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return null;
        }
        return result.getGroupingToList().stream().map(k -> {
            Player player = PlayerMgr.getInstance().getPlayer(k);
            if (Objects.nonNull(player)) {
                return new ClubGroupingMemberInfo(groupingID, player.getShortPlayer(), true);
            }
            return null;
        }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
    }

    /**
     * 求有交集
     *
     * @param pidList      玩家列表（除去玩家本身）
     * @param groupingList 分组列表
     * @return
     */
    private boolean retainAll(List<Long> pidList, List<Long> groupingList) {
        pidList.retainAll(groupingList);
        return pidList.size() > 0;
    }

    /**
     * 检查分组禁令
     *
     * @param pid     玩家ID
     * @param pidList []房间内的玩家ID
     * @return
     */
    public String checkGroupingBan(long pid, List<Long> pidList) {
        // 房间内的玩家ID
        if (CollectionUtils.isEmpty(pidList)) {
            return null;
        }
        // 检查亲友圈分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getClubGroupingList())) {
            return null;
        }
        pidList.remove(pid);
        // 获取禁止的分组信息
        List<List<Long>> groupingBOList = this.getClubGroupingList().stream()
                .filter(x -> x.getGroupingToList().contains(pid) && this.retainAll(Lists.newArrayList(pidList), x.getGroupingToList())).map(k -> k.getGroupingToList()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(groupingBOList)) {
            return null;
        }
        String name = null;
        Player player = null;
        // 遍历所有玩家。
        for (Long roomPid : pidList) {
            // 检查 Pid是否存在,组中。
            if (groupingBOList.stream().anyMatch(k -> k.contains(roomPid))) {
                // 获取玩家信息
                player = PlayerMgr.getInstance().getPlayer(roomPid);
                // 检查是否存在
                if (Objects.isNull(player)) {
                    continue;
                }
                return player.getShortPlayer().getName();
            }
        }
        CommLog.error("club name pid :{},pidList :{}", pid, pidList);
        return name;
    }


    /**
     * 将玩家添加到指定组中。
     *
     * @param groupingID 分组ID
     * @param pid        玩家ID
     * @return
     */
    public SData_Result addClubGroupingPid(long groupingID, long pid) {
        // 检查亲友圈分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getClubGroupingList())) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_GROUPING, "UNION_NOT_EXIST_GROUPING");
        }
        ClubGroupingBO result = this.getClubGroupingList().stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return SData_Result.make(ErrorCode.UNION_GROUPING_ID_ERROR, "UNION_GROUPING_ID_ERROR");
        }
        // 玩家加入该分组。
        if (result.addGrouping(pid)) {
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().updateField(this, "clubGroupingList");
            }
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.UNION_GROUPING_PID_EXIST, "UNION_GROUPING_PID_EXIST");
        }
    }


    /**
     * 将玩家从指定组中移除。
     *
     * @param groupingID 分组ID
     * @param pid        玩家ID
     * @return
     */
    public SData_Result removeClubGroupingPid(long groupingID, long pid) {
        // 检查亲友圈分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getClubGroupingList())) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_GROUPING, "UNION_NOT_EXIST_GROUPING");

        }
        ClubGroupingBO result = this.getClubGroupingList().stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return SData_Result.make(ErrorCode.CLUB_GROUPING_ID_ERROR, "UNION_GROUPING_ID_ERROR");
        }
        // 玩家移出该分组。
        if (result.removeGrouping(pid)) {
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().updateField(this, "clubGroupingList");
            }
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.CLUB_GROUPING_PID_NOT_EXIST, "UNION_GROUPING_PID_NOT_EXIST");
        }
    }


    /**
     * 增加分组
     */
    public ClubGroupingInfo addClubGrouping() {
        if (Objects.isNull(this.clubGroupingList)) {
            this.clubGroupingList = new ArrayList<>();
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().updateField(this, "clubGroupingList");
            }
            return null;
        }
        ClubGroupingBO cGroupingBO = new ClubGroupingBO();
        cGroupingBO.setClubID(this.getClubListBO().getId());
        cGroupingBO.setCreateTime(CommTime.nowSecond());
        cGroupingBO.setGrouping("");
        cGroupingBO.getBaseService().saveOrUpDate(cGroupingBO);
        this.clubGroupingList.add(cGroupingBO);
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(this, "clubGroupingList");
        }
        return new ClubGroupingInfo(cGroupingBO.getId());
    }

    /**
     * 移除分组
     */
    public ClubGroupingInfo removeClubGrouping(long clubGroupingId) {
        if (CollectionUtils.isEmpty(this.clubGroupingList)) {
            return null;
        }
        for (int i = 0, size = this.clubGroupingList.size(); i < size; i++) {
            if (this.clubGroupingList.get(i).getId() == clubGroupingId) {
                // 移除分组并删除。
                this.clubGroupingList.remove(i).del();
                if (Config.isShare()) {
                    ShareClubListMgr.getInstance().updateField(this, "clubGroupingList");
                }
                return new ClubGroupingInfo(clubGroupingId);
            }
        }
        return null;
    }

    /**
     * 获取亲友圈分组列表
     *
     * @return
     */
    public List<ClubGroupingBO> getClubGroupingList() {
        return this.clubGroupingList;
    }

    /**
     * 获取玩家分组列表
     *
     * @return
     */
    public List<ClubGroupingInfo> getClubGroupingInfoList(CClub_BanGame req, long doPid) {
        // 分组列表
        List<ClubGroupingInfo> clubGroupingInfos = new ArrayList<>();
        // 检查分组数据是否存在。
        if (CollectionUtils.isEmpty(this.clubGroupingList)) {
            return clubGroupingInfos;
        }
        //可以查询的pid  为空则全部可查
        List<Long> pidList = new ArrayList<>();
        //如果是亲友圈 圈主或者管理员
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, doPid)) {
            //获取当前查询的亲友圈成员信息
            ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, doPid);
            if (doClubMember.isLevelPromotion()) {
                this.getClubMemberPlayerIDList(doClubMember.getId(), pidList);
            } else if (doClubMember.isPromotionManage()) {
                this.getClubMemberPlayerIDList(doClubMember.getClubMemberBO().getUpLevelId(), pidList);
            } else {
                return clubGroupingInfos;
            }
        }
        this.clubGroupingList.sort((ClubGroupingBO itme1, ClubGroupingBO itme2) -> (int) itme1.getId() - (int) itme2.getId());
        return this.clubGroupingList.stream().filter(k -> null != k).map(k -> {
            return new ClubGroupingInfo(k.getId(), k.getGroupingToList().size(), k.getGroupingToList().stream().filter(pid -> null != pid && pid > 0L).map(pid -> {
                Player player = PlayerMgr.getInstance().getPlayer(pid);
                if (null != player) {

                    return player.getShortPlayer();
                } else {
                    return null;
                }
            }).filter(shortPlayer -> null != shortPlayer).collect(Collectors.toList()));
        }).filter(k -> {
            //为空的可以显示
            if (CollectionUtils.isEmpty(k.getPlayerList())) {
                return true;
            }
            if (CollectionUtils.isNotEmpty(pidList)) {
                //说明是推广员  如果不包含的话 直接不选择
                if (pidList.stream().noneMatch(m -> k.getPlayerList().stream().anyMatch(n -> n.getPid() == m.intValue()))) {
                    return false;
                }
            }
            if (StringUtils.isEmpty(req.getPidOne()) && StringUtils.isEmpty(req.getPidTwo())) {
                return true;
            }
            for (jsproto.c2s.cclass.Player.ShortPlayer con : k.getPlayerList()) {
                if (StringUtils.isNotEmpty(req.getPidOne())) {
                    if (String.valueOf(con.getPid()).contains(req.getPidOne()) || con.getName().contains(req.getPidOne())) {
                        return true;
                    }
                }
                if (StringUtils.isNotEmpty(req.getPidTwo())) {
                    if (String.valueOf(con.getPid()).contains(req.getPidTwo()) || con.getName().contains(req.getPidTwo())) {
                        return true;
                    }
                }
            }
            return false;
        }).map(k -> new ClubGroupingInfo(k.getGroupingId(), k.getGroupingSize(), k.getPlayerList().stream().limit(2).collect(Collectors.toList()))).collect(Collectors.toList());
    }

    /**
     * 获取所有下线包括自己
     *
     * @param doMememberID 查询人memeberid
     * @param pidList      结果
     */
    public void getClubMemberPlayerIDList(Long doMememberID, List<Long> pidList) {
        //查出自己的所有上线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", doMememberID), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 推广员id列表
        List<Long> uidList = com.google.common.collect.Lists.newArrayList();
        uidList.add(doMememberID);
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 查询我的所有上线（包括我）：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId).reversed()).map(k -> k.getUid()).collect(Collectors.toList()));
        }
        uidList.stream().forEach(k -> {
            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k);
            if (Objects.nonNull(clubMember)) {
                pidList.add(clubMember.getClubMemberBO().getPlayerID());
            }
        });
    }

    public ClubInvitedInfo getClubInvitedInfo() {
        ClubInvitedInfo gInfo = new ClubInvitedInfo();
        gInfo.setClubId(this.getClubListBO().getId());
        gInfo.setClubName(this.getClubListBO().getName());
        gInfo.setClubsign(this.getClubListBO().getClubsign());
        return gInfo;
    }

    /**
     * 通知局数变化
     */
    public void roomSetIDChange(long clubId, long roomID, String roomKey, int setID, int sort) {
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(clubId, SClub_RoomSetChange.make(clubId, roomID, roomKey, setID, sort));
    }

    /**
     * 通知开始
     */
    public void roomStartChange(long clubId, String roomKey) {
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(clubId, SClub_RoomStartChange.make(clubId, roomKey, RoomState.Playing.value()));
    }

    /**
     * 获取亲友圈信息
     *
     * @return
     */
    public ClubInfo getClubInfo() {
        // 亲友圈已解散
        if (this.getClubListBO().getStatus() == Club_Status.CLUB_STATUS_CLOSE.value()) {
            return null;
        }
        ClubInfo info = new ClubInfo();
        // 玩家头像
        info.setPlayer(getOwnerPlayer().getShortPlayer());
        // 亲友圈ID
        info.setId(this.getClubListBO().getId());
        // 亲友圈名称
        info.setName(this.getClubListBO().getName());
        // 亲友圈key
        info.setClubsign(this.getClubListBO().getClubsign());
        // 创建时间
        info.setCreattime(this.getClubListBO().getCreattime());
        // 公告
        info.setNotice(this.getClubListBO().getNotice());
        // 代理等级
        info.setLevel(this.getClubListBO().getLevel());
        // 代理ID
        info.setAgentsID(this.getClubListBO().getAgentsID());
        // 亲友圈人数
        info.setPeopleNum(ClubMgr.getInstance().getClubMemberMgr().clubPeopleNum(this.getClubListBO().getId()));
        // 统计当前房间数量
        Map<RoomState, Long> map = NormalRoomMgr.getInstance().groupingBy(this.getClubListBO().getUnionId() <= 0L ? RoomTypeEnum.CLUB : RoomTypeEnum.UNION, this.getClubListBO().getUnionId() <= 0L ? this.getClubListBO().getId() : this.getClubListBO().getUnionId());
        // 等待中房间
        info.setWaitRoomCount(NormalRoomMgr.Value(map, RoomState.Init));
        // 游戏中房间
        info.setPlayingRoomCount(NormalRoomMgr.Value(map, RoomState.Playing));
        // 所有的房间数
        info.setRoomCount(NormalRoomMgr.Value(map, RoomState.Init) + NormalRoomMgr.Value(map, RoomState.Playing));
        ///加入是否需要审核
        info.setJoinNeedExamine(this.getClubListBO().getJoin());
        //退出是否需要审核
        info.setQuitNeedExamine(this.getClubListBO().getQuit());
        //俱乐部全员钻石提醒
        info.setDiamondsAttentionAll(this.getClubListBO().getDiamondsAttentionAll());
        //俱乐部管理员钻石提醒
        info.setDiamondsAttentionMinister(this.getClubListBO().getDiamondsAttentionMinister());
        ClubPromotionShowConfig config = this.getClubListBO().getPromotionShowClubConfigJson();
        //推广员显示列表
        info.setPromotionShow(config.getShowConfig());
        //获取皮肤类型
        info.setSkinType(this.getClubListBO().getSkinType());
        if (this.isZhongZhiClub()) {
            //中至模式下  傅哥说要下发2 20220610
            info.setSkinType(2);
        }
//         图标
//        info.setHeadImages(ClubMgr.getInstance().getClubMemberMgr().findClubIdLimitHeadImage(this.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU.value(), 9));
        return info;
    }

    /**
     * 获取亲友圈信息
     * 简化版
     *
     * @return
     */
    public ClubInfoShort getClubInfoShort() {
        // 亲友圈已解散
        if (this.getClubListBO().getStatus() == Club_Status.CLUB_STATUS_CLOSE.value()) {
            return null;
        }
        ClubInfoShort info = new ClubInfoShort();
        // 玩家头像
        info.setPlayer(getOwnerPlayer().getShortPlayer());
        // 亲友圈ID
        info.setId(this.getClubListBO().getId());
        // 亲友圈名称
        info.setName(this.getClubListBO().getName());
        // 亲友圈key
        info.setClubsign(this.getClubListBO().getClubsign());
        // 代理等级
        info.setLevel(this.getClubListBO().getLevel());
        // 代理ID
        info.setAgentsID(this.getClubListBO().getAgentsID());
        //获取皮肤类型
        info.setSkinType(this.getClubListBO().getSkinType());
        if (this.isZhongZhiClub()) {
            //中至模式下  傅哥说要下发2 20220610
            info.setSkinType(2);
        }
        return info;
    }

    /**
     * 获取亲友圈信息
     * 简化版2
     *
     * @return
     */
    public ClubInfoShort2 getClubInfoShort2() {
        // 亲友圈已解散
        if (this.getClubListBO().getStatus() == Club_Status.CLUB_STATUS_CLOSE.value()) {
            return null;
        }
        ClubInfoShort2 info = new ClubInfoShort2();
        // 亲友圈ID
        info.setId(this.getClubListBO().getId());
        // 亲友圈名称
        info.setName(this.getClubListBO().getName());
        // 亲友圈key
        info.setClubsign(this.getClubListBO().getClubsign());
        info.setSkinType(this.getClubListBO().getSkinType());
        Player player = PlayerMgr.getInstance().getPlayer(this.getOwnerPlayerId());
        if (Objects.nonNull(player)) {
            info.setClubCreateName(player.getName());

        }
        if (this.isZhongZhiClub()) {
            //中至模式下  傅哥说要下发2 20220610
            info.setSkinType(2);
        }
        return info;
    }

    /**
     * 获取赛事信息
     *
     * @return
     */
    public UnionInfo getUnionInfo(long pid) {
        if (this.getClubListBO().getUnionId() > 0L) {
            // 获取赛事信息
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getClubListBO().getUnionId());
            if (Objects.isNull(union)) {
                return null;
            }
            if (this.getClubListBO().getOwnerID() == pid) {
                UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, this.getClubListBO().getId(), this.getClubListBO().getUnionId());
                if (null != unionMember) {
                    // 圈主及以上的成员
                    return new UnionInfo(union.getUnionBO().getId(), union.getUnionBO().getName(), unionMember.getUnionMemberBO().getType(), union.getUnionBO().getUnionSign(), union.getUnionBO().getEndRoundTime(), union.getUnionBO().getOutSports(), union.getUnionBO().getRoundId(), union.getOwnerClubName(), union.getUnionBO().getState(), union.getUnionBO().getSort(),
                            UnionDefine.UNION_QUIT_TABLENUM.valueOf(union.getUnionBO().getTableNum()).value(), union.getUnionBO().getShowLostConnect(), union.getUnionBO().getCaseStatus(), union.getUnionBO().getShowUplevelId(), union.getUnionBO().getShowClubSign(), union.getUnionBO().getUnionType(), union.getUnionBO().getZhongZhiShowStatus(), union.getUnionBO().getSkinTable(), union.getUnionBO().getSkinBackColor(), union.getUnionBO().getRankedOpenZhongZhi(), union.getUnionBO().getRankedOpenEntryZhongZhi());
                }
            }
            // 获取赛事名称
            return new UnionInfo(union.getUnionBO().getId(), union.getUnionBO().getName(), UnionDefine.UNION_POST_TYPE.UNION_GENERAL.value(), union.getUnionBO().getUnionSign(), union.getUnionBO().getEndRoundTime(), union.getUnionBO().getOutSports(), union.getUnionBO().getRoundId(), union.getOwnerClubName(), union.getUnionBO().getState(), union.getUnionBO().getSort(),
                    UnionDefine.UNION_QUIT_TABLENUM.valueOf(union.getUnionBO().getTableNum()).value(), union.getUnionBO().getShowLostConnect(), union.getUnionBO().getCaseStatus(), union.getUnionBO().getShowUplevelId(), union.getUnionBO().getShowClubSign(), union.getUnionBO().getUnionType(), union.getUnionBO().getZhongZhiShowStatus(), union.getUnionBO().getSkinTable(), union.getUnionBO().getSkinBackColor(), union.getUnionBO().getRankedOpenZhongZhi(), union.getUnionBO().getRankedOpenEntryZhongZhi());
        }
        // 没有加入赛事
        return null;
    }


    /**
     * 房间配置列表
     *
     * @param pageNum 第几页
     * @return
     */
    public Map<Long, ClubRoomConfigCalcActiveItem> getRoomConfigList(int pageNum) {
        return this.getMCreateGamesetMap()
                .values()
                .stream()
                .filter(k -> Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() == k.getStatus())
                .map(k -> {
                    if (Objects.isNull(k.getGameType())) {
                        return null;
                    }
                    return new ClubRoomConfigCalcActiveItem(k.getGameIndex(), k.getGameType().getId(), k.getbRoomConfigure().getBaseCreateRoom().getPlayerNum());
                }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toMap(k -> k.getConfigId(), k -> k, (k1, k2) -> k1));
    }

    public List<ClubRoomConfigItem> getClubRoomConfigItemList(long configId) {
        return this.getMCreateGamesetMap()
                .values()
                .stream()
                .filter(k -> Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() == k.getStatus())
                .map(k -> {
                    if (Objects.isNull(k.getbRoomConfigure())) {
                        return null;
                    }
                    if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                        return null;
                    }
                    BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
                    return new ClubRoomConfigItem(baseCreateRoom.getGameIndex(), k.getRoomKey(), k.getbRoomConfigure().getGameType().getId(), baseCreateRoom.getPlayerNum(), baseCreateRoom.getSetCount(), baseCreateRoom.getGameIndex() == configId ? 1 : 0, k.getbRoomConfigure().getTagId());
                }).filter(k -> Objects.nonNull(k)).sorted(Comparator.comparing(ClubRoomConfigItem::getTab).reversed()).collect(Collectors.toList());
    }


    /**
     * 检查并创建新房间
     */
    public void checkCreateNewSetRoom() {
        if (getClubListBO().getUnionId() > 0) {
            return;
        }
        List<Long> configIds = this.getMCreateGamesetMap().entrySet().stream().filter(k -> k.getValue().getStatus() == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value()).map(k -> k.getKey()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(configIds)) {
            // 没有亲友圈配置
            return;
        }
        // 需要创建房间的房间配置ID
        List<Long> notExistIdList;
        if (Config.isShare()) {
            notExistIdList = ShareRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(configIds, this.getClubListBO().getId(), RoomTypeEnum.CLUB);
        } else {
            notExistIdList = NormalRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(configIds, this.getClubListBO().getId(), RoomTypeEnum.CLUB);
        }
        if (CollectionUtils.isEmpty(notExistIdList)) {
            // 没有需要创建的房间配置
            return;
        }
        // 创建新房间
        notExistIdList.stream().forEach(k -> {
            ClubCreateGameSet clubCreateGameSet = this.getMCreateGamesetMap().get(k);
            if (Objects.nonNull(clubCreateGameSet)) {
                // 设置标识Id
                clubCreateGameSet.setTagId(getCurTabId(clubCreateGameSet.getGameIndex()));
                // 初始房间号
                clubCreateGameSet.setRoomKey("");
                // 增加房间数
                clubCreateGameSet.addRoomCount();
                // 创建时间
                clubCreateGameSet.setCreateTime(CommTime.nowSecond());
                // 创建一个空亲友圈房间
                clubCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneClubRoom(clubCreateGameSet.getbRoomConfigure(), 0));

            }
        });
    }

    /**
     * 判断是不是中至亲友圈
     *
     * @return
     */
    public boolean isZhongZhiClub() {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return false;
        }
        return union.getUnionBO().getUnionType() == UnionDefine.UNION_TYPE.ZhongZhi.value();
    }
}
