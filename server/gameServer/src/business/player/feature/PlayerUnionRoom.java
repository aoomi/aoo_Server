package business.player.feature;

import BaseCommon.CommLog;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.room.key.RoomKeyMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.rocketmq.bo.MqDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayerMgr;
import cenum.RoomTypeEnum;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.config.refdata.ref.RefRoomCost;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionBanGamePlayerBOService;
import core.db.service.clarkGame.UnionBanRoomConfigBOService;
import core.db.service.clarkGame.UnionRoomConfigBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.union.UnionCreateGameSet;
import jsproto.c2s.cclass.union.UnionDefine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
public class PlayerUnionRoom extends Feature {

    public PlayerUnionRoom(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }

    /**
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public void createNoneUnionRoom(WebSocketRequest request, BaseRoomConfigure baseRoomConfigure) {
        // 初始赛事配置
        this.initBaseRoomConfigure(baseRoomConfigure);
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(baseRoomConfigure.getBaseCreateRoom().getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            request.error(ErrorCode.UNION_NOT_EXIST, "createNoneUnionRoom null == union UnionId:{%d}", baseRoomConfigure.getBaseCreateRoom().getUnionId());
            return;
        }
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(this.getPid(), baseRoomConfigure.getBaseCreateRoom().getClubId(), union.getUnionBO().getId())) {
            // 不是赛事管理员
            request.error(ErrorCode.UNION_NOT_MANAGE, "createNoneUnionRoom you have not minister");
            return;
        }
        // 检查房卡配置和玩家房卡
        SData_Result result = this.checkRefNoneUnion(baseRoomConfigure);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        // 创建、修改验证房间配置。
        UnionCreateGameSet gameSet = null;
        if (Config.isShare()) {
            gameSet = getUnionCreateGameSetShare(request, union, baseRoomConfigure);
        } else {
            gameSet = getUnionCreateGameSet(request, union, baseRoomConfigure);
        }
        if (Objects.isNull(gameSet)) {
            // 验证配置存在错误。
            return;
        }
        // 是否创建房间
        boolean isCreate = gameSet.getGameIndex() <= 0L;
        if (isCreate && !union.checkCanCreateRoom()) {
            // 如果是创造并且超过限制
            request.error(ErrorCode.UNION_MAXCRATESET, "UNION_MAXCRATESET");
            return;
        }
        // 获取房间key
        int roomCard = (int) result.getCustom();
        request.response();
        // 创建游戏房间
        result = NormalRoomMgr.getInstance().createNoneUnionRoom(baseRoomConfigure, "", this.getPid(), roomCard, union.getUnionBO().getName());
        gameSet.setRoomConfigure((BaseRoomConfigure) result.getData());
        gameSet.setStatus(UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL.value());
        gameSet.setCreateTime(isCreate ? CommTime.nowSecond() : gameSet.getCreateTime());
        gameSet.addRoomCount();
        gameSet.incrementWait();
        // 保存或更新房间配置
        if (saveOrUpDate(gameSet)) {
            union.getRoomConfigBOMap().put(gameSet.getGameIndex(), gameSet);
            // 设置标识id
            gameSet.setTagId(union.getCurTabId(gameSet.getGameIndex()));
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().updateBaseRoomConfigure(gameSet.getbRoomConfigure());
            }
            // 添加游戏设置
            union.setCreateGameSet(this.getPid(), gameSet, isCreate);
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().addUnion(union);
            }
//            if(isCreate) {
//                FlowLogger.roomConfigPrizePoolLog(CommTime.getNowTimeStringYMD(), gameSet.getGameIndex(), 0, 0, 0, 0, baseRoomConfigure.getBaseCreateRoom().getUnionId(), baseRoomConfigure.getBaseCreateRoom().getRoomName(), new Gson().toJson(baseRoomConfigure.getBaseCreateRoomT()), 0,baseRoomConfigure.getGameType().getId());
//            }

        }
    }


    /**
     * 解散并回退联赛房卡
     *
     * @param baseRoomConfigure 房间配置信息
     */
    @SuppressWarnings("rawtypes")
    private void backUnionConsume(BaseRoomConfigure baseRoomConfigure, long unionId) {
        if (Config.isShare()) {
            List<ShareRoom> roomInitList = ShareRoomMgr.getInstance().getRoomInitList(baseRoomConfigure.getBaseCreateRoom().getGameIndex(), unionId, RoomTypeEnum.UNION);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (ShareRoom shareRoom : roomInitList) {
                    // 房间初始状态
                    if (RoomState.Init.equals(shareRoom.getRoomState())) {
                        if (RoomTypeEnum.UNION.equals(shareRoom.getRoomTypeEnum()) && shareRoom.getConfigId() == baseRoomConfigure.getBaseCreateRoom().getGameIndex() && unionId == shareRoom.getSpecialRoomId()) {
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
                            CommLog.error("backUnionConsume unionId:{},RoomKey :{}", unionId, baseRoomConfigure.getUnionRoomCfg().getRoomKey());
                        }
                    }
                }
            }
        } else {
            List<RoomImpl> roomInitList = NormalRoomMgr.getInstance().getRoomInitList(baseRoomConfigure.getBaseCreateRoom().getGameIndex(), unionId, RoomTypeEnum.UNION);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (RoomImpl roomImpl : roomInitList) {
                    // 房间初始状态
                    if (RoomState.Init.equals(roomImpl.getRoomState())) {
                        if (RoomTypeEnum.UNION.equals(roomImpl.getRoomTypeEnum()) && roomImpl.getConfigId() == baseRoomConfigure.getBaseCreateRoom().getGameIndex() && unionId == roomImpl.getSpecialRoomId()) {
                            // 房间解散
                            roomImpl.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
                        } else {
                            CommLog.error("backUnionConsume unionId:{},RoomKey :{}", unionId, baseRoomConfigure.getUnionRoomCfg().getRoomKey());
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始配置
     *
     * @param baseRoomConfigure 公共
     */
    private void initBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        BaseCreateRoom baseCreateRoom = baseRoomConfigure.getBaseCreateRoom();
        baseCreateRoom.setIsClubMemberCreateRoom(null);
        baseCreateRoom.setClubWinnerPayConsume(null);
        baseCreateRoom.setClubCostType(null);
        baseCreateRoom.setSportsDouble(CommMath.FormatDouble(baseCreateRoom.getSportsDouble().doubleValue()));
        baseCreateRoom.setAutoDismiss(setAutoDismiss(baseCreateRoom));
        baseCreateRoom.setRoomSportsThreshold(setRoomSportsThreshold(baseCreateRoom));
        baseCreateRoom.setRoomSportsType(0);
        baseCreateRoom.setRoomSportsEveryoneConsume(CommMath.FormatDouble(baseCreateRoom.getRoomSportsEveryoneConsume().doubleValue()));
        baseCreateRoom.setRoomSportsBigWinnerConsume(CommMath.FormatDouble(baseCreateRoom.getRoomSportsBigWinnerConsume().doubleValue()));
        baseCreateRoom.setBigWinnerConsumeList(baseCreateRoom.getBigWinnerConsumeList());
        baseCreateRoom.setPaymentRoomCardType(PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value());
        baseCreateRoom.updateRoomName(SensitiveWordMgr.getInstance().replaceSensitiveWordMax(baseCreateRoom.getRoomName()));
        baseCreateRoom.setPrizePool(CommMath.FormatDouble(baseCreateRoom.getPrizePool().doubleValue()));
        baseCreateRoom.setPassword(EncryptUtils.encryptDES(baseCreateRoom.getPassword()));
        baseRoomConfigure.setClubRoomCfg(null);
        baseRoomConfigure.setArenaRoomCfg(null);
        baseRoomConfigure.setRobotRoomCfg(null);
    }

    /**
     * 最小值限制
     *
     * @param baseCreateRoom
     * @return
     */
    private double setAutoDismiss(BaseCreateRoom baseCreateRoom) {
        if (baseCreateRoom.getAutoDismiss().doubleValue() <= -10000D) {
            return -10000D;
        }
        return CommMath.FormatDouble(baseCreateRoom.getAutoDismiss().doubleValue());
    }

    /**
     * 房间竞技点门槛
     *
     * @param baseCreateRoom
     * @return
     */
    private double setRoomSportsThreshold(BaseCreateRoom baseCreateRoom) {
        if (baseCreateRoom.getRoomSportsThreshold().doubleValue() <= -10000D) {
            return -10000D;
        }
        return CommMath.FormatDouble(baseCreateRoom.getRoomSportsThreshold().doubleValue());
    }

    /**
     * 保存或更新配置
     *
     * @param gameSet 游戏配置
     * @return
     */
    public boolean saveOrUpDate(UnionCreateGameSet gameSet) {
        return ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).saveOrUpDate(gameSet);
    }

    /**
     * 获取联赛创建房间配置
     *
     * @param request           请求
     * @param union             联赛
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private UnionCreateGameSet getUnionCreateGameSet(WebSocketRequest request, Union union,
                                                     BaseRoomConfigure baseRoomConfigure) {
        // 存在配置标记
        if (baseRoomConfigure.getBaseCreateRoom().getGameIndex() <= 0L) {
            // 新配置
            return new UnionCreateGameSet();
        }
        // 通过配置标记获取配置
        UnionCreateGameSet gameSet = union.getRoomConfigBOMap().get(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        if (ObjectUtils.allNotNull(gameSet)) {
            if (Objects.nonNull(baseRoomConfigure.getBaseCreateRoomT()) && baseRoomConfigure.getBaseCreateRoomT().equals(gameSet.getbRoomConfigure().getBaseCreateRoomT())) {
                // 新旧配置是一样的无需重新编辑。
                request.response();
            } else if (gameSet.getbRoomConfigure().getBaseCreateRoom().getUnionId() != baseRoomConfigure.getBaseCreateRoom().getUnionId()) {
                // 配置的联赛ID不正确。
                request.error(ErrorCode.NotAllow, "CreateNoneUnionRoom New_UnionID:{%d} and Old_UnionID:{%d}", baseRoomConfigure.getBaseCreateRoom().getUnionId(), gameSet.getbRoomConfigure().getBaseCreateRoom().getUnionId());
            } else {
                // 解散并回退联赛房卡
                this.backUnionConsume(gameSet.getbRoomConfigure(), union.getUnionBO().getId());
                // 获取配置
                return gameSet;
            }
        } else {
            // 配置为空
            request.error(ErrorCode.NotAllow, "CreateNoneUnionRoom Update GameIndex:{%d}",
                    baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        }
        // 返回错误
        return null;
    }

    /**
     * 获取联赛创建房间配置共享模式
     *
     * @param request           请求
     * @param union             联赛
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private UnionCreateGameSet getUnionCreateGameSetShare(WebSocketRequest request, Union union,
                                                          BaseRoomConfigure baseRoomConfigure) {
        // 存在配置标记
        if (baseRoomConfigure.getBaseCreateRoom().getGameIndex() <= 0L) {
            // 新配置
            return new UnionCreateGameSet();
        }
        // 通过配置标记获取配置
        UnionCreateGameSet gameSet = union.getRoomConfigBOMap().get(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        if (ObjectUtils.allNotNull(gameSet)) {
            if (Objects.nonNull(baseRoomConfigure.getShareBaseCreateRoom()) && baseRoomConfigure.getShareBaseCreateRoom().equals(gameSet.getbRoomConfigure().getShareBaseCreateRoom())) {
                // 新旧配置是一样的无需重新编辑。
                request.response();
            } else if (gameSet.getbRoomConfigure().getBaseCreateRoom().getUnionId() != baseRoomConfigure.getBaseCreateRoom().getUnionId()) {
                // 配置的联赛ID不正确。
                request.error(ErrorCode.NotAllow, "CreateNoneUnionRoom New_UnionID:{%d} and Old_UnionID:{%d}", baseRoomConfigure.getBaseCreateRoom().getUnionId(), gameSet.getbRoomConfigure().getBaseCreateRoom().getUnionId());
            } else {
                // 解散并回退联赛房卡
                this.backUnionConsume(gameSet.getbRoomConfigure(), union.getUnionBO().getId());
                // 获取配置
                return gameSet;
            }
        } else {
            // 配置为空
            request.error(ErrorCode.NotAllow, "CreateNoneUnionRoom Update GameIndex:{%d}",
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
    private SData_Result checkRefNoneUnion(BaseRoomConfigure baseRoomConfigure) {
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != baseRoomConfigure.getBaseCreateRoom().getPaymentRoomCardType()) {
            // 不是房主付
            return SData_Result.make(ErrorCode.UNION_ROOM_CFG_ERROR, "UNION_ROOM_CFG_ERROR getPaymentRoomCardType:{%d}", baseRoomConfigure.getBaseCreateRoom().getPaymentRoomCardType());
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
     * 共享进入房间
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
            SData_Result result = createUnionRoom(ShareRoomMgr.getInstance().getBaseRoomConfigure(shareRoom), null, shareRoom.getSpecialRoomId());
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
    public SData_Result createAndEnter(RoomImpl roomImpl, int posID, String roomKey, long clubId) {
        // 查询并进入相同配置的房间
        AbsBaseRoom baseRoom = NormalRoomMgr.getInstance().queryExistEmptyPos(roomImpl.getBaseRoomConfigure().getGameType(), roomImpl.getSpecialRoomId(), roomImpl.getConfigId());
        if (Objects.isNull(baseRoom)) {
            SData_Result result = createUnionRoom(roomImpl.getBaseRoomConfigure(), null, roomImpl.getSpecialRoomId());
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
    public SData_Result createUnionRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long specialRoomId) {
        // 赛事信息
        Union union = null;
        //共享赛事
        if (Config.isShare()) {
            union = UnionMgr.getInstance().getUnionListMgr().findUnionShare(specialRoomId);
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(specialRoomId);
        }
        if (Objects.isNull(union)) {
            CommLogD.error("赛事不存在specialRoomId={},roomKey={}", specialRoomId, roomKey);
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        // 检查并消耗主裁判身上房卡值
        SData_Result result = union.checkRefNoneUnionRoomCost(baseRoomConfigure, UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_CREATE);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        // 创建一个新的房间
        AbsBaseRoom baseRoom = NormalRoomMgr.getInstance().createUnionRoom(baseRoomConfigure, StringUtils.isEmpty(roomKey) ? RoomKeyMgr.getInstance().getNewKey() : roomKey, union.getUnionBO().getOwnerId(), (int) result.getCustom(), baseRoomConfigure.getUnionRoomCfg().getName());
        if (Objects.isNull(baseRoom)) {
            // 创建房间失败
            union.gainUnionRoomCard(baseRoomConfigure, UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_GAME);
            return SData_Result.make(ErrorCode.NotFind_Room, "createAndQuery NotFind_Room");
        }
        return SData_Result.make(ErrorCode.Success, baseRoom);
    }


    /**
     * 进入房间
     *
     * @param posID   位置ID
     * @param roomKey 房间号
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndEnter(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        SData_Result result = null;
        if (Objects.isNull(room)) {
            return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
        }
        // 检查加入
        result = UnionMgr.getInstance().getUnionMemberMgr().checkJoinUnion((Union) room.getRoomTyepImpl().getSpecialRoom(), clubId, this.player, room.getRoomPidAll(), room.getBaseRoomConfigure().getBaseCreateRoom());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMember clubMember = (ClubMember) result.getData();
        // 检查比赛分预警值
        result = this.checkSportsPointWarning(clubMember, room, clubId);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }

        // 重复进入同一个房间
        if (this.getRoomID() == room.getRoomID()) {
            // 进入房间信息
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        }
        if (clubMember.getClubMemberBO().getBanGame() > 0) {
            // 被亲友圈管理员禁止加入房间。
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        if (clubMember.getClubMemberBO().getUnionBanGame() > 0) {
            // 被赛事管理员禁止加入房间。
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        //被赛事禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanGamePlayerBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", room.getSpecialRoomId()), Restrictions.eq("pid", getPid())))) {
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        // 游戏配置被禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", room.getSpecialRoomId()), Restrictions.eq("clubId", clubId), Restrictions.eq("pid", getPid()), Restrictions.in("configId", Arrays.asList(room.getConfigId(), 0L))))) {
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        // 保存最近进入的配置Id
        clubMember.getClubMemberBO().saveConfigId(room.getConfigId(), room.getSpecialRoomId());
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
            return SData_Result.make(ErrorCode.NotAllow, "");
        }
        // 查询并进入房间。
        result = room.enterRoom(this.getPid(), posID, false, clubMember.getClubMemberBO());
        if (ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        } else {
            return result;
        }
    }

    /**
     * 检查比赛分预警值
     *
     * @param clubMember 成员
     * @param room       房间信息
     * @param clubId     亲友圈Id
     * @return
     */
    private SData_Result checkSportsPointWarning(ClubMember clubMember, AbsBaseRoom room, long clubId) {
        //检查推广员的预警值
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkSportsPointWarning(clubMember);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(ErrorCode.WarningSport_RoomJoinner, "WarningSport_RoomJoinner");
        }
        //检查个人的预警值
        SData_Result resultPersonal = ClubMgr.getInstance().getClubMemberMgr().checkPersonalSportsPointWarning(clubMember);
        if (!ErrorCode.Success.equals(resultPersonal.getCode())) {
            return SData_Result.make(ErrorCode.PersonalWarningSport_RoomJoinner, "PersonalWarningSport_RoomJoinner");
        }
        //检查联盟的预警值
        SData_Result sportsPointWarnResult = UnionMgr.getInstance().getUnionMemberMgr().checkSportsPointWarning((Union) room.getRoomTyepImpl().getSpecialRoom(), clubId);
        if (!ErrorCode.Success.equals(sportsPointWarnResult.getCode())) {
            sportsPointWarnResult.setMsg("CLUB_SPORT_POINT_WARN");
            return sportsPointWarnResult;
        }
        //中至检查联盟生存积分
        SData_Result alivePointWarnResult = UnionMgr.getInstance().getUnionMemberMgr().checkAlivePoint((Union) room.getRoomTyepImpl().getSpecialRoom(), clubId);
        if (!ErrorCode.Success.equals(alivePointWarnResult.getCode())) {
            alivePointWarnResult.setMsg("您所在的亲友圈生存积分过低，无法加入比赛，请联系管理");
            return alivePointWarnResult;
        }

        //中至检查检查个人的生存积分
        SData_Result personallivePointWarnResult = ClubMgr.getInstance().getClubMemberMgr().checkAlivePointWarning(clubMember);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return personallivePointWarnResult;
        }
        return SData_Result.make(ErrorCode.Success);
    }

    public SData_Result findAndSeat(AbsBaseRoom room, int posID, String roomKey, long clubId) {
        SData_Result result = null;
        if (Objects.isNull(room)) {
            return SData_Result.make(ErrorCode.NotFind_Room, "NotFind_Room");
        }

        // 检查加入
        result = UnionMgr.getInstance().getUnionMemberMgr().checkJoinUnion((Union) room.getRoomTyepImpl().getSpecialRoom(), clubId, this.player, room.getRoomPidAll(), room.getBaseRoomConfigure().getBaseCreateRoom());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMember clubMember = (ClubMember) result.getData();
        // 检查比赛分预警值
        result = this.checkSportsPointWarning(clubMember, room, clubId);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }

        // 重复进入同一个房间
//        if (this.getRoomID() == room.getRoomID()) {
//            // 进入房间信息
//            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
//        }
        if (clubMember.getClubMemberBO().getBanGame() > 0) {
            // 被亲友圈管理员禁止加入房间。
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        if (clubMember.getClubMemberBO().getUnionBanGame() > 0) {
            // 被赛事管理员禁止加入房间。
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        //被赛事禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanGamePlayerBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", room.getSpecialRoomId()), Restrictions.eq("pid", getPid())))) {
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        // 游戏配置被禁止游戏了
        if (ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).anyFind(Restrictions.and(Restrictions.eq("unionId", room.getSpecialRoomId()), Restrictions.eq("clubId", clubId), Restrictions.eq("pid", getPid()), Restrictions.in("configId", Arrays.asList(room.getConfigId(), 0L))))) {
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, ErrorCode.UNION_BAN_GAME.name());
        }
        // 保存最近进入的配置Id
        clubMember.getClubMemberBO().saveConfigId(room.getConfigId(), room.getSpecialRoomId());
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().getPlayer(this.getPlayer());
        }
        // 检查高级选项
        result = room.getRoomPosMgr().checkEnterRoomGaoJi(getPlayer().getLocationInfo(), getPlayer().getIp());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 检查房间支付类型并消费卡
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() != room.getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            return SData_Result.make(ErrorCode.NotAllow, "");
        }
        // 重置消费卡
//        this.getPlayer().getRoomInfo().clear();
        // 查询并进入房间。
        result = room.seatRoom(this.getPid(), posID, false, clubMember.getClubMemberBO());
        if (ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        } else {
            return result;
        }
    }
}
