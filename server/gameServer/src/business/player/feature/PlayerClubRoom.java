package business.player.feature;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.config.DiscountMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.room.key.RoomKeyMgr;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.rocketmq.bo.MqDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import cenum.RoomTypeEnum;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.config.refdata.ref.RefRoomCost;
import core.db.entity.clarkGame.ClubMemberBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionBanRoomConfigBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.club.ClubCreateGameSet;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.club.Club_define.Club_CreateGameSetStatus;
import jsproto.c2s.cclass.club.Club_define.Club_DISSOLVEROOM_STATUS;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.club.SClub_CreateGameSetChange;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
public class PlayerClubRoom extends Feature {

    public PlayerClubRoom(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }

    /**
     * 创建空亲友圈房间 1、检查亲友圈，操作者身份 2、检查房卡配置 3、
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public void createNoneClubRoom(WebSocketRequest request, BaseRoomConfigure baseRoomConfigure) {
        // 初始亲友圈配置
        this.initBaseRoomConfigure(baseRoomConfigure);
        // 是否亲友圈管理员
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(baseRoomConfigure.getBaseCreateRoom().getClubId());
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "createNoneClubRoom null == club ClubID:{%d}",
                    baseRoomConfigure.getBaseCreateRoom().getClubId());
            return;
        }
        // 查询亲友圈成员
        ClubMember myClubMember = ClubMgr.getInstance().getClubMemberMgr().find(getPid(), baseRoomConfigure.getBaseCreateRoom().getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (null == myClubMember) {
            request.error(ErrorCode.NotAllow, "createNoneClubRoom not find myClubMember");
            return;
        }
        // 亲友圈管理员
        if (!myClubMember.isMinister()) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "createNoneClubRoom you have not minister");
            return;
        }

        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkExistUnion(club);
        if (!ErrorCode.NotAllow.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }


        // 检查房卡配置和玩家房卡
        result = this.checkRefNoneClub(baseRoomConfigure);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        // 创建、修改验证房间配置。
        ClubCreateGameSet gameSet = null;
        if (Config.isShare()) {
            gameSet = getClubCreateGameSetShare(request, club, baseRoomConfigure);
        } else {
            gameSet = getClubCreateGameSet(request, club, baseRoomConfigure);
        }
        if (null == gameSet) {
            // 验证配置存在错误。
            return;
        }
        // 是否创建房间
        boolean isCreate = gameSet.getGameIndex() <= 0L;
        if (isCreate && !club.checkCanCreateRoom()) {
            // 如果是创造并且超过限制
            request.error(ErrorCode.CLUB_MAXCRATESET, "CLUB_MAXCRATESET");
            return;
        }
        int roomCard = (int) result.getCustom();
        // 创建游戏房间
        result = NormalRoomMgr.getInstance().createNoneClubRoom(baseRoomConfigure, null, this.getPid(), roomCard, club.getClubListBO().getName());
        gameSet.setRoomConfigure((BaseRoomConfigure) result.getData());
        gameSet.setStatus(Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value());
        gameSet.setCreateTime(gameSet.getCreateTime() <= 0L ? CommTime.nowSecond() : gameSet.getCreateTime());
        gameSet.addRoomCount();
        gameSet.setGameIndex(gameSet.getGameIndex() <= 0L ? CommTime.nowMS() : gameSet.getGameIndex());
        gameSet.setTagId(club.getCurTabId(gameSet.getGameIndex()));
        club.getMCreateGamesetMap().put(gameSet.getGameIndex(), gameSet);
        request.response();
        request = null;
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().updateBaseRoomConfigure(gameSet.getbRoomConfigure());
        }
        // 添加游戏设置
        club.setCreateGameSet(this.getPid(), gameSet, isCreate);
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().addClub(club);
        }
    }

    /**
     * 初始配置
     *
     * @param baseRoomConfigure 公共
     */
    private void initBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        baseRoomConfigure.getBaseCreateRoom().setIsClubMemberCreateRoom(Boolean.FALSE);
        baseRoomConfigure.getBaseCreateRoom().setClubWinnerPayConsume(0);
        baseRoomConfigure.getBaseCreateRoom().setClubCostType(0);
        baseRoomConfigure.getBaseCreateRoom().setRoomName(null);
        baseRoomConfigure.getBaseCreateRoom().setSportsDouble(null);
        baseRoomConfigure.getBaseCreateRoom().setAutoDismiss(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsThreshold(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsType(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsEveryoneConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsBigWinnerConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setBigWinnerConsumeList(null);
        baseRoomConfigure.getBaseCreateRoom().setPrizePool(null);
        baseRoomConfigure.getBaseCreateRoom().setPassword(EncryptUtils.encryptDES(baseRoomConfigure.getBaseCreateRoom().getPassword()));
        baseRoomConfigure.setUnionRoomConfig(null);
        baseRoomConfigure.setArenaRoomCfg(null);
        baseRoomConfigure.setRobotRoomCfg(null);
    }


    /**
     * 解散并回退亲友圈房卡
     *
     * @param baseRoomConfigure 房间配置信息
     */
    @SuppressWarnings("rawtypes")
    private void backClubConsume(BaseRoomConfigure baseRoomConfigure, long clubId) {
        if (Config.isShare()) {
            List<ShareRoom> roomInitList = ShareRoomMgr.getInstance().getRoomInitList(baseRoomConfigure.getBaseCreateRoom().getGameIndex(), clubId, RoomTypeEnum.CLUB);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (ShareRoom shareRoom : roomInitList) {
                    // 房间初始状态
                    if (RoomState.Init.equals(shareRoom.getRoomState())) {
                        if (RoomTypeEnum.CLUB.equals(shareRoom.getRoomTypeEnum()) && shareRoom.getConfigId() == baseRoomConfigure.getBaseCreateRoom().getGameIndex() && clubId == shareRoom.getSpecialRoomId()) {
                            //空配置房间
                            if (shareRoom.isNoneRoom()) {
                                // 房间解散
                                ShareRoomMgr.getInstance().doDissolveRoom(shareRoom);
                            } else {//正常房间通知游戏解散
                                MqDissolveRoomNotifyBo bo = new MqDissolveRoomNotifyBo(shareRoom.getRoomKey(), shareRoom.getCurShareNode());
                                // 通知解散房间
                                MqProducerMgr.get().send(MqTopic.DISSOLVE_ROOM_NOTIFY, bo);
                            }
                        } else {
                            CommLog.error("backClubConsume clubId:{},RoomKey :{}", clubId, baseRoomConfigure.getClubRoomCfg().getRoomKey());
                        }
                    }
                }
            }
        } else {
            List<RoomImpl> roomInitList = NormalRoomMgr.getInstance().getRoomInitList(baseRoomConfigure.getBaseCreateRoom().getGameIndex(), clubId, RoomTypeEnum.CLUB);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (RoomImpl roomImpl : roomInitList) {
                    // 房间初始状态
                    if (RoomState.Init.equals(roomImpl.getRoomState())) {
                        if (RoomTypeEnum.CLUB.equals(roomImpl.getRoomTypeEnum()) && roomImpl.getConfigId() == baseRoomConfigure.getBaseCreateRoom().getGameIndex() && clubId == roomImpl.getSpecialRoomId()) {
                            // 房间解散
                            roomImpl.doDissolveRoom(Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
                        } else {
                            CommLog.error("backClubConsume clubId:{},RoomKey :{}", clubId, baseRoomConfigure.getClubRoomCfg().getRoomKey());
                        }
                    }
                }
            }
        }
    }


    /**
     * 获取亲友圈创建房间配置
     *
     * @param request           请求
     * @param club              亲友圈
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ClubCreateGameSet getClubCreateGameSet(WebSocketRequest request, Club club,
                                                   BaseRoomConfigure baseRoomConfigure) {
        // 存在配置标记
        if (baseRoomConfigure.getBaseCreateRoom().getGameIndex() <= 0L) {
            // 新配置
            return new ClubCreateGameSet();
        }
        // 通过配置标记获取配置
        ClubCreateGameSet gameSet = club.getMCreateGamesetMap()
                .get(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        if (ObjectUtils.allNotNull(gameSet)) {
            if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOT.value() == gameSet.getStatus() || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == gameSet.getStatus()) {
                // 旧房间的状态不是正常
                request.error(ErrorCode.NotAllow, "CreateNoneClubRoom CLUB_CRATE_GAME_SET_STATUS Name:{%s} Value:{%d}",
                        Club_CreateGameSetStatus.valueOf(gameSet.getStatus()), gameSet.getStatus());
            } else if (Objects.nonNull(baseRoomConfigure.getBaseCreateRoomT()) && baseRoomConfigure.getBaseCreateRoomT().equals(gameSet.getbRoomConfigure().getBaseCreateRoomT())) {
                // 新旧配置是一样的无需重新编辑。
                request.response();
            } else if (gameSet.getbRoomConfigure().getBaseCreateRoom().getClubId() != baseRoomConfigure
                    .getBaseCreateRoom().getClubId()) {
                // 配置的亲友圈ID不正确。
                request.error(ErrorCode.NotAllow, "CreateNoneClubRoom New_ClubID:{%d} and Old_ClubID:{%d}",
                        baseRoomConfigure.getBaseCreateRoom().getClubId(),
                        gameSet.getbRoomConfigure().getBaseCreateRoom().getClubId());
            } else {

                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == gameSet.getStatus()) {
                    // 如果是禁用配置的话，则直接修改配置
                    gameSet.getbRoomConfigure().setBaseCreateRoom(baseRoomConfigure.getBaseCreateRoomT());
                    request.response();
                    // 亲友圈房间类型分组
                    Map<RoomState, Long> map = NormalRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, club.getClubListBO().getId());
                    ClubMgr.getInstance().getClubMemberMgr().notify2AllByClubMinister(club.getClubListBO().getId(), SClub_CreateGameSetChange.make(club.getClubListBO().getId(), player.getPid(), false, club.getClubCreateGameSetInfo(gameSet),
                            NormalRoomMgr.Value(map, RoomState.Init),
                            NormalRoomMgr.Value(map, RoomState.Playing), club.getClubListBO().getMemberCreationRoom()));
                    return null;
                } else {
                    this.backClubConsume(gameSet.getbRoomConfigure(), club.getClubListBO().getId());
                    // 获取配置
                    return gameSet;
                }
            }
        } else {
            // 配置为空
            request.error(ErrorCode.NotAllow, "CreateNoneClubRoom Update GameIndex:{%d}",
                    baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        }
        // 返回错误
        return null;
    }

    /**
     * 获取亲友圈创建房间配置共享模式
     *
     * @param request           请求
     * @param club              亲友圈
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ClubCreateGameSet getClubCreateGameSetShare(WebSocketRequest request, Club club,
                                                        BaseRoomConfigure baseRoomConfigure) {
        // 存在配置标记
        if (baseRoomConfigure.getBaseCreateRoom().getGameIndex() <= 0L) {
            // 新配置
            return new ClubCreateGameSet();
        }
        // 通过配置标记获取配置
        ClubCreateGameSet gameSet = club.getMCreateGamesetMap()
                .get(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        if (ObjectUtils.allNotNull(gameSet)) {
            if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOT.value() == gameSet.getStatus() || Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == gameSet.getStatus()) {
                // 旧房间的状态不是正常
                request.error(ErrorCode.NotAllow, "CreateNoneClubRoom CLUB_CRATE_GAME_SET_STATUS Name:{%s} Value:{%d}",
                        Club_CreateGameSetStatus.valueOf(gameSet.getStatus()), gameSet.getStatus());
            } else if (Objects.nonNull(baseRoomConfigure.getShareBaseCreateRoom()) && baseRoomConfigure.getShareBaseCreateRoom().equals(gameSet.getbRoomConfigure().getShareBaseCreateRoom())) {
                // 新旧配置是一样的无需重新编辑。
                request.response();
            } else if (gameSet.getbRoomConfigure().getBaseCreateRoom().getClubId() != baseRoomConfigure
                    .getBaseCreateRoom().getClubId()) {
                // 配置的亲友圈ID不正确。
                request.error(ErrorCode.NotAllow, "CreateNoneClubRoom New_ClubID:{%d} and Old_ClubID:{%d}",
                        baseRoomConfigure.getBaseCreateRoom().getClubId(),
                        gameSet.getbRoomConfigure().getBaseCreateRoom().getClubId());
            } else {

                if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == gameSet.getStatus()) {
                    // 如果是禁用配置的话，则直接修改配置
                    gameSet.getbRoomConfigure().setBaseCreateRoom(baseRoomConfigure.getBaseCreateRoomT());
                    request.response();
                    // 亲友圈房间类型分组
                    Map<RoomState, Long> map = NormalRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, club.getClubListBO().getId());
                    ClubMgr.getInstance().getClubMemberMgr().notify2AllByClubMinister(club.getClubListBO().getId(), SClub_CreateGameSetChange.make(club.getClubListBO().getId(), player.getPid(), false, club.getClubCreateGameSetInfo(gameSet),
                            NormalRoomMgr.Value(map, RoomState.Init),
                            NormalRoomMgr.Value(map, RoomState.Playing), club.getClubListBO().getMemberCreationRoom()));
                    return null;
                } else {
                    this.backClubConsume(gameSet.getbRoomConfigure(), club.getClubListBO().getId());
                    // 获取配置
                    return gameSet;
                }
            }
        } else {
            // 配置为空
            request.error(ErrorCode.NotAllow, "CreateNoneClubRoom Update GameIndex:{%d}",
                    baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        }
        // 返回错误
        return null;
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private SData_Result checkRefNoneClub(BaseRoomConfigure baseRoomConfigure) {
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != baseRoomConfigure.getBaseCreateRoom()
                .getPaymentRoomCardType()) {
            // 不是房主付
            return SData_Result.make(ErrorCode.Success, 0L);
        }
        SData_Result result = RefRoomCost.GetCost(baseRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        return result;
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createAndEnter(RoomImpl roomImpl, int posID, String roomKey, long clubId) {
        // 查询并进入相同配置的房间
        AbsBaseRoom baseRoom = NormalRoomMgr.getInstance().queryExistEmptyPos(roomImpl.getBaseRoomConfigure().getGameType(), roomImpl.getSpecialRoomId(), roomImpl.getConfigId());
        if (Objects.isNull(baseRoom)) {
            SData_Result result = createClubRoom(roomImpl.getBaseRoomConfigure(), null, roomImpl.getSpecialRoomId());
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            baseRoom = (AbsBaseRoom) result.getData();
        }
        return this.findAndEnter(baseRoom, posID, roomKey, clubId);
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createAndEnterShare(ShareRoom shareRoom, int posID, String roomKey, long clubId, boolean existQuickJoin) {
        // 查询并进入相同配置的房间
        GameType gameType = ShareRoomMgr.getInstance().getByShareRoomGameType(shareRoom.getBaseRoomConfigure().getGameType());
        AbsBaseRoom baseRoom = existQuickJoin ? NormalRoomMgr.getInstance().queryExistEmptyPos(gameType, shareRoom.getSpecialRoomId(), shareRoom.getConfigId())
                : NormalRoomMgr.getInstance().queryExistEmptyPosCount(gameType, shareRoom.getSpecialRoomId(), shareRoom.getConfigId());
        if (Objects.isNull(baseRoom)) {
            BaseRoomConfigure baseRoomConfigure = ShareRoomMgr.getInstance().getBaseRoomConfigure(shareRoom);
            SData_Result result = createClubRoom(baseRoomConfigure, null, shareRoom.getSpecialRoomId());
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            baseRoom = (AbsBaseRoom) result.getData();
        }
        return this.findAndEnter(baseRoom, posID, roomKey, clubId);
    }


    /**
     * 创建赛事房间
     *
     * @param baseRoomConfigure
     * @param roomKey
     * @param specialRoomId
     * @return
     */
    public SData_Result createClubRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long specialRoomId) {
        // 赛事信息
        Club club = null;
        if (Config.isShare()) {
            club = ClubMgr.getInstance().getClubListMgr().findClubShare(specialRoomId);
        } else {
            club = ClubMgr.getInstance().getClubListMgr().findClub(specialRoomId);
        }
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        // 检查并消耗主裁判身上房卡值
        SData_Result result = club.checkRefNoneClubRoomCost(baseRoomConfigure, Club_define.Club_OperationStatus.CLUB_OPERATION_STATUS_CREATE);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        // 创建一个新的房间
        AbsBaseRoom baseRoom = NormalRoomMgr.getInstance().createClubRoom(baseRoomConfigure, StringUtils.isEmpty(roomKey) ? RoomKeyMgr.getInstance().getNewKey() : roomKey, club.getClubListBO().getOwnerID(), (int) result.getCustom(), club.getClubListBO().getName());
        if (Objects.isNull(baseRoom)) {
            // 创建房间失败
            club.gainClubRoomCard(baseRoomConfigure, Club_define.Club_OperationStatus.CLUB_OPERATION_STATUS_GAME);
            return SData_Result.make(ErrorCode.NotFind_Room, "createAndQuery NotFind_Room");
        }
        //钻石消耗的通知
//		this.checkDiamondsAttention(club);
        return SData_Result.make(ErrorCode.Success, baseRoom);
    }

//	/**
//	 * 钻石消耗通知 全员 或者只通知管理
//	 * @param club
//	 */
//	private void checkDiamondsAttention(Club club) {
//		if(Objects.isNull(club)){
//			return;
//		}
//		//已经加入赛事的话 就只需要通知赛事的
//		if (club.getClubListBO().getUnionId() > 0L) {
//			return;
//		}
//		//没有加入赛事的话 通知亲友圈
//		//获取亲友圈圈主的钻石
//		int diamondsValue=club.getOwnerPlayer().getFeature(PlayerWallet.class).walletRecord().getValue();
//		//如果钻石小于设定的值则发起通知
//		if(club.getClubListBO().getDiamondsAttentionAll()>diamondsValue&&!club.isDiamondsAttentionAll()){
//			club.setDiamondsAttentionAll(true);
//			ClubMgr.getInstance().getClubMemberMgr().notify2AllByClubInClubSign(club.getClubListBO().getId(), SClub_DiamondsNotEnough.make(club.getClubListBO().getId(),club.getClubListBO().getName(),club.getClubListBO().getDiamondsAttentionAll()));
//		}
//		if(club.getClubListBO().getDiamondsAttentionMinister()>diamondsValue&&!club.isDiamondsAttentionMinister()){
//			club.setDiamondsAttentionMinister(true);
//			ClubMgr.getInstance().getClubMemberMgr().notify2AllMinisterByClubInClubSign(club.getClubListBO().getId(), SClub_DiamondsNotEnough.make(club.getClubListBO().getId(),club.getClubListBO().getName(),club.getClubListBO().getDiamondsAttentionMinister()));
//		}
//        //如果钻石数量超过的时候 把通知的标志设置回来
//        if(diamondsValue>club.getClubListBO().getDiamondsAttentionAll()&&club.isDiamondsAttentionAll()){
//            club.setDiamondsAttentionAll(false);
//        }
//        if(diamondsValue>club.getClubListBO().getDiamondsAttentionMinister()&&club.isDiamondsAttentionMinister()){
//            club.setDiamondsAttentionMinister(false);
//        }
//	}


    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndEnter(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        SData_Result result = null;
        if (null == room) {
            return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
        }
        // 检查加入亲友圈房间
        result = ClubMgr.getInstance().getClubMemberMgr().checkJoinClub((Club) room.getRoomTyepImpl().getSpecialRoom(), this.player.getPid(), room.getRoomPidAll(), room.getBaseRoomConfigure().getBaseCreateRoom());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 游戏配置被禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", 0L), Restrictions.eq("clubId", room.getSpecialRoomId()), Restrictions.eq("pid", getPid()), Restrictions.in("configId", Arrays.asList(room.getConfigId(), 0L))))) {
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        ClubMemberBO clubMemberBO = (ClubMemberBO) result.getData();
        // 重复进入同一个房间
        if (this.getRoomID() == room.getRoomID()) {
            // 进入房间信息
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        }
        // 保存最近进入的配置Id
        clubMemberBO.saveConfigId(room.getConfigId(), 0);
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
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != room.getBaseRoomConfigure()
                .getBaseCreateRoom().getPaymentRoomCardType()) {
            result = this.checkRefRoomCost(room.getBaseRoomConfigure());
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
        }
        // 查询并进入房间。
        result = room.enterRoom(this.getPid(), posID, false, clubMemberBO);
        if (ErrorCode.Success.equals(result.getCode())) {
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(room);
            }
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        } else {
            // 根据房间支付类型返回房卡。
            this.getPlayer().getFeature(PlayerClub.class).clubCardReturnCradRoom(this.getPlayer().getRoomInfo().getConsumeCard(), room.getBaseRoomConfigure().getGameType(), room.getSpecialRoomId(), ItemFlow.ClubCardClubRoom);
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(room);
            }
            return result;
        }
    }

    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndSeat(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        SData_Result result = null;
        if (null == room) {
            return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
        }
        // 检查加入亲友圈房间
        result = ClubMgr.getInstance().getClubMemberMgr().checkJoinClub((Club) room.getRoomTyepImpl().getSpecialRoom(), this.player.getPid(), room.getRoomPidAll(), room.getBaseRoomConfigure().getBaseCreateRoom());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 游戏配置被禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", 0L), Restrictions.eq("clubId", room.getSpecialRoomId()), Restrictions.eq("pid", getPid()), Restrictions.in("configId", Arrays.asList(room.getConfigId(), 0L))))) {
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        ClubMemberBO clubMemberBO = (ClubMemberBO) result.getData();
        // 重复进入同一个房间
//		if (this.getRoomID() == room.getRoomID()) {
//			// 进入房间信息
//			return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
//		}
        // 保存最近进入的配置Id
        clubMemberBO.saveConfigId(room.getConfigId(), 0);
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
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != room.getBaseRoomConfigure()
                .getBaseCreateRoom().getPaymentRoomCardType()) {
            result = this.checkRefRoomCost(room.getBaseRoomConfigure());
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
        }
        // 查询并进入房间。
        result = room.seatRoom(this.getPid(), posID, false, clubMemberBO);
        if (ErrorCode.Success.equals(result.getCode())) {
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(room);
            }
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        } else {
            // 根据房间支付类型返回房卡。
            this.getPlayer().getFeature(PlayerClub.class).clubCardReturnCradRoom(this.getPlayer().getRoomInfo().getConsumeCard(), room.getBaseRoomConfigure().getGameType(), room.getSpecialRoomId(), ItemFlow.ClubCardClubRoom);
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().addShareRoom(room);
            }
            return result;
        }
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private SData_Result checkRefRoomCost(BaseRoomConfigure baseRoomConfigure) {
        SData_Result result = RefRoomCost.GetCost(baseRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        // 获取消耗
        int roomCard = DiscountMgr.getInstance().calculateRoomCardCost(getPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), baseRoomConfigure.getBaseCreateRoom().getClubId(), 0L, baseRoomConfigure.getGameType().getId(), (int) result.getCustom());
        // 检查并消耗房卡
        if (roomCard > 0 && !this.player.getFeature(PlayerClub.class).checkClubCardAndConsume(baseRoomConfigure, ItemFlow.ClubCardClubRoom)) {
            // 房卡消耗不够。
            return SData_Result.make(ErrorCode.NotEnough_ClubCard,
                    "PlayerClubRoom NotEnough_ClubCard ConsumeCard:{%d}", roomCard);
        }
        // 设置消费房卡
        this.getPlayer().getRoomInfo().setConsumeCard(roomCard);
        //共享数据
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this.getPlayer(), "roomInfo");
        }
        return SData_Result.make(ErrorCode.Success);
    }

}
