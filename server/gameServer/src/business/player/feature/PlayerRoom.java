package business.player.feature;

import business.global.GM.MaintainGameMgr;
import business.global.GM.MaintainServerMgr;
import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.config.DiscountMgr;
import business.global.config.GameListConfigMgr;
import business.global.room.ContinueRoomInfoMgr;
import business.global.room.NormalRoomMgr;
import business.global.billing.RoomBillingGuard;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomImpl;
import business.global.sharegm.ShareNodeServerMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.PlayerRoomInfo;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqPlayerPushProtoBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.ShareNode;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.GaoJiTypeEnum;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomContinueEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.DistributedRedisLock;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.AssertsUtil;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.config.refdata.ref.RefRoomCost;
import core.config.server.GameTypeMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.GameTypeUrl;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.ContinueRoomInfo;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.room.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
public class PlayerRoom extends Feature {

    public PlayerRoom(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }


    /**
     * 检查创建房间高级选项
     *
     * @param baseCreateRoom 房间配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    private SData_Result checkCreateRoomGaoJi(BaseCreateRoom baseCreateRoom) {
        if (baseCreateRoom.getGaoji().contains(GaoJiTypeEnum.LOCATION.ordinal()) || baseCreateRoom.getGaoji().contains(GaoJiTypeEnum.LOCATION_200.ordinal())) {
            // 检查是否有有效的定位数据
            if (Config.isShare()) {
                SharePlayerMgr.getInstance().getPlayer(this.getPlayer());
            }
            /* TODO 测试，临时关闭
            if (!this.getPlayer().checkLocationInfo()) {
                // 定位未开启
                return SData_Result.make(ErrorCode.POSITIONING_NOT_ON, "error POSITIONING_NOT_ON");
            }*/
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 回退房卡
     *
     * @param gameType
     * @param prizeType
     * @param several
     */
    public void giveBack(GameType gameType, PrizeType prizeType, long clubID, long roomID, int several) {
        if (!RoomBillingGuard.beginRefund(roomID, this.getPid())) {
            return;
        }
        if (PrizeType.ClubCard.equals(prizeType)) {
            // 圈卡回退
            this.clubCardGiveBack(gameType, clubID, roomID, several);
        } else {
            // 房卡回退
            this.roomCardGiveBack(gameType, several);
        }
    }

    /**
     * @param gameType
     * @param several
     */
    private void roomCardGiveBack(GameType gameType, int several) {
        if (several == 0) {
            // 不是大赢家
            this.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(this.getPlayer().getRoomInfo().getConsumeCard(), gameType);
        } else {
            // 大赢家分
            this.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(this.getPlayer().getRoomInfo().getConsumeCard(), gameType, several);
        }
    }

    /**
     * 圈卡回退
     *
     * @param gameType
     * @param clubID
     * @param roomID
     * @param several
     */
    private void clubCardGiveBack(GameType gameType, long clubID, long roomID, int several) {
        if (several == 0) {
            // 不是大赢家
            this.player.getFeature(PlayerClub.class).clubCardReturnCradRoom(this.getPlayer().getRoomInfo().getConsumeCard(), gameType, clubID, ItemFlow.ClubCardClubRoom);
        } else {
            // 大赢家分
            this.player.getFeature(PlayerClub.class).clubCardReturnCradRoom(this.getPlayer().getRoomInfo().getConsumeCard(), gameType, clubID, several, roomID, ItemFlow.ClubCardClubRoom);
        }
    }

    /**
     * 创建房间并消费房卡。
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createRoomAndConsumeCard(BaseRoomConfigure baseRoomConfigure) {
        return createRoomAndConsumeCard(baseRoomConfigure, "");
    }

    /**
     * 继续房间
     *
     * @param baseRoomConfigure
     * @param continueRoomInfo
     * @param continueRoom
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result continueRoom(BaseRoomConfigure baseRoomConfigure, ContinueRoomInfo continueRoomInfo, CBase_ContinueRoom continueRoom) {
        if (Config.isShare()) {
            Integer gameTypeId = baseRoomConfigure.getGameType().getId();
            GameType enterGameType = GameTypeMgr.getInstance().gameType(gameTypeId);
            //是否维护中
            Boolean isContinueGame = MaintainGameMgr.getInstance().checkContinueGame(enterGameType.getId());
            //维护中不能继续游戏
            if (!isContinueGame) {
                return SData_Result.make(ErrorCode.Game_Maintain, MaintainGameMgr.getInstance().getMaintainGameContent(enterGameType.getId()));
            } else {
                //游戏当前使用的节点
                GameTypeUrl gameTypeUrl = GameListConfigMgr.getInstance().getByGameType(enterGameType.getId());
                //检查要进入的节点是不是当前节点
                if (ShareNodeServerMgr.getInstance().checkCurrentNode(gameTypeUrl.getGameServerIP(), gameTypeUrl.getGameServerPort())) {
                    return doContinueRoom(baseRoomConfigure, continueRoomInfo, continueRoom);
                } else {
                    if (GameListConfigMgr.getInstance().checkIsLiveByGameType(enterGameType.getId())) {
                        long requestId = Integer.toUnsignedLong(Objects.hash(player.getPid(),continueRoom.roomID,continueRoom.continueType));
                        CommLogD.info("创建房间开始[{}],请求标识[{}]", enterGameType.getName(), requestId);
                        ShareNode shareNode = new ShareNode("", gameTypeUrl.getWebSocketUrl(), gameTypeUrl.getGameServerIP(), gameTypeUrl.getGameServerPort());
                        MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo(player.getPid(), enterGameType.getName(), enterGameType.getId(), new Gson().toJson(continueRoom), "room.continueroom", shareNode);
                        mqAbsRequestBo.setRequestId(requestId);
                        mqAbsRequestBo.setShareNodeFrom(ShareNodeServerMgr.getInstance().getThisNode());
                        //推送到MQ
                        var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.BASE_CONTINUE_ROOM + enterGameType.getId(),"continue-room:"+player.getPid()+":"+continueRoom.roomID+":"+requestId,mqAbsRequestBo);
                        if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED)return SData_Result.make(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"ROOM_REQUEST_NOT_CONFIRMED:"+confirmation);
                        return SData_Result.make(ErrorCode.ROOM_GAME_SERVER_CHANGE, "ROOM_GAME_SERVER_CHANGE");
                    } else {
                        //节点没有启动
                        return SData_Result.make(ErrorCode.Server_Maintain, "SERVER_STARTING");
                    }
                }
            }
        } else {
            return doContinueRoom(baseRoomConfigure, continueRoomInfo, continueRoom);
        }

    }

    /**
     * 继续房间
     *
     * @param baseRoomConfigure
     * @param continueRoomInfo
     * @param continueRoom
     * @return
     */
    private SData_Result doContinueRoom(BaseRoomConfigure baseRoomConfigure, ContinueRoomInfo continueRoomInfo, CBase_ContinueRoom continueRoom) {
        SData_Result result = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(baseRoomConfigure);
        if (ErrorCode.Success.equals(result.getCode())) {
            SRoom_CreateRoom data = (SRoom_CreateRoom) result.getData();
            //创建成功了 把这条信息改为true存回管理器中
            continueRoomInfo.setUseFlag(true);
            ContinueRoomInfoMgr.getInstance().putContinueRoomInfo(continueRoomInfo);
            //结束时间五分钟内 向上一把的那些人员发送继续房间的消息  该人员必须不在房间里
            if (CommTime.nowSecond() - continueRoomInfo.getRoomEndTime() < RoomContinueEnum.RoomContinueTimeEnum.FiveMinute.value()) {
                for (Long pid : continueRoomInfo.getPlayerIDList()) {
                    if (pid == player.getPid()) continue;
                    if (Config.isShare()) {
                        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayerByOnline(pid);
                        if (sharePlayer != null && sharePlayer.getRoomInfo().getRoomId() == 0L) {
                            MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(pid, SRoom_ContinueRoomInfo.make(data.getRoomID(), data.getRoomKey(), player.getName(), continueRoom.continueType, continueRoom.roomID, data.getGameTypeUrl()), SRoom_ContinueRoomInfo.class.getName()));
                        }
                    } else {
                        Player playerLast = PlayerMgr.getInstance().getOnlinePlayerByPid(pid);
                        if (playerLast != null && playerLast.getRoomInfo().getRoomId() == 0L) {
                            playerLast.pushProto(SRoom_ContinueRoomInfo.make(data.getRoomID(), data.getRoomKey(), player.getName(), continueRoom.continueType, continueRoom.roomID));
                        }
                    }
                }
            }
            //创建成功的时候
            return SData_Result.make(ErrorCode.Success, SRoom_ContinueRoom.make(data.getRoomID(), data.getRoomKey(), data.getCreateType(), data.getGameType(), continueRoom.continueType, data.getGameTypeUrl()));
        } else {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
    }


    /**
     * 创建房间并消费房卡。
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createRoomAndConsumeCard(BaseRoomConfigure baseRoomConfigure, String roomKey) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("createRoomAndConsumeCard" + this.getPid(), uuid)) {
                this.initBaseRoomConfigure(baseRoomConfigure);
                // 检查玩家是否存在其他房间
                SData_Result result = this.checkExistOtherRoom();
                // 检查玩家是否存在其他房间
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 清空亲友圈ID
                baseRoomConfigure.getBaseCreateRoom().setClubId(0L);
                // 重置消费卡
                this.getPlayer().getRoomInfo().clear();
                // 检查高级选项
                result = this.checkCreateRoomGaoJi(baseRoomConfigure.getBaseCreateRoom());
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 检查房卡配置和玩家房卡
                result = this.checkRefRoomCost(baseRoomConfigure);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 创建游戏房间
                result = NormalRoomMgr.getInstance().createNormalRoom(baseRoomConfigure, this.getPid(), roomKey);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    // 创建房间失败返回房卡。
                    this.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(this.getPlayer().getRoomInfo().getConsumeCard(), baseRoomConfigure.getGameType());
                    return result;
                } else {
                    this.setRoomID(((SRoom_CreateRoom) result.getData()).getRoomID());
                    RoomBillingGuard.recordConsume(this.getRoomID(), this.getPid(),
                            this.getPlayer().getRoomInfo().getConsumeCard());
                }
                return result;
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("createRoomAndConsumeCard" + this.getPid(), uuid);
        }
    }

    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private SData_Result checkRefRoomCostShare(ShareRoom shareRoom) {
        if (Config.DE_DEBUG()) {
            return SData_Result.make(ErrorCode.Success, 0);
        }
        SData_Result result = RefRoomCost.GetCostShare(shareRoom);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        // 获取消耗
        int roomCard = (int) result.getCustom();
        // 检查房卡
        if (!this.getPlayer().getFeature(PlayerWallet.class).check(roomCard)) {
            // 房卡消耗不够。
            return SData_Result.make(ErrorCode.NotEnough_RoomCard,
                    "NotEnough_RoomCard ConsumeCard:{%d},PlayerCard:{%d}", roomCard, this.getPlayer().getRoomCard());
        }
        return SData_Result.make(ErrorCode.Success, roomCard);
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private SData_Result checkRefRoomCost(BaseRoomConfigure baseRoomConfigure) {
        if (Config.DE_DEBUG()) {
            return SData_Result.make(ErrorCode.Success, 0);
        }
        SData_Result result = RefRoomCost.GetCost(baseRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        // 获取消耗
        int roomCard = DiscountMgr.getInstance().calculateRoomCardCost(getPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), 0L, 0L, baseRoomConfigure.getGameType().getId(), (int) result.getCustom());
        // 检查并消耗房卡
        if (roomCard > 0 && !this.getPlayer().getFeature(PlayerWallet.class).checkAndConsumeRoom(roomCard, baseRoomConfigure.getGameType())) {
            // 房卡消耗不够。
            return SData_Result.make(ErrorCode.NotEnough_RoomCard,
                    "NotEnough_RoomCard ConsumeCard:{%d},PlayerCard:{%d}", roomCard, this.getPlayer().getRoomCard());
        }

        // 设置消费房卡
        this.getPlayer().getRoomInfo().setConsumeCard(roomCard);
        //共享数据
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this.getPlayer(), "roomInfo");
        }
        return SData_Result.make(ErrorCode.Success, roomCard);
    }

    /**
     * 初始配置
     *
     * @param baseRoomConfigure 公共
     */
    private void initBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        baseRoomConfigure.getBaseCreateRoom().setRoomName(null);
        baseRoomConfigure.getBaseCreateRoom().setIsClubMemberCreateRoom(null);
        baseRoomConfigure.getBaseCreateRoom().setClubWinnerPayConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setClubCostType(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomName(null);
        baseRoomConfigure.getBaseCreateRoom().setSportsDouble(null);
        baseRoomConfigure.getBaseCreateRoom().setAutoDismiss(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsThreshold(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsType(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsEveryoneConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsBigWinnerConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setBigWinnerConsumeList(null);
        baseRoomConfigure.getBaseCreateRoom().setPrizePool(null);
        baseRoomConfigure.getBaseCreateRoom().setPassword(null);
        baseRoomConfigure.setClubRoomCfg(null);
        baseRoomConfigure.setUnionRoomConfig(null);
        baseRoomConfigure.setArenaRoomCfg(null);
        baseRoomConfigure.setRobotRoomCfg(null);
    }


    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndEnter(int posID, String roomKey, long clubId, String password) {
        return findAndEnter(posID, roomKey, clubId, password, true);
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndSeat(int posID, String roomKey, long clubId, String password) {
        return findAndSeat(posID, roomKey, clubId, password, true);
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndEnter(int posID, String roomKey, long clubId, String password, boolean existQuickJoin) {
        long startTime = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("createRoomAndConsumeCard" + this.getPid(), uuid)) {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
                if (Objects.nonNull(club)) {
                    Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
                    if (Objects.nonNull(union) && union.getUnionBO().getChangeAllyLeader() == UnionDefine.UNION_CASE_STATUS.OPEN.ordinal()) {
                        return SData_Result.make(ErrorCode.ChangeAllyLeader, "当前赛事正在更换赛事主裁判，暂停游戏，请稍后再试！预计半小时");
                    }
                }
                SData_Result result = null;
                if (Config.isShare()) {
                    //检查房间是否存在和在其他房间
                    result = this.checkExistOtherRoomShare();
                } else {
                    result = this.checkExistOtherRoom();
                }
                // 检查玩家是否存在其他房间
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                RoomImpl roomImpl = null;
                if (Config.isShare()) {
                    ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
                    // 获取房间信息
                    if (null == shareRoom) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                    if (!shareRoom.isNoneRoom()) {
                        // 不是空配置房间需要去找实体房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                        // 获取房间信息
                        if (null == roomImpl) {
                            return SData_Result.make(ErrorCode.NotFind_Room, "NoneRoomByKey NotFind_Room ");
                        }
                    }
                    String tempPassword = shareRoom.getPassword();
                    if (StringUtils.isNotEmpty(tempPassword)) {
                        if (StringUtils.isEmpty(password) || !tempPassword.equals(EncryptUtils.encryptDES(password))) {
                            return SData_Result.make(ErrorCode.ROOM_PASSWORD_ERROR, "{\"tagId\": %d,\"clubId\": %d}", shareRoom.getTagId(), clubId);
                        }
                    }
                    // 检查是否亲友圈或者赛事房间
                    if (shareRoom.getClubId() != 0 || shareRoom.getUnionId() != 0) {
                        if (shareRoom.isNoneRoom()) {
                            // 必须要空配置房间
                            return createAndEnterShare(shareRoom, posID, roomKey, clubId, existQuickJoin);
                        }
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        // 实体房间找不到
                        return SData_Result.make(ErrorCode.NotFind_Room, "null == null NoneRoomByKey NotFind_Room ");
                    }
                } else {
                    if (Config.DE_DEBUG_ROOM()) {
                        // 服务端调试开关, 随机进入房间 0关1开
                        roomImpl = NormalRoomMgr.getInstance().getDeBugRoomKey();
                    } else {
                        // 通过房间号进入房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                    String tempPassword = roomImpl.getBaseRoomConfigure().getBaseCreateRoom().getPassword();
                    if (StringUtils.isNotEmpty(tempPassword)) {
                        if (StringUtils.isEmpty(password) || !tempPassword.equals(EncryptUtils.encryptDES(password))) {
                            return SData_Result.make(ErrorCode.ROOM_PASSWORD_ERROR, "{\"tagId\": %d,\"clubId\": %d}", roomImpl.getBaseRoomConfigure().getTagId(), clubId);
                        }
                    }
                    // 检查是否空的亲友圈房间
                    if (roomImpl.isNoneRoom()) {
                        return createAndEnter(roomImpl, posID, roomKey, clubId);
                    }
                }
                // 测试快速进入房间专用。
                AbsBaseRoom room = (AbsBaseRoom) roomImpl;
                if (room.getSpecialRoomId() > 0L) {
                    // (亲友圈、赛事)房间加入
                    return findAndEnter(room, posID, roomKey, clubId);
                }
                // 重复进入同一个房间
                if (this.getRoomID() == room.getRoomID()) {
                    // 进入房间信息
                    return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
                }
                if (Config.isShare()) {
                    SharePlayerMgr.getInstance().getPlayer(this.getPlayer());
                }
                // 重置消费卡
                this.getPlayer().getRoomInfo().clear();
                // 检查高级选项
                result = room.getRoomPosMgr().checkEnterRoomGaoJi(getPlayer().getLocationInfo(), getPlayer().getIp());
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 检查房间支付类型并消费卡
                if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != room.getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
                    result = this.checkRefRoomCost(room.getBaseRoomConfigure());
                    if (!ErrorCode.Success.equals(result.getCode())) {
                        return result;
                    }
                }
                // 查询并进入房间。
                result = room.enterRoom(this.getPid(), posID, false, null);
                if (ErrorCode.Success.equals(result.getCode())) {
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
                } else {
                    // 根据房间支付类型返回房卡。
                    this.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(this.getPlayer().getRoomInfo().getConsumeCard(), room.getBaseRoomConfigure().getGameType());
                    CommLogD.error("findAndEnter Code:{},Msg:{}", result.getCode(), result.getMsg());
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return result;
                }
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("createRoomAndConsumeCard" + this.getPid(), uuid);
            CommLogD.info("玩家[{}]进入房间耗时:[{}]", this.getPid(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndSeat(int posID, String roomKey, long clubId, String password, boolean existQuickJoin) {
        long startTime = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("createRoomAndConsumeCard" + this.getPid(), uuid)) {
//                Club club= ClubMgr.getInstance().getClubListMgr().findClub(clubId);
//                if(Objects.nonNull(club)){
//                    Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
//                    if(Objects.nonNull(union)&&union.getUnionBO().getChangeAllyLeader()== UnionDefine.UNION_CASE_STATUS.OPEN.ordinal()){
//                        return SData_Result.make(ErrorCode.ChangeAllyLeader, "当前赛事正在更换赛事主裁判，暂停游戏，请稍后再试！预计半小时");
//                    }
//                }
                SData_Result result = null;
                RoomImpl roomImpl = null;
                if (Config.isShare()) {
                    ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
                    // 获取房间信息
                    if (null == shareRoom) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                    if (!shareRoom.isNoneRoom()) {
                        // 不是空配置房间需要去找实体房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                        // 获取房间信息
                        if (null == roomImpl) {
                            return SData_Result.make(ErrorCode.NotFind_Room, "NoneRoomByKey NotFind_Room ");
                        }
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        // 实体房间找不到
                        return SData_Result.make(ErrorCode.NotFind_Room, "null == null NoneRoomByKey NotFind_Room ");
                    }
                } else {
                    if (Config.DE_DEBUG_ROOM()) {
                        // 服务端调试开关, 随机进入房间 0关1开
                        roomImpl = NormalRoomMgr.getInstance().getDeBugRoomKey();
                    } else {
                        // 通过房间号进入房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                }
                // 测试快速进入房间专用。
                AbsBaseRoom room = (AbsBaseRoom) roomImpl;
                clubId = 0;
                AbsRoomPos pos = room.getRoomPosMgr().getWatchPosByPid(this.getPid());
                if (null == pos) {
                    return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_WatchPos");
                }
                if (null != pos.getClubMemberBO()) {
                    clubId = pos.getClubMemberBO().getClubID();
                }
                if (room.getSpecialRoomId() > 0L) {
                    // (亲友圈、赛事)房间加入
                    return findAndSeat(room, posID, roomKey, clubId);
                }
                if (Config.isShare()) {
                    SharePlayerMgr.getInstance().getPlayer(this.getPlayer());
                }
                // 重置消费卡
//                this.getPlayer().getRoomInfo().clear();
                // 检查高级选项
                result = room.getRoomPosMgr().checkEnterRoomGaoJi(getPlayer().getLocationInfo(), getPlayer().getIp());
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 检查房间支付类型并消费卡
                if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != room.getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
                    result = this.checkRefRoomCost(room.getBaseRoomConfigure());
                    if (!ErrorCode.Success.equals(result.getCode())) {
                        return result;
                    }
                }
                // 查询并进入房间。
                result = room.seatRoom(this.getPid(), posID, false, null);
                if (ErrorCode.Success.equals(result.getCode())) {
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
                } else {
                    // 根据房间支付类型返回房卡。
                    this.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(this.getPlayer().getRoomInfo().getConsumeCard(), room.getBaseRoomConfigure().getGameType());
                    CommLogD.error("findAndEnter Code:{},Msg:{}", result.getCode(), result.getMsg());
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return result;
                }
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("createRoomAndConsumeCard" + this.getPid(), uuid);
            CommLogD.info("玩家[{}]坐下耗时:[{}]", this.getPid(), System.currentTimeMillis() - startTime);
        }
    }

    /***
     * 共享创建并加入房间
     * (亲友圈、赛事)
     * @param shareRoom 房间接口
     * @param posID 玩家位置
     * @param roomKey 房间key
     * @param clubId 亲友圈id
     * @return
     */
    private SData_Result createAndEnterShare(ShareRoom shareRoom, int posID, String roomKey, long clubId, boolean existQuickJoin) {
        if (RoomTypeEnum.CLUB.equals(shareRoom.getRoomTypeEnum())) {
            return this.getPlayer().getFeature(PlayerClubRoom.class).createAndEnterShare(shareRoom, posID, roomKey, clubId, existQuickJoin);
        } else {
            return this.getPlayer().getFeature(PlayerUnionRoom.class).createAndEnterShare(shareRoom, posID, roomKey, clubId, existQuickJoin);
        }
    }

    /***
     * 创建并加入房间
     * (亲友圈、赛事)
     * @param roomImpl 房间接口
     * @param posID 玩家位置
     * @param roomKey 房间key
     * @param clubId 亲友圈id
     * @return
     */
    private SData_Result createAndEnter(RoomImpl roomImpl, int posID, String roomKey, long clubId) {
        if (RoomTypeEnum.CLUB.equals(roomImpl.getRoomTypeEnum())) {
            return this.getPlayer().getFeature(PlayerClubRoom.class).createAndEnter(roomImpl, posID, roomKey, clubId);
        } else {
            return this.getPlayer().getFeature(PlayerUnionRoom.class).createAndEnter(roomImpl, posID, roomKey, clubId);
        }
    }

    /**
     * 进入房间
     * (亲友圈、赛事)
     *
     * @param room    房间信息
     * @param posID   玩家位置
     * @param roomKey 房间key
     * @param clubId  亲友圈id
     * @return
     */
    private SData_Result findAndEnter(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        if (RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum())) {
            return this.getPlayer().getFeature(PlayerClubRoom.class).findAndEnter(room, posID, roomKey, clubId);
        } else {
            return this.getPlayer().getFeature(PlayerUnionRoom.class).findAndEnter(room, posID, roomKey, clubId);
        }
    }

    private SData_Result findAndSeat(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        if (RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum())) {
            return this.getPlayer().getFeature(PlayerClubRoom.class).findAndSeat(room, posID, roomKey, clubId);
        } else {
            return this.getPlayer().getFeature(PlayerUnionRoom.class).findAndSeat(room, posID, roomKey, clubId);
        }
    }


    /**
     * 检查玩家是否存在其他房间
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkExistOtherRoom() {
        // 检查是否维护中
        SData_Result result = MaintainServerMgr.getInstance().checkUnderMaintenance(this.getPlayer());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 已经在其他房间
        if (0L < this.getRoomID()) {
            // 获取指定的房间信息是否存在
            AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomID());
            if (null != room) {
                // 检查是否信息
                return SData_Result.make(ErrorCode.Exist_OtherRoom, "已经在房间里");
            } else {
                // 强行踢出房间或玩家身上的房间信息。
                this.getPlayer().onGMExitRoom();
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 共享检查玩家是否存在其他房间
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkExistOtherRoomShare() {
        // 检查是否维护中
        SData_Result result = MaintainServerMgr.getInstance().checkUnderMaintenance(this.getPlayer());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Player player = SharePlayerMgr.getInstance().getPlayer(this.getPlayer());
        // 已经在其他房间
        if (0L < player.getRoomInfo().getRoomId()) {
            // 获取指定的房间信息是否存在
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(player.getRoomInfo().getRoomId());
            if (null != shareRoom) {
                // 检查是否信息
                return SData_Result.make(ErrorCode.Exist_OtherRoom, "已经在房间里");
            } else {
                // 强行踢出房间或玩家身上的房间信息。
                player.onGMExitRoom();
            }
        }
        //更新共享玩家
//        SharePlayerMgr.getInstance().updateAllSharePlayer(player);
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取游戏类型
     *
     * @param roomKey 房间key
     * @return 返回游戏类型
     * @throws Exception
     */
    public int getGameTypeByRoomKey(String roomKey) throws Exception {
        if (Config.isShare()) {
            AssertsUtil.notNull(roomKey, ErrorCode.Room_Key_ISNULL, "房间key不允许为空");
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
            AssertsUtil.notNull(shareRoom, ErrorCode.Room_NOT_Find, "房间未找到");
            AssertsUtil.notNull(shareRoom.getBaseRoomConfigure(), ErrorCode.Room_BaseConfigure_ISNULL, "配置为空");
            AssertsUtil.notNull(shareRoom.getBaseRoomConfigure().getGameType(), ErrorCode.Room_GameType_ISNULL, "游戏类型为空");
            return shareRoom.getBaseRoomConfigure().getGameType().getId();
        } else {
            AssertsUtil.notNull(roomKey, ErrorCode.Room_Key_ISNULL, "房间key不允许为空");
            RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
            AssertsUtil.notNull(roomImpl, ErrorCode.Room_NOT_Find, "房间未找到");
            AssertsUtil.notNull(roomImpl.getBaseRoomConfigure(), ErrorCode.Room_BaseConfigure_ISNULL, "配置为空");
            AssertsUtil.notNull(roomImpl.getBaseRoomConfigure().getGameType(), ErrorCode.Room_GameType_ISNULL, "游戏类型为空");
            return roomImpl.getBaseRoomConfigure().getGameType().getId();
        }
    }


    /**
     * 继续查询并加入房间
     *
     * @return
     */
    public SData_Result continueFindAndEnter() {
        // 获取玩家身上房间配置信息
        PlayerRoomInfo roomInfo = getPlayer().getRoomInfo();
        long specialRoomId = 0L;
        if (RoomTypeEnum.UNION.equals(roomInfo.getRoomTypeEnum())) {
            specialRoomId = roomInfo.getUnionId();
        } else if (RoomTypeEnum.CLUB.equals(roomInfo.getRoomTypeEnum())) {
            specialRoomId = roomInfo.getClubId();
        } else {
            return SData_Result.make(ErrorCode.NotFind_Room, "continueFindAndEnter RoomTypeEnum:{%s}", roomInfo.getRoomTypeEnum());
        }
        String roomKey = null;
        if (Config.isShare()) {
            roomKey = ShareRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(getPlayer().getRoomInfo().getConfigId(), specialRoomId, roomInfo.getRoomTypeEnum());
        } else {
            roomKey = NormalRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(getPlayer().getRoomInfo().getConfigId(), specialRoomId, roomInfo.getRoomTypeEnum());
        }
        if (StringUtils.isEmpty(roomKey)) {
            return SData_Result.make(ErrorCode.NotFind_Room, "continueFindAndEnter");
        }
        if (Config.isShare()) {
            ShareRoom enterShareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
            GameType enterGameType = GameTypeMgr.getInstance().gameType(enterShareRoom.getBaseRoomConfigure().getGameType().getId());
            //是否维护中
            Boolean isContinueGame = MaintainGameMgr.getInstance().checkContinueGame(enterGameType.getId());
            //维护中不能继续游戏
            CommLogD.info("PlayerRoom enterroom");
            if (!isContinueGame) {
                return SData_Result.make(ErrorCode.Game_Maintain, MaintainGameMgr.getInstance().getMaintainGameContent(enterGameType.getId()));
            } else {
                ShareNode shareNodeEnter = GameListConfigMgr.getInstance().getShareNodeByRoom(enterShareRoom);
                //检查要进入的节点是不是当前节点
                if (ShareNodeServerMgr.getInstance().checkCurrentNode(shareNodeEnter.getIp(), shareNodeEnter.getPort())) {
                    return findAndEnter(-1, roomKey, roomInfo.getClubId(), roomInfo.getPassword());
                } else {
                    //不是当前继续游戏走异步接口
                    CBase_EnterRoom req = new CBase_EnterRoom();
                    req.setPosID(-1);
                    req.setClubId(roomInfo.getClubId());
                    req.setRoomKey(roomKey);
                    req.setPassword(roomInfo.getPassword());
                    if (GameListConfigMgr.getInstance().checkIsLiveByGameType(enterGameType.getId())) {
                        MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo(player.getPid(), enterGameType.getName(), enterGameType.getId(), new Gson().toJson(req), "room.continueenterroom", shareNodeEnter);
                        long requestId=Integer.toUnsignedLong(Objects.hash(player.getPid(),roomInfo.getRoomId(),roomKey));mqAbsRequestBo.setRequestId(requestId);
                        mqAbsRequestBo.setShareNodeFrom(ShareNodeServerMgr.getInstance().getThisNode());
                        //推送到MQ
                        var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.BASE_ENTER_ROOM + enterGameType.getId(),"continue-enter:"+player.getPid()+":"+roomInfo.getRoomId()+":"+requestId,mqAbsRequestBo);
                        if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED)return SData_Result.make(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"ROOM_REQUEST_NOT_CONFIRMED:"+confirmation);
                        return SData_Result.make(ErrorCode.ROOM_GAME_SERVER_CHANGE, "ROOM_GAME_SERVER_CHANGE");
                    } else {
                        //节点没有启动
                        return SData_Result.make(ErrorCode.Server_Maintain, "SERVER_STARTING");
                    }
                }
            }
        } else {
            return findAndEnter(-1, roomKey, roomInfo.getClubId(), roomInfo.getPassword());
        }
    }

    /**
     * 获取继续房间的相关信息
     *
     * @param
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ContinueRoomInfo getContinueRoomInfo(Long roomID) {
        return ContinueRoomInfoMgr.getInstance().getContinueRoomInfo(roomID);
    }

    /**
     * 退出房间从更换房间拆分出来,分服务的情况需要先退出一个房间在进入另外一个房间可能不在同一个服务里面
     *
     * @return
     */
    public SData_Result onExitRoom() {
        // 已经退出房间了
        if (this.getRoomID() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "onChange RoomId:{%d}", this.getRoomID());
        }
        // 获取指定的房间信息是否存在
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomID());
        if (Objects.isNull(room)) {
            // 强行踢出房间或玩家身上的房间信息。
            this.getPlayer().onGMExitRoom();
            return SData_Result.make(ErrorCode.NotAllow, "onChange room == null RoomId:{%d}", this.getRoomID());
        }
        SData_Result result = room.exitRoom(getPid());
        // 主动离开房间
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        return result;
    }


    public SData_Result onChange(int posID, String roomKey, long clubId, String password) {
        // 已经在其他房间
        if (this.getRoomID() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "onChange RoomId:{%d}", this.getRoomID());
        }
        // 获取指定的房间信息是否存在
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomID());
        if (Objects.isNull(room)) {
            // 强行踢出房间或玩家身上的房间信息。
            this.getPlayer().onGMExitRoom();
            return SData_Result.make(ErrorCode.NotAllow, "onChange room == null RoomId:{%d}", this.getRoomID());
        }
        // 通过房间号进入房间
        RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
        if (Objects.isNull(roomImpl)) {
            return SData_Result.make(ErrorCode.NotAllow, "onChange null == roomImpl RoomKey:{%s}", roomKey);
        }
        String tempPassword = roomImpl.getBaseRoomConfigure().getBaseCreateRoom().getPassword();
        if (StringUtils.isNotEmpty(tempPassword)) {
            if (StringUtils.isEmpty(password) || !tempPassword.equals(EncryptUtils.encryptDES(password))) {
                return SData_Result.make(ErrorCode.ROOM_PASSWORD_ERROR, "{\"tagId\": %d,\"clubId\": %d}", roomImpl.getBaseRoomConfigure().getTagId(), clubId);
            }
        }

        SData_Result result = room.exitRoom(getPid());
        // 主动离开房间
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        return this.findAndEnter(posID, roomKey, clubId, password);
    }


    public SData_Result onRoomConfig() {
        // 已经在其他房间
        if (this.getRoomID() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig RoomId:{%d}", this.getRoomID());
        }
        // 获取指定的房间信息是否存在
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomID());
        if (Objects.isNull(room)) {
            // 强行踢出房间或玩家身上的房间信息。
            this.getPlayer().onGMExitRoom();
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig null room RoomId:{%d}", this.getRoomID());
        }
        RoomInfoItem roomInfoItem = room.getRoomTyepImpl().getRoomInfoItem();
        if (Objects.isNull(roomInfoItem)) {
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig null roomInfoItem RoomId:{%d}", this.getRoomID());
        }
        return SData_Result.make(ErrorCode.Success, roomInfoItem);
    }

    /**
     * 共享获取数据
     *
     * @return
     */
    public SData_Result onRoomConfigShare() {
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(this.player.getPid());
        // 已经在其他房间
        if (sharePlayer.getRoomInfo() != null && sharePlayer.getRoomInfo().getRoomId() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig RoomId:{%d}", this.getRoomID());
        }
        // 获取指定的房间信息是否存在
        ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(this.getRoomID());
        if (Objects.isNull(shareRoom)) {
            // 强行踢出房间或玩家身上的房间信息。
            this.getPlayer().onGMExitRoom();
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig null room RoomId:{%d}", this.getRoomID());
        }

        RoomInfoItem roomInfoItem = ShareRoomMgr.getInstance().getRoomInfoItem(shareRoom);
        if (Objects.isNull(roomInfoItem)) {
            return SData_Result.make(ErrorCode.NotAllow, "onRoomConfig null roomInfoItem RoomId:{%d}", this.getRoomID());
        }
        return SData_Result.make(ErrorCode.Success, roomInfoItem);
    }


    public SData_Result leavePos(String roomKey) {
        long startTime = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("createRoomAndConsumeCard" + this.getPid(), uuid)) {
                SData_Result result = null;
                RoomImpl roomImpl = null;
                if (Config.isShare()) {
                    ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
                    // 获取房间信息
                    if (null == shareRoom) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                    if (!shareRoom.isNoneRoom()) {
                        // 不是空配置房间需要去找实体房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                        // 获取房间信息
                        if (null == roomImpl) {
                            return SData_Result.make(ErrorCode.NotFind_Room, "NoneRoomByKey NotFind_Room ");
                        }
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        // 实体房间找不到
                        return SData_Result.make(ErrorCode.NotFind_Room, "null == null NoneRoomByKey NotFind_Room ");
                    }
                } else {
                    if (Config.DE_DEBUG_ROOM()) {
                        // 服务端调试开关, 随机进入房间 0关1开
                        roomImpl = NormalRoomMgr.getInstance().getDeBugRoomKey();
                    } else {
                        // 通过房间号进入房间
                        roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
                    }
                    // 获取房间信息
                    if (null == roomImpl) {
                        return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
                    }
                }
                // 测试快速进入房间专用。
                AbsBaseRoom room = (AbsBaseRoom) roomImpl;
                // 查询并进入房间。
                result = room.leavePos(this.getPid());
                if (ErrorCode.Success.equals(result.getCode())) {
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
                } else {
                    CommLogD.error("leavePos Code:{},Msg:{}", result.getCode(), result.getMsg());
                    if (Config.isShare()) {
                        ShareRoomMgr.getInstance().addShareRoom(room);
                    }
                    return result;
                }
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("createRoomAndConsumeCard" + this.getPid(), uuid);
            CommLogD.info("玩家[{}]离开坐位耗时:[{}]", this.getPid(), System.currentTimeMillis() - startTime);
        }
    }
}
