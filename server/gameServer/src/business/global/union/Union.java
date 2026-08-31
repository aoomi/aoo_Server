package business.global.union;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.config.DiscountMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.sharegm.ShareInitMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerCurrency;
import business.player.feature.PlayerFamily;
import business.rocketmq.bo.MqDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import cenum.*;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import core.config.refdata.ref.RefRoomCost;
import core.db.entity.clarkGame.*;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberLatylyConfigIDBOService;
import core.db.service.clarkGame.UnionGroupingBOService;
import core.db.service.clarkGame.UnionRoomConfigBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.disruptor.union.UnionMatchFactory;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.RoomCfgCount;
import jsproto.c2s.cclass.club.ClubMemberLatelyConfigIDItem;
import jsproto.c2s.cclass.club.ClubRoomConfigCalcActiveItem;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.room.RoomPosInfoShort;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.union.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public class Union implements Serializable {
    /**
     * 大赛事数据
     */
    private UnionBO unionBO = null;
    /**
     * key:ID,value:房间配置
     */
    private Map<Long, UnionCreateGameSet> roomConfigBOMap = new ConcurrentHashMap<>();

    /**
     * 标识配置Id
     */
    private Map<Integer, Long> limitMap = new ConcurrentHashMap<>();
//    /**
//     * 赛事创建者
//     */
//    private transient Player ownerPlayer = null;

    /**
     * 赛事创建者
     */
    private long ownerPlayerId = 0;
    /**
     * 赛事分组列表
     */
    private List<UnionGroupingBO> unionGroupingBOList;

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
     * 保险箱功能状态
     */
    private boolean caseStatusChange;
    /**
     * 皮肤修改状态
     */
    private boolean changeSkinStatus;

    public Union(UnionBO unionBO) {
        // 设置赛事数据
        this.setUnionBO(unionBO);
        // 初始化大赛事房间配置
        this.initUnionRoomConfig();
        // 初始化赛事分组
        this.initUnionGrouping();
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
        for (int i = 1; i <= this.curTabId; i++) {
            if (!this.limitMap.containsKey(i)) {
                this.limitMap.put(i, configId);
                //共享亲友圈更新
                if (Config.isShare()) {
                    ShareUnionListMgr.getInstance().updateField(this, "limitMap", "curTabId");
                }
                return i;
            }
        }
        int id = ++curTabId;
        this.limitMap.put(id, configId);
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateField(this, "limitMap", "curTabId");
        }
        return id;
    }

    /**
     * 初始化赛事分组
     */
    private void initUnionGrouping() {
        this.setUnionGroupingBOList(ContainerMgr.get().getComponent(UnionGroupingBOService.class).findAll(Restrictions.eq("unionId", this.getUnionBO().getId())));
        if (CollectionUtils.isEmpty(getUnionGroupingBOList())) {
            // 默认添加一个空限制组
            this.addUnionGrouping();
        }
    }

    /**
     * 主办亲友圈名称
     *
     * @return
     */
    public String getOwnerClubName() {
        // 主办亲友圈名称是否存在
        if (StringUtils.isEmpty(getUnionBO().getClubName())) {
            // 不存在，去获取
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getUnionBO().getClubId());
            if (Objects.nonNull(club)) {
                getUnionBO().saveClubName(club.getClubListBO().getName());
            }
        }
        // 返回名称
        return getUnionBO().getClubName();
    }

    public boolean updateOwnerPlayer(Player player) {
        // 设置圈主pid
        getUnionBO().saveOwnerId(player.getPid());
        // 设置创建者信息
        this.setOwnerPlayerId(player.getPid());
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateField(this, "ownerPlayerId");
        }
        return true;
    }

    /**
     * 获取赛事创建者
     *
     * @return
     */
    public Player getOwnerPlayer() {
        // 赛事创造者信息不存在
        if (this.getOwnerPlayerId() == 0) {
            // 设置创建者信息
            this.setOwnerPlayerId(this.getUnionBO().getOwnerId());
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().updateField(this, "ownerPlayerId");
            }
            return PlayerMgr.getInstance().getPlayer(this.getUnionBO().getOwnerId());
        } else {
            return PlayerMgr.getInstance().getPlayer(this.getOwnerPlayerId());
        }
    }

    /**
     * 初始化大赛事房间配置
     */
    private void initUnionRoomConfig() {
        List<UnionRoomConfigBO> unionRoomConfigBOList = ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).findAll(Restrictions.eq("unionId", this.getUnionBO().getId()));
        if (CollectionUtils.isEmpty(unionRoomConfigBOList)) {
            // 检查数据是否存在
            return;
        }

        //共享的情况
        if (Config.isShare()) {
            // 分配默认创建房间的索引
            for (UnionRoomConfigBO unionRoomConfigBO : unionRoomConfigBOList) {
                UnionCreateGameSet unionCreateGameSet = new UnionCreateGameSet();
                unionCreateGameSet.setStatus(unionRoomConfigBO.getStatus());
                unionCreateGameSet.setRoomConfigure(getBaseRoomConfigure(unionRoomConfigBO));
                unionCreateGameSet.setGameIndex(unionRoomConfigBO.getId());
                unionCreateGameSet.setRoomCount(0);
                unionCreateGameSet.setRoomCard(0);
                if (ShareInitMgr.getInstance().getShareDataInit()) {
                    unionCreateGameSet.setTagId(getCurTabId(unionRoomConfigBO.getId()));
                    this.getRoomConfigBOMap().put(unionRoomConfigBO.getId(), unionCreateGameSet);
                } else {
                    Union unionShare = ShareUnionListMgr.getInstance().getUnion(this.unionBO.getId());
                    if (unionShare != null) {
                        this.limitMap = unionShare.limitMap;
                        this.curTabId = unionShare.curTabId;
                        this.roomConfigBOMap = unionShare.roomConfigBOMap;
                    }
                    unionCreateGameSet.setTagId(getCurTabId(unionRoomConfigBO.getId()));
                }
                if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DELETE.value() == unionRoomConfigBO.getStatus()
                        || UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DISABLE.value() == unionRoomConfigBO.getStatus()) {
                    // 跳过解散配置和禁止配置
                    continue;
                }
                //修改原来的房间数据为共享模式数据
                initCreateGameSetToShare(unionCreateGameSet, unionRoomConfigBO);
                unionCreateGameSet.incrementWait();
                // 增加房间数
                unionCreateGameSet.addRoomCount();
                ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(unionCreateGameSet.getRoomKey());
                if (shareRoom == null || shareRoom.getUnionId() == 0 || ShareInitMgr.getInstance().getShareDataInit()) {
                    // 创建时间
                    unionCreateGameSet.setCreateTime(CommTime.nowSecond());
                    // 创建一个空赛事房间
                    unionCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneUnionRoom(unionCreateGameSet.getbRoomConfigure(), 0));
                } else {
//                    UnionCreateGameSet set = ShareUnionListMgr.getInstance().getUnion(this.unionBO.getId()).getRoomConfigBOMap().get(unionRoomConfigBO.getId());
//                    if (set != null) {
//                        unionCreateGameSet.setRoomCount(set.getRoomCount());
//                        unionCreateGameSet.setRoomCard(set.getRoomCard());
//                    }
                    // 创建时间
//                    unionCreateGameSet.setCreateTime(shareRoom.getCreateTime());
                    // 创建一个空赛事房间
//                    unionCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneUnionRoomShare(unionCreateGameSet.getbRoomConfigure(), unionCreateGameSet.getRoomKey(), 0));
                }
                // 保存配置
                unionRoomConfigBO.savaGameConfig(new Gson().toJson(unionCreateGameSet.getbRoomConfigure()), unionCreateGameSet.getbRoomConfigure().getGameType().getId(), CommTime.nowSecond());
            }
        } else {
            UnionCreateGameSet unionCreateGameSet = null;
            for (UnionRoomConfigBO unionRoomConfigBO : unionRoomConfigBOList) {
                unionCreateGameSet = new UnionCreateGameSet();
                unionCreateGameSet.setStatus(unionRoomConfigBO.getStatus());
                unionCreateGameSet.setRoomConfigure(getBaseRoomConfigure(unionRoomConfigBO));
                if (unionCreateGameSet.getRoomCard() > 0) {
                    // TODO 2020/05/25 需要特殊处理
                    // 回退房卡
//                this.gainUnionRoomCard(unionCreateGameSet.getbRoomConfigure(), UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_SERVER_RESTART);
                }
                unionCreateGameSet.setGameIndex(unionRoomConfigBO.getId());
                unionCreateGameSet.setRoomCount(0);
                unionCreateGameSet.setRoomCard(0);
                this.getRoomConfigBOMap().put(unionRoomConfigBO.getId(), unionCreateGameSet);
                if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DELETE.value() == unionRoomConfigBO.getStatus()
                        || UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DISABLE.value() == unionRoomConfigBO.getStatus()) {
                    // 跳过解散配置和禁止配置
                    continue;
                }
                unionCreateGameSet.setTagId(getCurTabId(unionRoomConfigBO.getId()));
                unionCreateGameSet.incrementWait();
                // 增加房间数
                unionCreateGameSet.addRoomCount();
                // 创建时间
                unionCreateGameSet.setCreateTime(CommTime.nowSecond());
                // 创建一个空赛事房间
                unionCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneUnionRoom(unionCreateGameSet.getbRoomConfigure(), 0));
                // 保存配置
                unionRoomConfigBO.savaGameConfig(new Gson().toJson(unionCreateGameSet.getbRoomConfigure()), unionCreateGameSet.getbRoomConfigure().getGameType().getId(), CommTime.nowSecond());
            }
        }
    }

    /**
     * 设置房间配置数据为共享模式的数据
     *
     * @param unionCreateGameSet
     * @param unionRoomConfigBO
     */
    private void initCreateGameSetToShare(UnionCreateGameSet unionCreateGameSet, UnionRoomConfigBO unionRoomConfigBO) {
        Gson gson = new Gson();
        Map<String, Object> baseRoomConfigure = gson.fromJson(unionRoomConfigBO.getGameConfig(), Map.class);
        //判断不是共享模式数据修改成共享模式的数据
        if (StringUtils.isEmpty(unionCreateGameSet.getbRoomConfigure().getShareBaseCreateRoom())) {
            Map<String, Object> baseCreateRoomMap = (Map) baseRoomConfigure.get("baseCreateRoom");
            String shareBaseCreateRoom = gson.toJson(baseCreateRoomMap);
            unionCreateGameSet.getbRoomConfigure().setShareBaseCreateRoom(shareBaseCreateRoom);
            BaseCreateRoom baseCreateRoom = gson.fromJson(shareBaseCreateRoom, BaseCreateRoom.class);
            if (Objects.nonNull(baseCreateRoom)) {
                String roomName = baseCreateRoom.getRoomName();
                if (StringUtils.isNotEmpty(roomName)) {
                    baseCreateRoom.setInitRoomName(SensitiveWordMgr.getInstance().replaceSensitiveWordMax(roomName));
                }
            }
            unionCreateGameSet.getbRoomConfigure().setBaseCreateRoom(baseCreateRoom);
        }

    }

    /**
     * 获取赛事房间配置
     *
     * @param unionRoomConfigBO
     * @return
     */
    private BaseRoomConfigure getBaseRoomConfigure(UnionRoomConfigBO unionRoomConfigBO) {
        try {
            return parseResult(unionRoomConfigBO.getGameConfig());
        } catch (ClassNotFoundException e) {
            CommLog.error("BaseRoomConfigure UnionId:{},ConfigId:{},GameId:{} ", unionBO.getId(), unionRoomConfigBO.getId(), unionRoomConfigBO.getGameId(), e);
        }
        return null;
    }


    /**
     * 解析赛事房间json配置
     *
     * @param json json
     * @return
     */
    private BaseRoomConfigure parseResult(String json) throws ClassNotFoundException {
        Gson gson = new Gson();
        BaseRoomConfigure baseRoomConfigure = gson.fromJson(json, BaseRoomConfigure.class);
        String crateCfg = gson.toJson(baseRoomConfigure.getBaseCreateRoomT());
        baseRoomConfigure.setBaseCreateRoom(gson.fromJson(crateCfg, BaseCreateRoom.class));
        String name = baseRoomConfigure.getUnionRoomCfg().getName();
        BaseCreateRoom baseCreateRoom = baseRoomConfigure.getBaseCreateRoom();
        if (Objects.nonNull(baseCreateRoom)) {
            String roomName = baseCreateRoom.getRoomName();
            if (StringUtils.isNotEmpty(roomName)) {
                baseCreateRoom.setInitRoomName(SensitiveWordMgr.getInstance().replaceSensitiveWordMax(roomName));
            }
        }
        return baseRoomConfigure;
    }


    /**
     * 返回赛事房卡
     *
     * @param baseRoomConfigure 房间公共配置
     * @param status            赛事操作状态
     */
    @SuppressWarnings("rawtypes")
    public void gainUnionRoomCard(BaseRoomConfigure baseRoomConfigure, UnionDefine.UNION_OPERATION_STATUS status) {
        this.getOwnerPlayer().getFeature(PlayerWallet.class).backUnionConsumeRoom(baseRoomConfigure.getUnionRoomCfg().getRoomCard(), baseRoomConfigure.getGameType(), this.getUnionBO().getId(), status, this.getUnionBO().getAgentsID(), this.getUnionBO().getLevel());
        baseRoomConfigure.getUnionRoomCfg().setRoomCard(0);
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @param baseRoomConfigure 房间公共配置
     * @param state             赛事操作状态
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public SData_Result checkRefNoneUnionRoomCost(BaseRoomConfigure baseRoomConfigure, UnionDefine.UNION_OPERATION_STATUS state) {
        SData_Result result = RefRoomCost.GetCost(baseRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return result;
        }
        // 获取消耗
        int roomCard = DiscountMgr.getInstance().calculateRoomCardCost(getOwnerPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), 0L, getUnionBO().getUnionSign(), baseRoomConfigure.getGameType().getId(), (int) result.getCustom());
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == baseRoomConfigure.getBaseCreateRoom().getPaymentRoomCardType()) {
            // 检查并赛事消耗房卡
            if (roomCard > 0 && !this.getOwnerPlayer().getFeature(PlayerWallet.class).checkAndUnionConsumeRoom(roomCard, baseRoomConfigure.getGameType(), baseRoomConfigure.getBaseCreateRoom().getUnionId(), state, this.getUnionBO().getAgentsID(), this.getUnionBO().getLevel())) {
                return SData_Result.make(ErrorCode.NotEnough_Currency, "union owner room-card balance is insufficient");

            }
        } else {
            roomCard = 0;
        }
//        //钻石消耗的通知
//        this.checkDiamondsAttention();
        return SData_Result.make(ErrorCode.Success, roomCard);
    }

//    /**
//     * 钻石消耗通知 全员 或者只通知管理
//     *
//     * @param
//     */
//    public void checkDiamondsAttention() {
//        //没有加入赛事的话 通知亲友圈
//        //获取亲友圈圈主的钻石
//        int diamondsValue = this.getOwnerPlayer().getFeature(PlayerWallet.class).walletRecord().getValue();
//        // 如果钻石小于设定的值则发起通知
//        if (this.getUnionBO().getUnionDiamondsAttentionAll() > diamondsValue && !this.isDiamondsAttentionAll()) {
//            this.setDiamondsAttentionAll(true);
//            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), SUnion_DiamondsNotEnough.make(this.getUnionBO().getId(), this.getUnionBO().getName(), this.getUnionBO().getUnionDiamondsAttentionAll()));
//        }
//        if (this.getUnionBO().getUnionDiamondsAttentionAll() > diamondsValue && !this.isDiamondsAttentionMinister()) {
//            this.setDiamondsAttentionMinister(true);
//            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByManager(this.getUnionBO().getId(), SUnion_DiamondsNotEnough.make(this.getUnionBO().getId(), this.getUnionBO().getName(), this.getUnionBO().getUnionDiamondsAttentionMinister()));
//            //联赛创建者的亲友圈
//            Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getUnionBO().getClubId());
//            if (Objects.nonNull(club)) {
//                ClubMgr.getInstance().getClubMemberMgr().notify2UnionMinisterByClubInClubSign(this.getUnionBO().getClubId(), SUnion_DiamondsNotEnough.make(this.getUnionBO().getClubId(), this.getUnionBO().getName(), this.getUnionBO().getUnionDiamondsAttentionMinister()));
//            }
//        }
//        //如果钻石数量超过的时候 把通知的标志设置回来
//        if (diamondsValue > this.getUnionBO().getUnionDiamondsAttentionAll() && this.isDiamondsAttentionAll()) {
//            this.setDiamondsAttentionAll(false);
//        }
//        if (diamondsValue > this.getUnionBO().getUnionDiamondsAttentionMinister() && this.isDiamondsAttentionMinister()) {
//            this.setDiamondsAttentionMinister(false);
//        }
//    }


    /**
     * 获取赛事设置信息
     *
     * @return 赛事设置信息
     */
    public UnionSetConfig getUnionSetConfigInfo() {
        return new UnionSetConfig(
                this.getUnionBO().getId(),
                this.getUnionBO().getName(),
                this.getUnionBO().getJoin(),
                this.getUnionBO().getQuit(),
                this.getUnionBO().getExpression(),
                this.getUnionBO().getStateValue(),
                this.getUnionBO().getSports(),
                this.getUnionBO().getInitSports(),
                this.getUnionBO().getMatchRateValue(),
                this.getUnionBO().getOutSports(),
                this.getUnionBO().getPrizeType(),
                this.getUnionBO().getRanking(),
                this.getUnionBO().getValue(),
                this.getUnionBO().getUnionDiamondsAttentionMinister(),
                this.getUnionBO().getUnionDiamondsAttentionAll(),
                UnionDefine.UNION_QUIT_TABLENUM.valueOf(this.getUnionBO().getTableNum()).value(),
                this.getUnionBO().getJoinClubSameUnion());
    }

    /**
     * 保存赛事设置信息
     *
     * @param name       名称
     * @param join       加入申请
     * @param quit       退出申请
     * @param expression 魔法表情
     * @param state      赛事状态
     * @param sports     竞技点清零
     */

    public void setUnionSetConfigInfo(String name, int join, int quit, int tableNum, int expression, int state, int sports, double initSports, int matchRate, double outSports, int prizeType, int ranking, int value, long exePid, int joinClubSameUnion) {
        this.getUnionBO().saveMap(name, join, quit, tableNum, expression, state, sports, initSports, matchRate, outSports, prizeType, ranking, value, exePid, joinClubSameUnion);
    }


    /**
     * 判断是否创建的空房间已超过限制
     */
    public boolean checkCanCreateRoom() {
        return this.getRoomConfigBOMap().size() < 100;
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
        // 获取亲友圈创建房间配置
        UnionCreateGameSet createGameSet = this.getRoomConfigBOMap().get(gameIndex);
        if (null == createGameSet) {
            // 找不到配置
            return SData_Result.make(ErrorCode.NotAllow, "null == createGameSet gameIndex{%d}", gameIndex);
        }
        if (Config.isShare()) {
            List<ShareRoom> roomInitList = ShareRoomMgr.getInstance().getRoomInitList(gameIndex, this.getUnionBO().getId(), RoomTypeEnum.UNION);
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
            List<RoomImpl> roomInitList = NormalRoomMgr.getInstance().getRoomInitList(gameIndex, this.getUnionBO().getId(), RoomTypeEnum.UNION);
            if (CollectionUtils.isNotEmpty(roomInitList)) {
                for (RoomImpl roomImpl : roomInitList) {
                    // 房间已经结束
                    if (roomImpl.isEndRoom()) {
                        continue;
                    }
                    if (RoomState.Init.equals(roomImpl.getRoomState())) {
                        // 房间解散
                        roomImpl.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
                    }
                }
            }
        }
        this.getLimitMap().remove(createGameSet.getbRoomConfigure().getTagId());
        // 解散房间配置
        this.getRoomConfigBOMap().remove(gameIndex);
        ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).delete(gameIndex);
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(this);
        }
        return SData_Result.make(ErrorCode.Success, this.getUnionRoomCfgItem(createGameSet));
    }

    /**
     * 亲友圈游戏配置状态修改
     *
     * @param gameIndex 索引标记
     * @param status    状态
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createGameSetChangeStopAndUse(long gameIndex, int status) {
        // 获取亲友圈创建房间配置
        UnionCreateGameSet createGameSet = this.getRoomConfigBOMap().get(gameIndex);
        if (null == createGameSet) {
            // 找不到配置
            return SData_Result.make(ErrorCode.NotAllow, "null == createGameSet gameIndex{%d}", gameIndex);
        }
        createGameSet.setStatus(status);
        //如果是禁用的话
        if (status == 1 || status == 2) {
            if (Config.isShare()) {
                List<ShareRoom> roomInitList = ShareRoomMgr.getInstance().getRoomInitList(gameIndex, this.getUnionBO().getId(), RoomTypeEnum.UNION);
                if (CollectionUtils.isNotEmpty(roomInitList)) {
                    for (ShareRoom shareRoom : roomInitList) {
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
            } else {
                // 获取当前配置的空房间
                List<RoomImpl> roomInitList = NormalRoomMgr.getInstance().getRoomInitList(gameIndex, this.getUnionBO().getId(), RoomTypeEnum.UNION);
                if (CollectionUtils.isNotEmpty(roomInitList)) {
                    for (RoomImpl roomImpl : roomInitList) {
                        // 房间已经结束
                        if (roomImpl.isEndRoom()) {
                            continue;
                        }
                        if (RoomState.Init.equals(roomImpl.getRoomState())) {
                            // 房间解散
                            roomImpl.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
                        }
                    }
                }
            }
            if (status == 2) {
                this.getLimitMap().remove(createGameSet.getbRoomConfigure().getTagId());
                // 解散房间配置
                this.getRoomConfigBOMap().remove(gameIndex);
                ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).delete(gameIndex);
                ContainerMgr.get().getComponent(ClubMemberLatylyConfigIDBOService.class).delete(Restrictions.eq("configID", gameIndex));
            }
        } else if (status == 0) {//如果是启用的话
            BaseRoomConfigure baseRoomConfigure = NormalRoomMgr.getInstance().createNoneUnionRoom(createGameSet.getbRoomConfigure(), 0);
            // 通知俱乐部玩家
            RoomInfoItem roomInfoItem = newRoomInfoItem(createGameSet.getbRoomConfigure(), baseRoomConfigure.getUnionRoomCfg().getRoomKey(), false);
            roomInfoItem.setSort(RoomSortedEnum.NONE_CONFIG.ordinal());
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), gameIndex, SUnion_RoomStatusChange.make(roomInfoItem));
            //更新数据库
            ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).saveOrUpDate(createGameSet);
        }
        //更新数据库状态
        ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).updateStatus(gameIndex, status);
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(this);
        }
        return SData_Result.make(ErrorCode.Success, this.getUnionRoomCfgItem(createGameSet));
    }

    /**
     * 获取操作状态
     *
     * @param status 状态
     * @return
     */
    private UnionDefine.UNION_OPERATION_STATUS getOperationStatus(int status) {
        UnionDefine.UNION_OPERATION_STATUS operationStatus = UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_NOMARL;
        if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DELETE.value() == status) {
            operationStatus = UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_DELETE;
        } else if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL.value() == status) {
            operationStatus = UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_RESTART;
        } else if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DISABLE.value() == status) {
            operationStatus = UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_DISABLE;
        }
        return operationStatus;
    }


    /**
     * 关闭所有房间
     *
     * @return
     */
    public boolean close() {
        // 清空房间创建配置
        this.getRoomConfigBOMap().clear();
        ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).delete(this.getUnionBO().getId());
        if (Config.isShare()) {
            List<ShareRoom> rooms = ShareRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, this.getUnionBO().getId());
            for (ShareRoom room : rooms) {
                // 房间初始状态
                if (RoomState.Init.equals(room.getRoomState())) {
                    // 房间解散
                    ShareRoomMgr.getInstance().doDissolveRoom(room);
                }
            }
        } else {
            // 亲友圈预开房列表
            List<RoomImpl> rooms = NormalRoomMgr.getInstance().roomValues(RoomTypeEnum.UNION, this.getUnionBO().getId());
            for (RoomImpl room : rooms) {
                // 房间初始状态
                if (RoomState.Init.equals(room.getRoomState())) {
                    // 房间解散
                    room.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_CLOSE.value());
                }
            }
        }
        this.getUnionBO().setDistime(CommTime.nowSecond());
        this.getUnionBO().setState(UnionDefine.UNION_STATUS.UNION_STATUS_CLOSE.value());
        this.getUnionBO().closeUnion();
        return true;
    }

    /**
     * 俱乐部房间玩家进入或者退出
     *
     * @param room 房间信息
     * @param pos  当前操作玩家位置信息
     */
    public void onUnionRoomPlayerChange(AbsBaseRoom room, long unionGameCfgId, RoomPosInfoShort pos, int sorted) {
//        if (Config.isShare()) {
//            Union union = ShareUnionListMgr.getInstance().getUnion(room.getSpecialRoomId());
//            if (union != null && union.getRoomConfigBOMap().containsKey(unionGameCfgId)) {
//                UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), unionGameCfgId, SUnion_RoomPlayerChange.make(this.getUnionBO().getId(), room.getRoomKey(), pos, sorted));
//                if (room.getRoomPosMgr().checkExistNoOne()) {
//                    // 解散房间
//                    room.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_NO_PLAYEER.value());
//                }
//            }
//        } else {
        if (this.getRoomConfigBOMap().containsKey(unionGameCfgId)) {
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), unionGameCfgId, SUnion_RoomPlayerChange.make(this.getUnionBO().getId(), room.getRoomKey(), pos, sorted));
            if (room.getRoomPosMgr().checkExistNoOne()) {
                // 解散房间
                room.doDissolveRoom(UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_NO_PLAYEER.value());
            }
        }
//        }
    }


    /**
     * 游戏开始或者房间解散新增空房间
     *
     * @param gameIndex 索引标记
     */
    public void createNewSetRoom(long gameIndex, BaseRoomConfigure baseRoomConfigure, String roomKey) {
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), gameIndex, SUnion_RoomStatusChange.make(newRoomInfoItem(baseRoomConfigure, roomKey, false)));
        UnionCreateGameSet createGameSet = this.getRoomConfigBOMap().get(gameIndex);
        if (Objects.nonNull(createGameSet)) {
            createGameSet.incrementWait();
            // 增加房间数
            createGameSet.addRoomCount();
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().addRoomCount(this.getUnionBO().getId(), gameIndex);
            }
        }
    }


    /**
     * 亲友圈房间结束移除
     *
     * @param gameIndex 亲友圈配置标识
     * @param roomKey   房间号
     */
    @SuppressWarnings("rawtypes")
    public void onUnionRoomRemove(long gameIndex, String roomKey, int sort) {
        UnionCreateGameSet createGameSet = this.getRoomConfigBOMap().get(gameIndex);
        if (null == createGameSet) {
            CommLog.info("onUnionRoomRemove null == key unionId:{},gameIndex:{},roomKey:{}", this.getUnionBO().getId(), gameIndex, roomKey);
            return;
        } else {
            createGameSet.subRoomCount();
            createGameSet.deIncrementWait();
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().subRoomCount(this.unionBO.getId(), gameIndex);
            }

        }
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setId(this.getUnionBO().getId());
        roomInfoItem.setRoomKey(roomKey);
        roomInfoItem.setPosList(null);
        roomInfoItem.setClose(true);
        roomInfoItem.setSort(sort);
        roomInfoItem.setTagId(createGameSet.getbRoomConfigure().getTagId());
        // 通知俱乐部玩家
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), gameIndex, SUnion_RoomStatusChange.make(roomInfoItem));
//        if (Config.isShare()) {
//            ShareUnionListMgr.getInstance().addUnion(this);
//        }
    }

    /**
     * 通知局数变化
     */
    public void roomSetIDChange(long unionId, long unionGameCfgId, String roomKey, int setID, int sort) {
        if (this.getRoomConfigBOMap().containsKey(unionGameCfgId)) {
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(unionId, unionGameCfgId, SUnion_RoomSetChange.make(unionId, roomKey, setID, sort));
        }
    }

    /**
     * 通知开始
     */
    public void roomStartChange(long unionId, long unionGameCfgId, String roomKey) {
        if (this.getRoomConfigBOMap().containsKey(unionGameCfgId)) {
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(unionId, unionGameCfgId, SUnion_RoomStartChange.make(unionId, roomKey, RoomState.Playing.value()));
        }
    }


    /**
     * 添加游戏设置
     *
     * @param pid           玩家PID
     * @param createGameSet 亲友圈创建房间游戏配置
     * @param isCreate      T:创建,F:修改
     */
    public void setCreateGameSet(long pid, UnionCreateGameSet createGameSet, boolean isCreate) {
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
    public boolean createSetRoom(UnionCreateGameSet createGameSet, long pid, boolean isClose, boolean isNotifyCreateChange, boolean isCreate) {
        return createSetRoom(createGameSet, createGameSet.getbRoomConfigure().getUnionRoomCfg().getRoomKey(), pid, isClose, isNotifyCreateChange, isCreate);
    }


    /**
     * 开启新房间
     **/
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean createSetRoom(UnionCreateGameSet createGameSet, String roomKey, long pid, boolean isClose, boolean isNotifyCreateChange, boolean isCreate) {
        if (Objects.isNull(createGameSet)) {
            CommLog.info("createSetRoom notify2AllByUnion");
            return false;
        }
        // 通知俱乐部玩家
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(this.getUnionBO().getId(), createGameSet.getGameIndex(), SUnion_RoomStatusChange.make(newRoomInfoItem(createGameSet.getbRoomConfigure(), roomKey, isClose)));
        // 通知修改设置
        if (isNotifyCreateChange) {
            if (isCreate) {
                // 创建房间
                UnionDynamicBO.insertUnionGameConfig(pid, getUnionBO().getClubId(), getUnionBO().getId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CREATE_ROOM.value());
            } else {
                // 修改房间
                UnionDynamicBO.insertUnionGameConfig(pid, getUnionBO().getClubId(), getUnionBO().getId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_UPDATE_ROOM.value(), createGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName());
            }
        }
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(this);
        }
        return true;
    }

    public RoomInfoItem newRoomInfoItem(BaseRoomConfigure baseRoomConfigure, String roomKey, boolean isClose) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName(baseRoomConfigure.getBaseCreateRoom().getRoomName());
        roomInfoItem.setId(this.getUnionBO().getId());
        roomInfoItem.setRoomKey(roomKey);
        roomInfoItem.setTagId(baseRoomConfigure.getTagId());
        roomInfoItem.setGameId(baseRoomConfigure.getGameType().getId());
        roomInfoItem.setSetCount(baseRoomConfigure.getBaseCreateRoom().getSetCount());
        roomInfoItem.setPlayerNum(baseRoomConfigure.getBaseCreateRoom().getPlayerNum());
        roomInfoItem.setCreateTime(0);
        roomInfoItem.setSort(RoomSortedEnum.NONE_ROOM.ordinal());
        roomInfoItem.setClose(isClose);
        roomInfoItem.setPassword(baseRoomConfigure.getBaseCreateRoom().getPassword());
        roomInfoItem.setRoomSportsThreshold(baseRoomConfigure.getBaseCreateRoom().getRoomSportsThreshold());
        return roomInfoItem;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public UnionRoomCfgItem getUnionRoomCfgItem(UnionCreateGameSet createGameSet) {
        if (Config.isShare()) {
            Map<String, ShareRoom> allRooms = ShareRoomMgr.getInstance().allOneUnionShareRooms(this.unionBO.getId());
            return new UnionRoomCfgItem(1, createGameSet.getGameIndex(), createGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName(), createGameSet.getGameType().getId(), getRoomCountBySet(allRooms, createGameSet) - 1, createGameSet.getStatus(), createGameSet.getbRoomConfigure().getTagId());
        } else {
            return new UnionRoomCfgItem(1, createGameSet.getGameIndex(), createGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName(), createGameSet.getGameType().getId(), createGameSet.getRoomCount(), createGameSet.getStatus(), createGameSet.getbRoomConfigure().getTagId());
        }
    }


    /**
     * 房间玩法列表
     * 禁用和停用功能加上的话 这边要加入停用的房间
     *
     * @param classType 类型
     * @param pageNum   第几页
     * @return
     */
    public SData_Result getUnionRoomCfgList(final List<Long> unionNotGameList, int classType, int pageNum) {
        final Map<String, ShareRoom> allRooms = new HashMap<>();
        if (Config.isShare()) {
            allRooms.putAll(ShareRoomMgr.getInstance().allOneUnionShareRooms(this.unionBO.getId()));
        }
        return SData_Result.make(ErrorCode.Success
                , this.getRoomConfigBOMap()
                        .values()
                        .stream()
                        .filter(k -> Objects.nonNull(k) && (k.getStatus() == UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL.value() || k.getStatus() == UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DISABLE.value()) && (classType <= 0 || k.getGameType().getType().value() == classType))
                        .map(k -> new UnionRoomCfgItem(isSelect(unionNotGameList, k.getGameIndex()), k.getGameIndex(), k.getbRoomConfigure().getBaseCreateRoom().getRoomName(), k.getGameType().getId(), Math.max(0, (getRoomCountBySet(allRooms, k) - 1)), k.getbRoomConfigure().getTagId()))
                        .sorted(Comparator.comparing(k -> k.getIdMax(unionNotGameList)))
                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE))
                        .limit(Page.PAGE_SIZE)
                        .collect(Collectors.toList()));
    }

    /**
     * 是否选择
     *
     * @param unionNotGameList 未选择列表
     * @param cfgId            配置Id
     * @return
     */
    private int isSelect(final List<Long> unionNotGameList, long cfgId) {
        if (CollectionUtils.isEmpty(unionNotGameList)) {
            return 1;
        }
        return unionNotGameList.contains(cfgId) ? 0 : 1;
    }

    /**
     * 获取房间数量
     *
     * @param unionCreateGameSet
     * @return
     */
    private int getRoomCountBySet(Map<String, ShareRoom> allRooms, UnionCreateGameSet unionCreateGameSet) {
        int count = 0;
        if (Config.isShare()) {
            count = ShareRoomMgr.getInstance().getOneRoomCfgCount(allRooms, this.getUnionBO().getId(), unionCreateGameSet.getGameIndex());
        } else {
            count = unionCreateGameSet.getRoomCount();
        }
        return count;
    }

    /**
     * 房间玩法统计
     *
     * @param classType 类型
     * @return
     */
    public SData_Result getUnionRoomCfgCount(int classType) {
        UnionRoomCfgCount unionRoomCfgCount = new UnionRoomCfgCount();
        RoomCfgCount roomCfgCount = null;
        if (Config.isShare()) {
            roomCfgCount = ShareRoomMgr.getInstance().getRoomCfgCount(RoomTypeEnum.UNION, ClassType.valueOf(classType), this.getUnionBO().getId());
        } else {
            roomCfgCount = RoomMgr.getInstance().getRoomCfgCount(RoomTypeEnum.UNION, ClassType.valueOf(classType), this.getUnionBO().getId());
        }
        unionRoomCfgCount.setRoomCount(roomCfgCount.getRoomCount().get());
        unionRoomCfgCount.setPlayerCount(roomCfgCount.getPlayerCount().get());
        unionRoomCfgCount.setSort(getUnionBO().getSort());
        return SData_Result.make(ErrorCode.Success, unionRoomCfgCount);
    }

    /**
     * 获取指定房间配置信息
     *
     * @param unionRoomCfgId 房间配置Id
     * @return
     */
    public SData_Result getUnionRoomCfgInfo(long unionRoomCfgId) {
        UnionCreateGameSet unionCreateGameSet = this.getRoomConfigBOMap().get(unionRoomCfgId);
        if (null == unionCreateGameSet) {
            return SData_Result.make(ErrorCode.NotAllow, "");
        }
        BaseCreateRoom baseCreateRoom = unionCreateGameSet.getbRoomConfigure().getBaseCreateRoom().deepClone();
        baseCreateRoom.setPassword(EncryptUtils.decryptDES(baseCreateRoom.getPassword()));
        return SData_Result.make(ErrorCode.Success, new UnionCreateGameSetInfo(unionCreateGameSet.getGameIndex(), new Gson().fromJson(unionCreateGameSet.getbRoomConfigure().getShareBaseCreateRoom(), Map.class), unionCreateGameSet.getStatus(), unionCreateGameSet.getGameType().getId()));
    }


    /**
     * 获取指定亲友圈分组成员列表
     *
     * @param groupingID
     */
    public List<UnionGroupingMemberInfo> getUnionGroupingMemberList(long groupingID) {
        // 获取指定分组
        UnionGroupingBO result = this.getUnionGroupingBOList().stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return Collections.emptyList();
        }
        return result.getGroupingToList().stream().map(k -> {
            Player player = PlayerMgr.getInstance().getPlayer(k);
            if (Objects.nonNull(player)) {
                return new UnionGroupingMemberInfo(groupingID, player.getShortPlayer());
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
        if (CollectionUtils.isEmpty(this.getUnionGroupingBOList())) {
            return null;
        }
        pidList.remove(pid);
        // 获取禁止的分组信息
        List<List<Long>> groupingBOList = this.getUnionGroupingBOList().stream()
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
        CommLog.info("union name pid :{},pidList :{}", pid, pidList);
        return name;
    }

    /**
     * 将玩家添加到指定组中。
     *
     * @param groupingID 分组ID
     * @param pid        玩家ID
     * @return
     */
    public SData_Result addUnionGroupingPid(long groupingID, long pid) {
        // 检查亲友圈分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getUnionGroupingBOList())) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_GROUPING, "UNION_NOT_EXIST_GROUPING");
        }
        UnionGroupingBO result = this.getUnionGroupingBOList().stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return SData_Result.make(ErrorCode.UNION_GROUPING_ID_ERROR, "UNION_GROUPING_ID_ERROR");
        }
        // 玩家加入该分组。
        if (result.addGrouping(pid)) {
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().updateField(this, "unionGroupingBOList");
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
    public SData_Result removeUnionGroupingPid(long groupingID, long pid) {
        // 检查亲友圈分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getUnionGroupingBOList())) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_GROUPING, "UNION_NOT_EXIST_GROUPING");

        }
        UnionGroupingBO result = this.getUnionGroupingBOList().stream()
                .filter(x -> groupingID == x.getId())
                .findAny()
                .orElse(null);
        if (Objects.isNull(result)) {
            return SData_Result.make(ErrorCode.UNION_GROUPING_ID_ERROR, "UNION_GROUPING_ID_ERROR");
        }
        // 玩家移出该分组。
        if (result.removeGrouping(pid)) {
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().updateField(this, "unionGroupingBOList");
            }
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.UNION_GROUPING_PID_NOT_EXIST, "UNION_GROUPING_PID_NOT_EXIST");
        }
    }


    /**
     * 增加分组
     */
    public UnionGroupingInfo addUnionGrouping() {
        if (Objects.isNull(this.getUnionGroupingBOList())) {
            this.setUnionGroupingBOList(Lists.newArrayList());
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().updateField(this, "unionGroupingBOList");
            }
            return null;
        }
        UnionGroupingBO cGroupingBO = new UnionGroupingBO();
        cGroupingBO.setUnionId(this.getUnionBO().getId());
        cGroupingBO.setCreateTime(CommTime.nowSecond());
        cGroupingBO.setGrouping("");
        cGroupingBO.getBaseService().save(cGroupingBO);
        this.getUnionGroupingBOList().add(cGroupingBO);
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateField(this, "unionGroupingBOList");
        }
        return new UnionGroupingInfo(cGroupingBO.getId());
    }

    /**
     * 移除分组
     */
    public UnionGroupingInfo removeUnionGrouping(long clubGroupingId) {
        if (CollectionUtils.isEmpty(this.getUnionGroupingBOList())) {
            return null;
        }
        for (int i = 0, size = this.getUnionGroupingBOList().size(); i < size; i++) {
            if (this.getUnionGroupingBOList().get(i).getId() == clubGroupingId) {
                // 移除分组并删除。
                this.getUnionGroupingBOList().remove(i).del();
                if (Config.isShare()) {
                    ShareUnionListMgr.getInstance().updateField(this, "unionGroupingBOList");
                }
                return new UnionGroupingInfo(clubGroupingId);
            }
        }
        return null;
    }


    /**
     * 获取玩家分组列表
     *
     * @return
     */
    public List<UnionGroupingInfo> getUnionGroupingInfoList(CUnion_GroupParam req) {
        // 分组列表
        // 检查分组数据是否存在。
        if (CollectionUtils.isEmpty(this.getUnionGroupingBOList())) {
            // 默认添加一个空限制组
            this.addUnionGrouping();
        }
        return this.getUnionGroupingBOList().stream().filter(k -> Objects.nonNull(k)).sorted(Comparator.comparing(UnionGroupingBO::getId)).map(k -> {
            return new UnionGroupingInfo(k.getId(), k.getGroupingToList().size(), k.getGroupingToList().stream().filter(pid -> Objects.nonNull(pid) && pid > 0L).limit(2).map(pid -> {
                Player player = PlayerMgr.getInstance().getPlayer(pid);
                if (Objects.nonNull(player)) {

                    return player.getShortPlayer();
                }
                CommLog.error("getUnionGroupingInfoList Pid:{}", pid);
                return null;
            }).filter(shortPlayer -> Objects.nonNull(shortPlayer)).collect(Collectors.toList()));
        }).filter(k -> {
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
        }).collect(Collectors.toList());
    }


    /**
     * 执行赛事比赛排名
     */
    public boolean execUnionMatchRanking() {
        if (getUnionBO().getState() >= UnionDefine.UNION_STATE.UNION_STATE_STOP.ordinal()) {
            // 停赛不用检查
            return false;
        }
        if (CommTime.MinutesBetween(getUnionBO().getEndRoundTime(), CommTime.nowSecond()) > 5) {
            // 如果距离回合结束时间相差5分钟，则不理会。
            return false;
        }
        // 获取亲友圈列表
        List<Long> clubIdList;
        if (Config.isShare()) {
            clubIdList = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(getUnionBO().getId()).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == getUnionBO().getId() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        } else {
            clubIdList = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == getUnionBO().getId() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(clubIdList)) {
            return false;
        }
        // 消耗类型
        PrizeType prizeType = PrizeType.valueOf(getUnionBO().getPrizeType());
        // 排名
        int ranking = getUnionBO().getRanking();
        // 奖励值
        int value = getUnionBO().getValue();
        // 回合id
        int roundId = getUnionBO().getRoundId();
        // 加入队列中
        UnionMatchFactory.getInstance().publish(new UnionMatchLogItem(clubIdList, getUnionBO().getOwnerId(), prizeType, ranking, value, roundId, getUnionBO().getId(), getUnionBO().getClubId()));
        // 赛事总人数
        int count = ClubMgr.getInstance().getClubMemberMgr().clubIdListToCount(clubIdList, getUnionBO().getOwnerId(), getUnionBO().getClubId());
        // 排名奖励回退
        this.rankingRewardRollback(count, ranking, prizeType, value);
        // 总奖励值
        int sum = ranking * value;
        // 设置空奖励
        boolean isNonePrize = PrizeType.None.equals(prizeType) || sum <= 0;
        if (isNonePrize || getOwnerPlayer().getFeature(PlayerCurrency.class).checkAndConsumeItemFlow(prizeType, sum, ItemFlow.UNION_MATCH_REWARD)) {
            // 更新本轮的开始、结束时间
            getUnionBO().saveRoundTimeAndRoundId();
            CommLog.info("execUnionMatchRanking UnionId:{},prizeType:{},ranking:{},value:{},sum:{}", getUnionBO().getId(), prizeType, ranking, value, sum);
        } else {
            // 每一轮检查并消耗主裁判设置的奖励，奖励所需的道具不足，无法开启新一轮的赛事，请前往设置
            // 赛事状态切换为 停用
            getUnionBO().saveNotEnoughRewardStopState();
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(getUnionBO().getId(), SUnion_StateChange.make(getUnionBO().getId(), UnionDefine.UNION_STATE.UNION_STATE_NOT_ENOUGH_REWARD.ordinal()));
        }
        return true;
    }


    /**
     * 执行赛事比赛排名
     */
    public boolean execUnionStateStop(CUnion_SetConfig setConfig) {
        // 获取亲友圈列表
        List<Long> clubIdList;
        if (Config.isShare()) {
            clubIdList = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(getUnionBO().getId()).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == getUnionBO().getId() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        } else {
            clubIdList = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == getUnionBO().getId() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(clubIdList)) {
            // 消耗类型
            PrizeType prizeType = PrizeType.valueOf(setConfig.getPrizeType());
            // 排名
            int ranking = setConfig.getRanking();
            // 奖励值
            int value = setConfig.getValue();
            // 回合id
            int roundId = getUnionBO().getRoundId();
            // 加入队列中
            UnionMatchFactory.getInstance().publish(new UnionMatchLogItem(clubIdList, getUnionBO().getOwnerId(), prizeType, ranking, value, roundId, getUnionBO().getId(), getUnionBO().getClubId()));
            // 赛事总人数
            int count = ClubMgr.getInstance().getClubMemberMgr().clubIdListToCount(clubIdList, getUnionBO().getOwnerId(), getUnionBO().getClubId());
            // 排名奖励回退
            this.rankingRewardRollback(count, ranking, prizeType, value);
        }
        // 手动操作停止
        getUnionBO().saveStopState();
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(getUnionBO().getId(), SUnion_StateChange.make(getUnionBO().getId(), UnionDefine.UNION_STATE.UNION_STATE_STOP.ordinal()));
        return true;
    }

    /**
     * 排名奖励回退
     *
     * @param count     赛事总人数
     * @param ranking   排名
     * @param prizeType 消耗类型
     * @param value     奖励值
     */
    private void rankingRewardRollback(int count, int ranking, PrizeType prizeType, int value) {
        if (count >= ranking) {
            // 参数人数 >= 设置奖励人数
            return;
        }
        // 旧的总奖励值
        int oldSum = ranking * value;
        // 新的总奖励值
        int newSum = count * value;
        // 当前需要回退的值
        int curSum = oldSum - newSum;
        getOwnerPlayer().getFeature(PlayerCurrency.class).gainItemFlow(prizeType, curSum, ItemFlow.UNION_MATCH_REWARD, ConstEnum.ResOpType.Fallback);
    }

    /**
     * 房间配置列表
     *
     * @param pageNum 第几页
     * @return
     */
    public Map<Long, ClubRoomConfigCalcActiveItem> getRoomConfigList(int pageNum) {
        return this.getRoomConfigBOMap()
                .values()
                .stream()
                .filter(k -> UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL.value() == k.getStatus())
                .map(k -> {
                    if (Objects.isNull(k.getGameType())) {
                        return null;
                    }
                    if (Objects.isNull(k.getbRoomConfigure())) {
                        return null;
                    }
                    if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                        return null;
                    }
                    return new ClubRoomConfigCalcActiveItem(k.getGameIndex(), k.getbRoomConfigure().getBaseCreateRoom().getRoomName(), k.getbRoomConfigure().getBaseCreateRoom().getPlayerNum());
                }).filter(k -> Objects.nonNull(k)).
//                        skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).
        collect(Collectors.toMap(k -> k.getConfigId(), k -> k, (k1, k2) -> k1));
    }


    public List<UnionRoomConfigItem> getUnionRoomConfigItemList(ClubMember clubMember, Integer gameType) {
        List<ClubMemberLatelyConfigIDItem> items = ContainerMgr.get().getComponent(ClubMemberLatylyConfigIDBOService.class).getDefaultDao().findAll(Restrictions.and(Restrictions.eq("unionID", this.getUnionBO().getId()), Restrictions.eq("clubID", clubMember.getClubID()), Restrictions.eq("memberID", clubMember.getId())), ClubMemberLatelyConfigIDItem.class, ClubMemberLatelyConfigIDItem.getItemsNameUid());
        List<Long> latelyConfigList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            latelyConfigList = items.stream().sorted(Comparator.comparing(ClubMemberLatelyConfigIDItem::getStartTime)).map(k -> k.getConfigID()).collect(Collectors.toList());
        }
        List<Long> latelyConfigListFinal = new ArrayList<>(latelyConfigList);
        if (Config.isShare()) {
            Union union = ShareUnionListMgr.getInstance().getUnion(this.getUnionBO().getId());
            Map<Long, String> roomKeyMap = ShareRoomMgr.getInstance().getRoomKeyMapByConfigIds(latelyConfigList, this.getUnionBO().getId(), RoomTypeEnum.UNION);
            return union.getRoomConfigBOMap().values().stream().map(k -> {
                if (Objects.isNull(k.getbRoomConfigure())) {
                    return null;
                }
                if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                    return null;
                }
                if (null != gameType && k.getbRoomConfigure().getGameType().getId() != gameType) {
                    return null;
                }
                BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
                return new UnionRoomConfigItem(baseCreateRoom.getGameIndex(), !roomKeyMap.isEmpty() ? roomKeyMap.get(baseCreateRoom.getGameIndex()) : k.getRoomKey(), baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), baseCreateRoom.getSetCount(), latelyConfigListFinal.indexOf(baseCreateRoom.getGameIndex()), k.getGameType().getId(), k.getbRoomConfigure().getTagId(), baseCreateRoom.getPassword());
            }).sorted(Comparator.comparing(UnionRoomConfigItem::getTab).reversed()).collect(Collectors.toList());
        } else {
            return this.getRoomConfigBOMap().values().stream().map(k -> {
                if (Objects.isNull(k.getbRoomConfigure())) {
                    return null;
                }
                if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                    return null;
                }
                BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
                return new UnionRoomConfigItem(baseCreateRoom.getGameIndex(), k.getRoomKey(), baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), baseCreateRoom.getSetCount(), latelyConfigListFinal.indexOf(baseCreateRoom.getGameIndex()), k.getGameType().getId(), k.getbRoomConfigure().getTagId(), baseCreateRoom.getPassword());
            }).sorted(Comparator.comparing(UnionRoomConfigItem::getTab).reversed()).collect(Collectors.toList());
        }
    }

    /**
     * 中至的 需要添加竞技点倍数
     *
     * @param clubMember
     * @return
     */
    public List<UnionRoomConfigItem> getUnionRoomConfigItemListZhongZhi(ClubMember clubMember) {
        List<ClubMemberLatelyConfigIDItem> items = ContainerMgr.get().getComponent(ClubMemberLatylyConfigIDBOService.class).getDefaultDao().findAll(Restrictions.and(Restrictions.eq("unionID", this.getUnionBO().getId()), Restrictions.eq("clubID", clubMember.getClubID()), Restrictions.eq("memberID", clubMember.getId())), ClubMemberLatelyConfigIDItem.class, ClubMemberLatelyConfigIDItem.getItemsNameUid());
        List<Long> latelyConfigList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            latelyConfigList = items.stream().sorted(Comparator.comparing(ClubMemberLatelyConfigIDItem::getStartTime)).map(k -> k.getConfigID()).collect(Collectors.toList());
        }
        List<Long> latelyConfigListFinal = new ArrayList<>(latelyConfigList);
        if (Config.isShare()) {
            Union union = ShareUnionListMgr.getInstance().getUnion(this.getUnionBO().getId());
            Map<Long, String> roomKeyMap = ShareRoomMgr.getInstance().getRoomKeyMapByConfigIds(latelyConfigList, this.getUnionBO().getId(), RoomTypeEnum.UNION);
            return union.getRoomConfigBOMap().values().stream().map(k -> {
                if (Objects.isNull(k.getbRoomConfigure())) {
                    return null;
                }
                if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                    return null;
                }
                BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
                return new UnionRoomConfigItem(baseCreateRoom.getGameIndex(), !roomKeyMap.isEmpty() ? roomKeyMap.get(baseCreateRoom.getGameIndex()) : k.getRoomKey(), baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), baseCreateRoom.getSetCount(), latelyConfigListFinal.indexOf(baseCreateRoom.getGameIndex()), k.getGameType().getId(), k.getbRoomConfigure().getTagId(), baseCreateRoom.getPassword(), baseCreateRoom.getSportsDouble());
            }).sorted(Comparator.comparing(UnionRoomConfigItem::getTab).reversed()).collect(Collectors.toList());
        } else {
            return this.getRoomConfigBOMap().values().stream().map(k -> {
                if (Objects.isNull(k.getbRoomConfigure())) {
                    return null;
                }
                if (Objects.isNull(k.getbRoomConfigure().getBaseCreateRoom())) {
                    return null;
                }
                BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
                return new UnionRoomConfigItem(baseCreateRoom.getGameIndex(), k.getRoomKey(), baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), baseCreateRoom.getSetCount(), latelyConfigListFinal.indexOf(baseCreateRoom.getGameIndex()), k.getGameType().getId(), k.getbRoomConfigure().getTagId(), baseCreateRoom.getPassword(), baseCreateRoom.getSportsDouble());
            }).sorted(Comparator.comparing(UnionRoomConfigItem::getTab).reversed()).collect(Collectors.toList());
        }
    }

    /**
     * 检查并创建新房间
     */
    public void checkCreateNewSetRoom() {
        List<Long> configIds = this.getRoomConfigBOMap().keySet().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(configIds)) {
            // 没有联赛配置
            return;
        }
        // 需要创建房间的房间配置ID
        List<Long> notExistIdList;
        if (Config.isShare()) {
            notExistIdList = ShareRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(configIds, this.getUnionBO().getId(), RoomTypeEnum.UNION);
        } else {
            notExistIdList = NormalRoomMgr.getInstance().getSpecifiedConfigurationRoomKey(configIds, this.getUnionBO().getId(), RoomTypeEnum.UNION);
        }
        if (CollectionUtils.isEmpty(notExistIdList)) {
            // 没有需要创建的房间配置
            return;
        }
        // 创建新房间
        notExistIdList.stream().forEach(k -> {
            UnionCreateGameSet unionCreateGameSet = getRoomConfigBOMap().get(k);
            if (Objects.nonNull(unionCreateGameSet)) {
                // 设置标识Id
                unionCreateGameSet.setTagId(getCurTabId(k));
                // 增加房间数
                unionCreateGameSet.incrementWait();
                // 增加房间数
                unionCreateGameSet.addRoomCount();
                // 创建时间
                unionCreateGameSet.setCreateTime(CommTime.nowSecond());
                // 创建一个空赛事房间
                unionCreateGameSet.setRoomConfigure(NormalRoomMgr.getInstance().createNoneUnionRoom(unionCreateGameSet.getbRoomConfigure(), 0));
            }
        });
    }

    /**
     * 初始化联盟区间分成
     */
    public void initUnionShareSection() {
        int createTime = CommTime.nowSecond();
        for (UnionDefine.UNION_SHARE_SECTION con : UnionDefine.UNION_SHARE_SECTION.values()) {
            UnionShareSectionBO unionShareSectionBO = new UnionShareSectionBO();
            unionShareSectionBO.setClubId(this.getUnionBO().getClubId());
            unionShareSectionBO.setCreateTime(createTime);
            unionShareSectionBO.setBeginValue(con.beginValue());
            unionShareSectionBO.setEndValue(con.endValue());
            unionShareSectionBO.setUnionId(this.getUnionBO().getId());
            unionShareSectionBO.setEndFlag(con.endValue() == con.beginValue() ? 1 : 0);
            unionShareSectionBO.getBaseService().saveIgnoreOrUpDate(unionShareSectionBO);
        }
    }

    /**
     * 判断是不是中至联赛
     *
     * @return
     */
    public boolean isZhongZhiUnion() {
        return this.getUnionBO().getUnionType() == UnionDefine.UNION_TYPE.ZhongZhi.value();
    }
}
