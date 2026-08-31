package business.global.room.base;

import BaseCommon.CommLog;
import business.player.Player;
import business.player.Robot.RobotMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerClub;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqPlayerPushProtoBo;
import business.rocketmq.constant.MqTopic;
import cenum.ItemFlow;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.CKickOutType;
import cenum.room.GaoJiTypeEnum;
import cenum.room.PaymentRoomCardType;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Lists;
import core.db.entity.clarkGame.ClubMemberBO;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.room.RoomPromotionActiveEvent;
import core.dispatch.event.room.RoomPromotionShare;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.LocationInfo;
import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.club.ClubRoomPromotionItem;
import jsproto.c2s.cclass.pos.PlayerPosInfo;
import jsproto.c2s.cclass.pos.RoomPlayerPos;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.room.RoomPosInfoShort;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.room.SRoom_SportsPointChange;
import jsproto.c2s.iclass.room._StartVoteSameIpDissolve;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间内每个位置信息 管理器
 */
@Data
public abstract class AbsRoomPosMgr {
    // 房间信息
    public AbsBaseRoom room = null;
    // 玩家信息列表
    public List<AbsRoomPos> posList = Collections.synchronizedList(new ArrayList<>());

    public List<AbsRoomPos> watchList = Collections.synchronizedList(new ArrayList<>());
    public List<AbsRoomPos> allList = Collections.synchronizedList(new ArrayList<>());

    // 玩家数
    private int playerNum;
    //所有人准备的时间
    private int allEnterTime = 0;
    /**
     * 是否需要插入统计表
     */
    private boolean isCalcPoint;

    public AbsRoomPosMgr(AbsBaseRoom room) {
        this.room = room;
        this.playerNum = room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum();
        this.initPosList();
    }

    /**
     * 初始位置列表
     */
    protected abstract void initPosList();

    public AbsRoomPos addWatch() {
        AbsRoomPos newWatch = new AbsRoomPos(-1, this.getRoom());
        this.watchList.add(newWatch);
        return newWatch;
    }

    protected AbsRoomPos addAll() {
        AbsRoomPos newAll = new AbsRoomPos(-1, this.getRoom());
        this.allList.add(newAll);
        return newAll;
    }

    /**
     * 清除房间玩家管理
     */
    public void clear() {
        if (null != this.getPosList()) {
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key)) {
                    key.roomPosClear();
                }
            });
            this.getPosList().clear();
            this.setPosList(null);
        }
        this.setRoom(null);
    }

    /**
     * 检查进入房间高级选项
     *
     * @param locationInfo 房间信息
     * @param ip
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkEnterRoomGaoJi(LocationInfo locationInfo, String ip) {
        // 检查同位置
        SData_Result result = checkApartLocation(locationInfo);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), "您和房间内的玩家距离过近,禁止进入本房间");
        }
        // 检查同IP
        if (checkSameIp(ip)) {
            return SData_Result.make(ErrorCode.EXIST_SAME_IP, "IP相同,禁止进入本房间");
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 检查是否出现同IP
     *
     * @param ip Ip
     */
    public boolean checkSameIp(String ip) {
        if (!this.getRoom().checkGaoJiXuanXiang(GaoJiTypeEnum.SAME_IP)) {
            // 未勾选同IP选项
            return false;
        }
        if (StringUtils.isEmpty(ip)) {
            // TODO 2018/11/12-15:53 傅哥说IP为空可以创建房间、进入游戏。
            return false;
        }
        if (this.getPosList().stream().anyMatch(k -> ip.equals(k.getPlayerIP()))) {
            return true;
        }
        Map<String, Long> groupingBy = this.getPosList().stream().filter(k -> Objects.nonNull(k.getPlayer())).map(k -> k.getPlayerIP()).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (MapUtils.isEmpty(groupingBy)) {
            return false;
        }
        return groupingBy.entrySet().stream().anyMatch(k -> k.getValue() > 1L);
    }

    /**
     * 检查是否出现同位置
     *
     * @param locationInfo 定位信息
     * @return
     */
    public SData_Result checkApartLocation(LocationInfo locationInfo) {
        if (!(this.getRoom().checkGaoJiXuanXiang(GaoJiTypeEnum.LOCATION) || this.getRoom().checkGaoJiXuanXiang(GaoJiTypeEnum.LOCATION_200))) {
            // 未勾选同位置选项
            return SData_Result.make(ErrorCode.Success);
        }
        double value = 100D;
        if (this.getRoom().checkGaoJiXuanXiang(GaoJiTypeEnum.LOCATION_200)) {
            value = 200D;
        }
        // 超时时间
        final int overTime = 60;
        if (Objects.isNull(locationInfo) || locationInfo.isGetError() || CommTime.minTimeDifference(locationInfo.getUpdateTime(), CommTime.nowSecond()) >= overTime) {
            // 定位信息不存在
            CommLog.info("locationInfo error RoomId:{},RoomKey:{},locationInfo:{}", getRoom().getRoomID(), getRoom().getRoomKey(), Objects.nonNull(locationInfo) ? locationInfo.toString() : "error");
            return SData_Result.make(ErrorCode.POSITIONING_NOT_ON);
        }
        for (int i = 0; i < this.getPlayerNum(); i++) {
            AbsRoomPos roomPos = getPosList().get(i);
            if (Objects.nonNull(roomPos) && roomPos.getPid() > 0L) {
                LocationInfo otherLocationInfo = roomPos.getLocationInfo();
                if (Objects.nonNull(roomPos.getLocationInfo())) {
                    // 检查经纬度是否正确或者定位的更新时间是否超过1个小时
                    if (otherLocationInfo.getLatitude() == 0 || otherLocationInfo.getLongitude() == 0 || CommTime.minTimeDifference(otherLocationInfo.getUpdateTime(), CommTime.nowSecond()) >= overTime) {
                        // TODO 2019/09/03 傅哥说：不用检查房间内的玩家是否定位失败，(此处有雷。。。有问题找傅哥说。)
                        CommLog.info("otherLocationInfo error RoomId:{},RoomKey:{},Pid:{},otherLocationInfo:{}", getRoom().getRoomID(), getRoom().getRoomKey(), roomPos.getPid(), otherLocationInfo.toString());
                        continue;
                    }
                    // 检查进入房间内玩家的信息是否和房间内的玩家相距 >= 100
                    double dis = CommMath.getDistance(locationInfo.getLatitude(), locationInfo.getLongitude(), otherLocationInfo.getLatitude(), otherLocationInfo.getLongitude());
                    if (dis < value) {
                        // 定位距离过近
                        CommLog.info("otherLocationInfo error dis:{},RoomId:{},RoomKey:{},Pid:{},otherLocationInfo:{}", dis, getRoom().getRoomID(), getRoom().getRoomKey(), roomPos.getPid(), otherLocationInfo.toString());
                        return SData_Result.make(ErrorCode.APART_LOCATION);
                    }
                } else {
                    CommLog.info("otherLocationInfo error RoomId:{},RoomKey:{},Pid:{},otherLocationInfo:{}", getRoom().getRoomID(), getRoom().getRoomKey(), roomPos.getPid(), "error");
                    // 找不到玩家的定位信息
                    return SData_Result.make(ErrorCode.POSITIONING_NOT_ON);
                }
            }
        }
        // 没有出现定位异常
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 通知存在相互Ip的玩家
     */
    public void notify2ExistSameIp() {
        // 重新刷新所有玩家身上的ip数据，并返回是否有
        this.notify2ExistSameIp(this.getPosList().stream().filter(k -> Objects.nonNull(k.getPlayer())).map(k -> k.setPlayerIP(k.getPlayer().getIp())).collect(Collectors.toSet()));
    }

    /**
     * 通知存在相互Ip的玩家
     *
     * @param booleanList
     */
    public void notify2ExistSameIp(Set<Boolean> booleanList) {
        if (this.getRoom().checkGaoJiXuanXiang(GaoJiTypeEnum.SAME_IP)) {
            // 检查是否有玩家身上的ip被重新设置
            boolean existResetIp = booleanList.contains(Boolean.TRUE);
            if (existResetIp) {
                // 根据 ip 分组
                Map<String, List<AbsRoomPos>> menuMap = this.getPosList().stream().filter(k -> Objects.nonNull(k.getPlayer())).collect(Collectors.toMap(AbsRoomPos::getPlayerIP, menuObj -> Lists.newArrayList(menuObj), (List<AbsRoomPos> newValueList, List<AbsRoomPos> oldValueList) -> {
                    oldValueList.addAll(newValueList);
                    return oldValueList;
                }));
                // 存在相同ip的玩家列表
                List<Long> sameIpPidList = menuMap.values().stream().filter(k -> k.size() > 1).flatMap(k -> k.stream()).map(k -> k.getPid()).collect(Collectors.toList());
                if (sameIpPidList.size() >= 1) {
                    this.getRoom().setSameIpDissolveRoom(this.getRoom().initSameIpDissolveRoom(sameIpPidList, 0));
                    // 房间中存在相同ip的玩家
                    this.notify2All(_StartVoteSameIpDissolve.make(this.getRoom().getRoomID(), sameIpPidList, this.getRoom().getSameIpDissolveRoom().getEndSec(), this.getRoom().getBaseRoomConfigure().getGameType().getName()));
                    Map<Long, String> sameIpPidToIpMap = this.getPosList().stream().filter(k -> Objects.nonNull(k.getPlayer())).collect(Collectors.toMap(AbsRoomPos::getPid, AbsRoomPos::getPlayerIP, (p1, p2) -> p1));
                    CommLog.error("notify2ExistSameIp roomId:{},roomKey:{},sameIpPidList:{},sameIpPidToIpMap:{}", this.getRoom().getRoomID(), this.getRoom().getRoomKey(), sameIpPidList, sameIpPidToIpMap.toString());
                }
            }
        }
    }

    /**
     * 获取玩家信息
     *
     * @return
     */
    public List<Player> getPlayer() {
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L && Objects.nonNull(k.getPlayer()))
                .sorted(Comparator.comparing(AbsRoomPos::getPosID)).map(k -> k.getPlayer())
                .collect(Collectors.toList());
    }


    /**
     * 获取位置信息
     *
     * @return
     */
    public List<AbsRoomPos> getRoomPosList() {
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L && Objects.nonNull(k.getPlayer()))
                .sorted(Comparator.comparing(AbsRoomPos::getPosID))
                .collect(Collectors.toList());
    }

    /**
     * 检查用户超时
     */
    public void checkOverTime(int ServerTime) {
        if (ServerTime == 0) {
            return;
        }
        for (AbsRoomPos pos : this.getPosList()) {
            if (Objects.isNull(pos) || pos.getPid() <= 0L) {
                continue;
            }
            if (pos.getLatelyOutCardTime() <= 0) {
                continue;
            }
            if (pos.isTrusteeship()) {
                continue;
            }

            if (pos.isRobot() && CommTime.nowMS() > pos.getLatelyOutCardTime() + RobotMgr.getInstance().getThinkTime()) {
                this.getRoom().RobotDeal(pos.getPosID());
                continue;
            }
            if (CommTime.nowMS() > pos.getLatelyOutCardTime() + ServerTime) {
                pos.setLatelyOutCardTime(CommTime.nowMS());
                if (Objects.nonNull(this.getRoom())) {
                    // 启动定时器
                    this.getRoom().startTrusteeShipTime();
                }
                pos.setTrusteeship(true, false);
                if (Objects.nonNull(this.getRoom())) {
                    if (room.needAtOnceOpCard()) {
                        room.roomTrusteeship(pos.getPosID());
                    }
                }

            }
        }
    }

    private void dd() {

    }

    /**
     * 清空除自己的所有人状态
     *
     * @param waitOpPos
     */
    public void clearPosLatelyOutCardTime(int waitOpPos) {
        for (AbsRoomPos pos : this.getPosList()) {
            if (pos.getPosID() == waitOpPos) {
                pos.setLatelyOutCardTime(CommTime.nowMS());
                continue;
            }
            pos.setLatelyOutCardTime(0);
        }
    }

    /**
     * 添加玩家个人游戏记录
     */
    public void insertPlayerRoomAloneBO() {
        this.getPosList().stream().filter(k -> Objects.nonNull(k) && (this.getRoom().getCurSetID() < 1 || k.isPlayTheGame())).forEach(k -> k.insertPlayerRoomAloneBO());
    }

    /**
     * 保存每小局分数
     */
    public void savaPlayerRoomAlonePoint(int endTime) {
        this.getPosList().stream().filter(k -> Objects.nonNull(k) && (this.getRoom().getCurSetID() < 1 || k.isPlayTheGame())).forEach(k -> k.savaPlayerRoomAlonePoint(endTime));
    }

    /**
     * 更新玩家个人游戏记录
     *
     * @param consumeValue 消耗值
     * @param dateTime     日期时间
     */
    public void updatePlayerRoomAloneBO(int consumeValue, int dateTime, int endTime, double roomSportsPointConsume) {
        // 获取大赢家列表
        List<Long> winnerPayList = this.winnerPayList();
        // 更新玩家个人游戏记录
        this.getPosList().stream().filter(k -> Objects.nonNull(k) && (this.getRoom().getCurSetID() < 1 || k.isPlayTheGame())).forEach(k -> k.updatePlayerRoomAloneBO(consumeValue, winnerPayList.size(), winnerPayList.contains(k.getPid()), dateTime, endTime, roomSportsPointConsume));
    }

    /**
     * 获取房间里面所有人的定位信息
     *
     * @return
     */
    public List<LocationInfo> getLocationInfoList() {
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L && Objects.nonNull(k.getPlayer()))
                .map(k -> k.getLocationInfo()).collect(Collectors.toList());
    }


    /**
     * 获取非房主付的总消耗
     *
     * @return
     */
    public int notHomeOwerPaySumConsume() {
        if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.value() == this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.isPlayTheGame() && Objects.nonNull(k.getPlayer())).map(k -> k.getPlayer().getRoomInfo().getConsumeCard()).findAny().orElse(0);
        }
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.isPlayTheGame() && Objects.nonNull(k.getPlayer())).map(k -> k.getPlayer().getRoomInfo().getConsumeCard()).reduce(0, Integer::sum);
    }

//    /**
//     * 获取房间内的竞技点消耗
//     */
//    public double getRoomSportsPointConsume(BaseCreateRoom baseCreateRoom) {
//        if (!RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum()) || this.getRoom().getHistorySetSize() < 1) {
//            // 不是赛事房间
//            return -1D;
//        }
//        if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
//            // 大赢家消耗
//            return this.bigWinnerConsume(baseCreateRoom,CommMath.mul(maxPoint(), Math.max(0, this.getRoom().getRoomTyepImpl().getSportsDouble())));
//        } else {
//            // 房间竞技点每人消耗
//            return CommMath.mul(this.getRoom().getPlayingCount(), baseCreateRoom.getRoomSportsEveryoneConsume());
//        }
//    }
//
//
//    /**
//     * 大赢家消耗
//     * @param baseCreateRoom 创建房间配置
//     * @param sportsPoint 赢的比赛分
//     * @return
//     */
//    private double bigWinnerConsume (BaseCreateRoom baseCreateRoom,double sportsPoint)  {
//        List<UnionRoomSportsBigWinnerConsumeItem> bigWinnerConsumeList = baseCreateRoom.getBigWinnerConsumeList();
//        if (CollectionUtils.isEmpty(bigWinnerConsumeList)) {
//            // 没有区间列表，就直接用设置的固定值
//            if (sportsPoint >= CommMath.FormatDouble(baseCreateRoom.getGeWinnerPoint())) {
//                // 房间竞技点大赢家消耗
//                return CommMath.FormatDouble(baseCreateRoom.getRoomSportsBigWinnerConsume());
//            }
//        } else {
//            bigWinnerConsumeList = bigWinnerConsumeList.stream().sorted(Comparator.comparing(UnionRoomSportsBigWinnerConsumeItem::getWinScore).reversed()).limit(10).collect(Collectors.toList());
//            UnionRoomSportsBigWinnerConsumeItem minItem = bigWinnerConsumeList.get(bigWinnerConsumeList.size() -1);
//            if(sportsPoint > minItem.getWinScore()) {
//                return CommMath.FormatDouble(bigWinnerConsumeList.stream().filter(k->sportsPoint >= k.getWinScore()).map(k->k.getSportsPoint()).findFirst().orElse(0D));
//            }
//        }
//        return 0D;
//    }

    /**
     * 是否在玩
     *
     * @param roomPos
     * @return
     */
    public boolean isPlaying(AbsRoomPos roomPos) {
        return (this.getRoom().getCurSetID() < 1 || roomPos.isPlayTheGame());
    }

    /**
     * 竞技点收益分成点
     */
    public void scorePoint(double consume, BaseCreateRoom baseCreateRoom, String dateTime, String dataJsonCfg, int consumeValue, int setCount, int gameId) {
        Map<Long, List<Long>> clubMap = this.getPosList().stream().filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getClubMemberBO()) && isPlaying(k)).collect(Collectors.groupingBy(p -> p.getClubMemberBO().getClubID(), Collectors.mapping(p -> p.getPid(), Collectors.toList())));
        if (MapUtils.isEmpty(clubMap)) {
            // 分组失败
            return;
        }
        Map<Long, Long> pidMap = this.getPosList().stream().filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getClubMemberBO()) && isPlaying(k)).collect(Collectors.toMap(AbsRoomPos::getPid, p -> p.getClubMemberBO().getClubID()));
        // 房间消耗疲劳
        if (consume <= 0D) {
            // 没有房间消耗
            return;
        }
        // 房间内的成员根据亲友圈Id分组统计
        if (MapUtils.isEmpty(pidMap)) {
            // 分组失败
            return;
        }
        // 类型ID
        long unionId = this.getRoom().getSpecialRoomId();
        // 房间名称
        String roomName = baseCreateRoom.getRoomName();
        // 配置Id
        long configId = baseCreateRoom.getGameIndex();
        // 房间key
        int roomKey = TypeUtils.StringTypeInt(this.getRoom().getRoomKey());
        DispatcherComponent.getInstance().publish(new RoomPromotionShare(consume, unionId, pidMap, roomName, roomKey, configId, getRoom().getRoomID(), getRoom().getBaseRoomConfigure().getGameType().getId(), 0, clubMap, room.getSportsPointCost(), dateTime, dataJsonCfg, consumeValue, setCount, gameId, this.room.getDateTimeZhongZhi()));
    }

    /**
     * 房间推广员活跃度计算
     * 已废弃
     */
    @Deprecated
    public void roomPromotionActiveEvent() {
        if (RoomTypeEnum.checkUnionOrClub(this.getRoom().getRoomTypeEnum()) && this.getRoom().notExistOneSetNotFinished()) {
            // 亲友圈或者赛事房间
            Map<ClubRoomPromotionItem, List<ClubMemberBO>> map = this.getRoomPosList().stream().filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getClubMemberBO()) && k.getClubMemberBO().getPartnerPid() > 0L).map(k -> k.getClubMemberBO()).collect(Collectors.groupingBy(p -> new ClubRoomPromotionItem(p.getPartnerPid(), p.getClubID())));
            if (MapUtils.isNotEmpty(map)) {
                for (Map.Entry<ClubRoomPromotionItem, List<ClubMemberBO>> entry : map.entrySet()) {
                    DispatcherComponent.getInstance().publish(new RoomPromotionActiveEvent(entry.getKey().getPartnerPid(), entry.getKey().getClubId(), this.getRoom().getGameRoomBO().getUnionId(), this.getRoom().getConfigId(), getRoom().getRoomKey(), entry.getValue()));
                }
            }
        }
    }

    /**
     * 最大分数
     *
     * @return
     */
    public int maxPoint() {
        //大局分数 计算方式不同
        if (this.getRoom().calcFenUseYiKao()) {
            return Double.valueOf(this.getPosList().stream().filter(k -> k.isPlayTheGame()).map(k -> k.getPointYiKao()).reduce(0.0, Double::max)).intValue();
        }
        return this.getPosList().stream().filter(k -> k.isPlayTheGame()).map(k -> k.getPoint()).reduce(0, Integer::max);
    }

    /**
     * 大赢家总消耗
     *
     * @return
     */
    public int sumWinnerPayConsumeCard() {
        // 最大分数
        final int maxPoint = this.maxPoint();
        // 获取大赢家列表
        return this.getPosList().stream().filter(k -> k.isPlayTheGame() && k.getPoint() == maxPoint)
                .map(k -> k.getPlayer().getRoomInfo().getConsumeCard()).reduce(0, Integer::sum);
    }

    /**
     * 获取大赢家列表
     *
     * @return
     */
    public List<Long> winnerPayList() {
        // 最大分数
        final int maxPoint = this.maxPoint();
        //大局分数 计算方式不同
        if (this.getRoom().calcFenUseYiKao()) {
            return this.getPosList().stream().filter(k -> k.isPlayTheGame() && k.getPointYiKao().intValue() == maxPoint)
                    .map(k -> k.getPid()).collect(Collectors.toList());
        }
        // 获取大赢家列表
        return this.getPosList().stream().filter(k -> k.isPlayTheGame() && k.getPoint() == maxPoint)
                .map(k -> k.getPid()).collect(Collectors.toList());
    }

    /**
     * 玩家输赢
     *
     * @return
     */
    public Map<Long, Double> playerWinOrLose() {
        // 最大分数
        Map<Long, Double> winOrLoseList = new HashMap<>();
        this.getPosList().stream().filter(k -> k.isPlayTheGame()).forEach(absRoomPos -> {
            if (null != absRoomPos.getSeatSportsPoint()) {
                double win = absRoomPos.getRoomSportsPoint() - absRoomPos.getSeatSportsPoint();
                if (0 != win) {
                    if (winOrLoseList.containsKey(absRoomPos.getPid())) {
                        winOrLoseList.replace(absRoomPos.getPid(), winOrLoseList.get(absRoomPos.getPid()) + win);
                    } else {
                        winOrLoseList.put(absRoomPos.getPid(), win);
                    }
                }
            }
        });
        return winOrLoseList;
    }

    /**
     * 查询房间内的所有玩家PID
     *
     * @return
     */
    public List<Long> getRoomPidAll() {
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L).map(k -> k.getPid()).collect(Collectors.toList());
    }

    /**
     * 查询房间内玩过游戏的所有玩家PID
     *
     * @return
     */
    public List<Long> getPlayTheGameRoomPidAll() {
        // 获取大赢家列表
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L && k.isPlayTheGame())
                .map(k -> k.getPid()).collect(Collectors.toList());
    }

    /**
     * 获取大赢家总数
     *
     * @return
     */
    public int winnerPayCount() {
        // 最大分数
        final int maxPoint = this.maxPoint();
        // 获取大赢家列表
        return (int) this.getPosList().stream().filter(k -> k.isPlayTheGame() && k.getPoint() == maxPoint)
                .map(k -> k.getPid()).count();
    }

    /**
     * 获取玩过这个游戏的人数
     *
     * @return
     */
    public int getPlayTheGameNum() {
        return (int) this.getPosList().stream().filter(k -> k.isPlayTheGame()).count();
    }

    /**
     * 清除玩家身上的房间状态
     */
    public void onClose() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key)) {
                if (key.isRobot()) {
                    RobotMgr.getInstance().freeRobot((int) key.getPid());
                } else if (key.getPid() > 0L) {
                    // 玩家离开房间 清除玩家在当前房间的标识
                    key.getPlayer().onExitRoom(this.getRoom().getRoomID());
                    // 如果 消耗类型是房卡，并且玩过一局以上，就可以验证标记为 试玩用户
                    if (PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())
                            && this.room.getCurSetID() > 1) {
                        // 如果 消耗类型是房卡，并且玩过一局以上，就可以验证标记为 试玩用户
                        key.getPlayer().tryToPlayUsers();
                    }
                    key.setTrusteeship(false, false);
                    key.setLatelyOutCardTime(0);
                }

            }
        });
        // 遍历所有玩家
        this.getWatchList().forEach(key -> {
            if (Objects.nonNull(key)) {
                if (key.isRobot()) {
                    RobotMgr.getInstance().freeRobot((int) key.getPid());
                } else if (key.getPid() > 0L) {
                    // 玩家离开房间 清除玩家在当前房间的标识
                    key.getPlayer().onExitRoom(this.getRoom().getRoomID());
                    // 如果 消耗类型是房卡，并且玩过一局以上，就可以验证标记为 试玩用户
                    if (PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())
                            && this.room.getCurSetID() > 1) {
                        // 如果 消耗类型是房卡，并且玩过一局以上，就可以验证标记为 试玩用户
                        key.getPlayer().tryToPlayUsers();
                    }
                    key.setTrusteeship(false, false);
                    key.setLatelyOutCardTime(0);
                }

            }
        });

    }

    /**
     * 操作大赢家付
     */
    public void onWinnerPay() {
        // 检查是否大赢家付
        if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.value() != this.getRoom().getBaseRoomConfigure()
                .getBaseCreateRoom().getPaymentRoomCardType()) {
            return;
        }
        // 检查是否有局数
        if (this.getRoom().getHistorySetSize() <= 0) {
            return;
        }
        List<Long> winList = this.winnerPayList();
        this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.isPlayTheGame()).forEach(k -> {
            if (winList.contains(k.getPid())) {
                k.getPlayer().getFeature(PlayerRoom.class).giveBack(this.getRoom().getBaseRoomConfigure().getGameType(),
                        this.getRoom().getValueType(), this.getRoom().getSpecialRoomId(), this.getRoom().getRoomID(),
                        winList.size());
            } else {
                k.getPlayer().getFeature(PlayerRoom.class).giveBack(this.getRoom().getBaseRoomConfigure().getGameType(),
                        this.getRoom().getValueType(), this.getRoom().getSpecialRoomId(), this.getRoom().getRoomID(), 0);
            }
        });

    }

    /**
     * 赢家按比例抽水
     */
    public double onWinnerC(boolean cost) {
        if (UnionDefine.UNION_ROOM_SPORTS_TYPE.EVERYWIN.ordinal() != this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getRoomSportsType()) {
            return 0;
        }
        int totalFee = 0;
        if (this.getRoom().getRoomTypeEnum() == RoomTypeEnum.UNION) {
            if (null == this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPercentage()) {
                return 0;
            }
            Double percentage = this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPercentage() / 100;
            if (percentage > 0) {
                Map<Long, Double> winOrLose = getRoom().getWinOrLose();
                int fee = 0;
                int totalFlow = 0;
                int totalFenCheng = 0;
                for (Map.Entry<Long, Double> win : winOrLose.entrySet()) {
                    AbsRoomPos absRoomPos = getPosByPid(win.getKey());
                    if (null == absRoomPos) {
                        absRoomPos = getWatchPosByPid(win.getKey());
                    }
                    if (null == absRoomPos) {
                        absRoomPos = getAllPosByPid(win.getKey());
                    }
                    if (null != absRoomPos && null != absRoomPos.getPlayer()) {
                        if (win.getValue() > 0) {
                            fee = Double.valueOf(Math.floor(win.getValue() * 1.0 * percentage)).intValue();
                            totalFee += fee;
                            CommLog.info(absRoomPos.getPlayer().getId() + "抽水" + fee);
                            if (cost) {
                                absRoomPos.getClubMemberBO().saveGameSportsPoint(absRoomPos.getPlayer(), this.getRoom().getSpecialRoomId(), -fee, this.getRoom().getBaseRoomConfigure().getGameType().getId(), 0, getRoom().getRoomID(), getRoom().getRoomKey());
                            }
                        }
                        totalFlow += Math.abs(win.getValue());
                    }
                }

                for (Map.Entry<Long, Double> win : winOrLose.entrySet()) {
                    AbsRoomPos absRoomPos = getPosByPid(win.getKey());
                    if (null == absRoomPos) {
                        absRoomPos = getWatchPosByPid(win.getKey());
                    }
                    if (null == absRoomPos) {
                        absRoomPos = getAllPosByPid(win.getKey());
                    }
                    if (null != absRoomPos && null != absRoomPos.getPlayer()) {
                        int fenCheng = Double.valueOf(Math.floor(Math.abs(win.getValue()) / totalFlow * totalFee)).intValue();
                        totalFenCheng += fenCheng;
                        CommLog.info(absRoomPos.getPlayer().getId() + "抽水分成" + fenCheng);
                    }
                }

                CommLog.info("总抽水" + totalFee + "总流水" + totalFlow + "总分成" + totalFenCheng);
            }
        }
        return totalFee;
    }

    /**
     * 存在托管的玩家
     */
    public boolean checkExistTrusteeship() {
        return this.getPosList().stream().filter(k -> k.isTrusteeship()).findAny().isPresent();
    }

    /**
     * 不存在托管的玩家
     */
    public boolean checkNotExistTrusteeship() {
        return !this.checkExistTrusteeship();
    }

    /**
     * 设置所有用户的超时
     */
    public void setAllLatelyOutCardTime() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key)) {
                key.setLatelyOutCardTime(CommTime.nowMS());
            }
        });
    }

    /**
     * 所有玩家设置拒绝解散状态
     */
    public List<RoomPosInfo> getNotify_PosList() {
        return this.getPosList().stream().map(k -> k.getNotify_PosInfo()).collect(Collectors.toList());
    }

    public List<RoomPosInfoShort> getRoomPosInfoShortList() {
        return this.getPosList().stream().map(k -> k.getRoomPosInfoShort()).collect(Collectors.toList());
    }

    /**
     * 玩家位置基本信息
     *
     * @return
     */
    public List<RoomPlayerPos> getRoomPlayerPosList() {
        return this.getPosList().stream()
                .filter(k -> Objects.nonNull(k) && k.getPid() > 0L && (this.getRoom().getCurSetID() <= 1 || k.isPlayTheGame()))
                .map(k -> k.roomPlayerPos()).sorted(Comparator.comparing(RoomPlayerPos::getPos))
                .collect(Collectors.toList());
    }

    /**
     * 获取玩家简介列表
     *
     * @return
     */
    public List<ShortPlayer> getShortPlayerList() {
        return this.getPosList().stream().filter(k -> k.getPid() > 0L && k.isPlayTheGame()).map(k -> k.getShortPlayer())
                .collect(Collectors.toList());
    }

    /**
     * 获取玩家简介列表
     *
     * @return
     */
    public List<PlayerPosInfo> getPlayerPosInfoList() {
        return this.getPosList().stream().filter(k -> k.getPid() > 0L && k.isPlayTheGame())
                .map(k -> k.getPlayerPosInfo()).collect(Collectors.toList());
    }


    /**
     * 回退所有未玩过游戏人的数据
     */
    public void backConsumeNotPlayTheGame() {
        if (this.getRoom().isBackRoomCard()) {
            // 已回退过房卡不能重复回退
            return;
        }
        PrizeType type = this.getRoom().getValueType();
        if (this.getRoom().getHistorySetSize() >= 1 && PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())) {
            this.getPosList().forEach(k -> {
                if (k.getPid() > 0 && !k.isPlayTheGame()) {
                    if (!business.global.billing.RoomBillingGuard.beginRefund(
                            this.getRoom().getRoomID(), k.getPid())) {
                        return;
                    }
                    if (PrizeType.RoomCard.equals(type)) {
                        k.getPlayer().getFeature(PlayerWallet.class).backConsumeRoom(k.getPlayer().getRoomInfo().getConsumeCard(), this.getRoom().getBaseRoomConfigure().getGameType());
                    } else {
                        k.getPlayer().getFeature(PlayerClub.class).clubCardReturnCradRoom(k.getPlayer().getRoomInfo().getConsumeCard(), this.getRoom().getBaseRoomConfigure().getGameType(), this.getRoom().getSpecialRoomId(), ItemFlow.ClubCardClubRoom);
                    }
                }
            });
        }
    }

    /**
     * 清空游戏准备状态
     */
    public void clearGameReady() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key)) {
                key.setGameReady(false);
            }
        });
    }

    /**
     * 检查指定位置是否空位置
     *
     * @param posID
     * @return
     */
    public AbsRoomPos getEmptyPos(int posID) {
        AbsRoomPos pos = posList.get(posID);
        // 玩家位置不存在
        if (Objects.isNull(pos)) {
            return null;
        }
        // 检查该位置是否有玩家
        if (pos.getPid() <= 0L) {
            return pos;
        }
        return null;
    }

    /**
     * 通过pid获取玩家信息
     *
     * @param pid 玩家PID
     * @return
     */
    public AbsRoomPos getPosByPid(long pid) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getPosList().stream().filter((x) -> x.getPid() == pid).findAny().orElse(null);
    }

    /**
     * 通过pid获取玩家信息
     *
     * @param pid 玩家PID
     * @return
     */
    public AbsRoomPos getWatchPosByPid(long pid) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getWatchList().stream().filter((x) -> x.getPid() == pid).findAny().orElse(null);
    }


    /**
     * 通过pid获取玩家信息
     *
     * @param pid 玩家PID
     * @return
     */
    public AbsRoomPos getAllPosByPid(long pid) {
        if (CollectionUtils.isEmpty(this.getAllList())) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getAllList().stream().filter((x) -> x.getPid() == pid).findAny().orElse(null);
    }

    /**
     * 通过PosID获取玩家信息
     *
     * @param posID 玩家位置ID
     * @return
     */
    public AbsRoomPos getPosByPosID(int posID) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getPosList().stream().filter((x) -> x.getPosID() == posID).findAny().orElse(null);
    }

    /**
     * 获取一个空的位置
     *
     * @return
     */
    public AbsRoomPos getEmptyPos() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getPosList().stream().filter((x) -> x.getPid() <= 0L).findAny().orElse(null);
    }

    /**
     * 获取空位数量
     *
     * @return
     */
    public int getEmptyPosCount() {
        return (int) this.getPosList().stream().filter((x) -> x.getPid() <= 0L).count();
    }

    /**
     * 获取坐在位置玩家的数量
     *
     * @return
     */
    public int getFullPosCount() {
        return (int) this.getPosList().stream().filter((x) -> x.getPid() > 0L).count();
    }

    /**
     * 检查是否存在空位置
     *
     * @return
     */
    public boolean checkExistEmptyPos() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return false;
        }
        return this.getPosList().stream().anyMatch(k -> k.getPid() <= 0L);
    }


    /**
     * 不存在空位置
     *
     * @return
     */
    public boolean checkNotExistEmptyPos() {
        return !this.checkExistEmptyPos();
    }

    /**
     * 检查存在没人
     *
     * @return T：没人，F:有
     */
    public boolean checkExistNoOne() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return false;
        }
        return this.getPosList().stream().allMatch(k -> k.getPid() <= 0L || k.isRobot());
    }

    /**
     * 检查所有自动准备游戏的玩家是否超时
     *
     * @return
     */
    public void checkAllAutoReadyGameOvertime() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        this.getPosList().stream().forEach(k -> k.autoReadyGameOvertime());
    }

    /**
     * 手动模式下，踢出30秒未准备的玩家
     *
     * @return
     */
    public void checkKickOutTimeOutPlayer() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        boolean fullPosList = getEmptyPos() == null;
        boolean isClubOrUnion = RoomTypeEnum.checkUnionOrClub(room.getRoomTypeEnum());
        List<Long> kickOutList = new ArrayList<>();
        if (fullPosList && allEnterTime != 0 && isClubOrUnion) {
            this.getPosList().stream().forEach(k -> {
                if (k.isReady() || k.getPid() <= 0L) {
                    // 已经准备了不用管
                    return;
                }
                // 当前时间和进入房间时间相差 大于30秒
                if (CommTime.nowSecond() - allEnterTime >= 30) {
                    kickOutList.add(k.getPid());
                }
            });
            //踢出房间
            kickOutList.stream().forEach(k -> {
                AbsRoomPos roomPos = getPosByPid(k);
                roomPos.leave(true, this.getRoom().getOwnerID(), CKickOutType.TIMEOUT);
                allEnterTime = 0;
            });
        }
    }


    /**
     * 清空所有玩家准备状态
     *
     * @return
     */
    public void clearAllPosReady() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        this.getPosList().stream().filter(k -> k.getPid() > 0L).forEach(k -> k.setReady(false));
        //清除踢出房间计时
        setAllEnterTime(0);
    }

    /**
     * 是否所有玩家准备
     *
     * @return
     */
    public boolean isAllReady() {
        if (null == this.getPosList() || this.getPosList().size() <= 1) {
            // 玩家信息列表没数据
            return false;
        }
        return this.getPosList().stream().allMatch(k -> k.isReady());
    }

    /**
     * 获取在玩玩家
     *
     * @return
     */
    public List<AbsRoomPos> getPlayingPos() {
        if (null == this.getPosList()) {
            // 玩家信息列表没数据
            return null;
        }
        return this.getPosList().stream().filter(k -> k.isPlayTheGame()).collect(Collectors.toList());
    }

    /**
     * 是否所有玩家继续下一局
     *
     * @return
     */
    public boolean isAllContinue() {
        if (null == this.getPosList() || this.getPosList().stream().filter(absRoomPos -> absRoomPos.getPlayer() != null).count() <= 1) {
            // 玩家信息列表没数据
            return false;
        }
//        //超时继续，萍乡
        this.getPosList().stream().forEach(k -> {
            if (k.getPid() > 0 && !k.isGameReady() && k.getTimeSec() > 0 && CommTime.nowSecond() - k.getTimeSec() >= 5) {
                getRoom().continueGame(k.getPid());
            }
        });
        // 玩家在游戏中并且没有准备。
        return this.getPosList().stream().allMatch(k -> k.getPid() > 0L && k.isGameReady());
    }

    /**
     * 所有玩家设置拒绝解散状态
     */
    public void allRefuseDissolve() {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && key.getPid() > 0L) {
                key.setDissolveRoom(false);
            }
        });
        // 遍历通知所有玩家
        this.getWatchList().forEach(key -> {
            if (Objects.nonNull(key) && key.getPid() > 0L) {
                key.setDissolveRoom(false);
            }
        });
    }


    /**
     * 通知所有人房间竞技点更新
     */
    public boolean notify2RoomSportsPointChange(long pid, long memberId, double value) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return false;
        }
        AbsRoomPos roomPos = this.getPosByPid(pid);
        if (Objects.isNull(roomPos)) {
            return false;
        }
        if (memberId > 0L && memberId == roomPos.memberId()) {
            roomPos.setRoomSportsPoint(CommMath.addDouble(roomPos.getRoomSportsPointValue(), value));
            if (value > 0) {
                roomPos.setOtherSportsPointConsume(CommMath.subDouble(roomPos.getOtherSportsPointConsume(), value));
            } else if (value < 0) {
                roomPos.setOtherSportsPointConsume(CommMath.addDouble(roomPos.getOtherSportsPointConsume(), value));
            }
            BaseSendMsg msg = SRoom_SportsPointChange.make(this.room.getRoomID(), roomPos.getPosID(), pid, value);
            // 遍历通知所有玩家
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L) {
                    key.getPlayer().pushProto(msg);
                }
            });
            this.getWatchList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L) {
                    key.getPlayer().pushProto(msg);
                }
            });
            return true;
        }
        return false;
    }


    /**
     * 只通知在游戏中的玩家
     *
     * @param msg 通知消息
     */
    public void notify2Playing(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.isPlayTheGame()) {
                key.getPlayer().pushProto(msg);
            }
        });
    }


    /**
     * 通知所有人
     *
     * @param msg 通知消息
     */
    public void notify2AllNotExistRoom(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        if (Config.isShare()) {
            // 通知MQ
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(key.getPid(), msg, msg.getClass().getName()));
                }
            });
            this.getWatchList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(key.getPid(), msg, msg.getClass().getName()));
                }
            });

        } else {
            // 遍历通知所有玩家
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    key.getPlayer().pushProto(msg);
                }
            });
            // 遍历通知所有玩家
            this.getWatchList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    key.getPlayer().pushProto(msg);
                }
            });
        }

    }

    /**
     * 通知所有人
     *
     * @param msg 通知消息
     */
    public void notify2SeatNotExistRoom(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        if (Config.isShare()) {
            // 通知MQ
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(key.getPid(), msg, msg.getClass().getName()));
                }
            });

        } else {
            // 遍历通知所有玩家
            this.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPlayer().notExistRoom()) {
                    key.getPlayer().pushProto(msg);
                }
            });
        }

    }

    /**
     * 通知所有人走MQ
     *
     * @param msg 通知消息
     */
    public void notify2AllMq(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L) {
                MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(key.getPid(), msg, msg.getClass().getName()));
            }
        });
    }

    /**
     * 通知所有人
     *
     * @param msg 通知消息
     */
    public void notify2All(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L) {
                key.getPlayer().pushProto(msg);
            }
        });
    }

    /**
     * 仅用于已经确认不含手牌、暗牌、牌墙、随机种子等私有字段的公开消息。
     * 默认 notify2All 永远不包含观战者，防止玩法误用导致透视。
     */
    public void notify2AllPublic(BaseSendMsg publicMsg) {
        notify2All(publicMsg);
        this.getWatchList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L) {
                key.getPlayer().pushProto(publicMsg);
            }
        });
    }

    /**
     * 通知所有在玩玩家
     *
     * @param msg 通知消息
     */
    public void notify2AllPlaying(BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.isPlayTheGame()) {
                key.getPlayer().pushProto(msg);
            }
        });
    }

    /**
     * 通知指定位置的人
     * 并且清除最近打牌时间
     *
     * @param pos 位置
     * @param msg 通知消息
     */
    public void notify2PosClearLatelyOutCardTime(int pos, BaseSendMsg msg) {
        // 通过PosID获取玩家信息
        AbsRoomPos result = this.getPosByPosID(pos);
        if (Objects.isNull(result)) {
            return;
        }
        result.setLatelyOutCardTime(0);
        if (result.isRobot()) {
            return;
        }
        if (result.getPid() <= 0L) {
            return;
        }
        // 玩家通知
        result.getPlayer().pushProto(msg);
    }

    /**
     * 通知指定位置的人
     *
     * @param pos 位置
     * @param msg 通知消息
     */
    public void notify2Pos(int pos, BaseSendMsg msg) {
        // 通过PosID获取玩家信息
        AbsRoomPos result = this.getPosByPosID(pos);
        if (Objects.isNull(result)) {
            return;
        }
        if (result.isRobot()) {
            return;
        }
        if (result.getPid() <= 0L) {
            return;
        }
        // 玩家通知
        result.getPlayer().pushProto(msg);
    }

    /**
     * 通知所有玩家除指定排除的PID玩家
     *
     * @param excludePid 排除玩家PID
     * @param msg        通知消息
     */
    public void notify2ExcludePid(long excludePid, BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPid() != excludePid) {
                key.getPlayer().pushProto(msg);
            }
        });
    }

    /**
     * 通知所有玩家除指定排除的PosID玩家
     *
     * @param excludePosID 排除玩家PosID
     * @param msg          通知消息
     */
    public void notify2ExcludePosID(int excludePosID, BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(this.getPosList())) {
            // 玩家信息列表没数据
            return;
        }
        // 遍历通知所有玩家
        this.getPosList().forEach(key -> {
            if (Objects.nonNull(key) && !key.isRobot() && key.getPid() > 0L && key.getPosID() != excludePosID) {
                key.getPlayer().pushProto(msg);
            }
        });
    }

    /**
     * 当所有人取消所有托管状态
     */
    public boolean cancleTrusteeship() {
        for (int i = 0; i < playerNum; i++) {
            AbsRoomPos pos = posList.get(i);
            if (null != pos) {
                pos.setTrusteeship(false, false);
            }
        }
        return true;
    }

    /**
     * @return
     */
    public boolean calcAnyPlayerHasChangePoint() {
        if (this.getRoom().notExistOneSetNotFinished()) {
            return true;
        }
        if (this.isCalcPoint()) {
            return true;
        }
        this.setCalcPoint(this.getPosList().stream().anyMatch(k -> Objects.nonNull(k.sportsPoint()) && k.sportsPoint().doubleValue() != 0D));
        return this.isCalcPoint();
    }

    public void removeWatch(long pid) {
        AbsRoomPos roomPos = getWatchPosByPid(pid);
        getWatchList().remove(roomPos);
    }

    public void leavePos(long pid) {
        AbsRoomPos pos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
        if (null != pos) {
            pos.clear();
        }
    }
}
