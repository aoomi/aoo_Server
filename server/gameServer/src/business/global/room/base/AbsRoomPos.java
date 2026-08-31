package business.global.room.base;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.Robot.Robot;
import business.player.Robot.RobotMgr;
import business.player.feature.PlayerRoom;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.VisitSignEnum;
import cenum.room.CKickOutType;
import cenum.room.PaymentRoomCardType;
import cenum.room.SetState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.ClubMemberBO;
import core.db.entity.clarkGame.PlayerRoomAloneBO;
import core.db.entity.clarkLog.ClubLevelRoomLogFlow;
import core.db.entity.clarkLog.ClubLevelRoomLogZhongZhiFlow;
import core.db.other.AsyncInfo;
import core.db.service.clarkLog.ClubLevelRoomLogFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogZhongZhiFlowService;
import core.ioc.Constant;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.LocationInfo;
import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.pos.PlayerPosInfo;
import jsproto.c2s.cclass.pos.RoomPlayerPos;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseResults;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.room.RoomPosInfoShort;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.room.SBase_PosLeave;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * 房间内每个位置信息
 */
@Data
public class AbsRoomPos implements Serializable {
    /**
     * 0-3->1-4号位置
     */
    private int posID = 0;
    /**
     * 玩家ID
     */
    private long pid = 0;
    /**
     * 房间父节点
     */
    private AbsBaseRoom room = null;
    /**
     * 是否已经准备好，全部准备好，才能开始进行第一轮游戏
     */
    protected boolean isReady = false;
    /**
     * 是否已经准备好，继续游戏
     */
    private boolean isGameReady = false;
    /**
     * 是否掉线
     */
    private boolean isLostConnect = false;
    /**
     * 是否显示离开
     */
    private boolean isShowLeave = false;
    /**
     * 最近出手的时间
     */
    private long latelyOutCardTime = 0;
    /**
     * 积分
     */
    private int point = 0;
    /**
     * 是否托管
     */
    private boolean isTrusteeship = false;
    /**
     * 是否同意解散
     */
    private boolean isDissolveRoom = false;
    /**
     * 是否机器人
     */
    private boolean isRobot = false;
    /**
     * 是否玩过这个游戏
     */
    private boolean isPlayTheGame = false;
    /**
     * 玩家身上携带的金币
     */
    private int goldValue = 0;
    /**
     * 玩家信息
     */
    private transient Player player = null;
    /**
     * 玩家位置结算记录统计
     */
    protected BaseResults results = null;
    /**
     * 机器人信息
     */
    private transient Robot robot = null;
    /**
     * 玩家名称
     */
    private String name = "";
    /**
     * 头像信息
     */
    private String headImageUrl = "";
    /**
     * 性别
     */
    private int sex = 0;
    /**
     * IP
     */
    private String playerIP = null;
    /**
     * 玩家账号ID
     */
    private long accountID = 0L;
    /**
     * 合伙人PID
     */
    private long partnerPid = 0L;
    /**
     * 玩家记录数据
     */
    private PlayerRoomAloneBO playerRoomAloneBO;
    /**
     * 统计分数
     */
    private int countPoint;
    /**
     * 亲友圈成员
     */
    private ClubMemberBO clubMemberBO = null;

    /**
     * 房间竞技点
     */
    private double roomSportsPoint;

    /**
     * 加入游戏时间
     */
    private int joinPlayTime;

    /**
     * 超时继续
     */
    private long timeSec = 0;

    /**
     * 解散次数
     */
    private int dissolveCount = 0;

    /**
     * 亲友圈名称
     */
    private String clubName;

    /**
     * 总增减竞技点
     */
    private Double pointYiKao = 0D;
    /**
     * 坐下位置时候的竞技点
     */
    private Double seatSportsPoint;
    /**
     * 不是游戏导致的消耗(比如 切牌费用)
     * 值为正数
     */
    private Double otherSportsPointConsume = 0D;

    /**
     * 上级玩家名称
     */
    private String upLevelName;
    /**
     * 上级玩家名称
     */
    private long clubID;
    /**
     * 发牌的时候玩家身上的分数
     */
    private double gameBeginSportsPoint = 0D;

    private boolean isPlaying;

    private Long liuzuoSec;

    /**
     * 添加玩家个人游戏记录
     */
    public void insertPlayerRoomAloneBO() {
        if (null != this.getPlayerRoomAloneBO()) {
            // 数据存在不需要在插入了
            if (this.getPid() != this.getPlayerRoomAloneBO().getPid()) {
                // 更新玩家Pid
                this.getPlayerRoomAloneBO().savePid(this.getPid());
            }
            return;
        }
        if (this.getPid() <= 0L || this.isRobot()) {
            return;
        }
        if (PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())) {
            // 房间配置
            BaseCreateRoom baseCreateRoom = this.getRoom().getBaseRoomConfigure().getBaseCreateRoom();
            this.setPlayerRoomAloneBO(new PlayerRoomAloneBO());
            this.getPlayerRoomAloneBO().setPid(this.getPid());
            this.getPlayerRoomAloneBO().setPrizeType(this.getRoom().getValueType().value());
            this.getPlayerRoomAloneBO().setPaymentType(baseCreateRoom.getPaymentRoomCardType());
            this.getPlayerRoomAloneBO().setSumCount(this.getRoom().getCount());
            this.getPlayerRoomAloneBO().setRoomKey(this.getRoom().getRoomKey());
            this.getPlayerRoomAloneBO().setCreateRoomTime(this.getRoom().getTask().getCreateSec());
            this.getPlayerRoomAloneBO().setClubCostType(this.getRoom().getValueType().value());
            this.getPlayerRoomAloneBO().setRoomID(this.getRoom().getRoomID());
            this.getPlayerRoomAloneBO().setGameType(this.getRoom().getBaseRoomConfigure().getGameType().getId());
            this.getPlayerRoomAloneBO().setClassType(this.getRoom().getBaseRoomConfigure().getGameType().getType().value());
            this.getPlayerRoomAloneBO().setPartnerPid(this.getPartnerPid());
            this.getPlayerRoomAloneBO().setRoomTypeValue(this.getRoom().getRoomTyepImpl().getRoomTypeEnum().ordinal());
            this.getPlayerRoomAloneBO().setClubID(clubId());
            this.getPlayerRoomAloneBO().setUnionId(baseCreateRoom.getUnionId());
            this.getPlayerRoomAloneBO().setRoomSportsType(baseCreateRoom.getRoomSportsType());
            this.getPlayerRoomAloneBO().getBaseService().saveOrUpDate(this.getPlayerRoomAloneBO(), new AsyncInfo(this.getPid()));
            this.getPlayer().setSignEnum(VisitSignEnum.ROOM);
        }
    }

    public BaseResults getResults() {
        if (Objects.nonNull(results)) {
            // 解散操作状态（-1:正常结束,0:未操作,1:同意操作,2:拒绝操作,3:发起者）
            results.setDissolveState(Objects.nonNull(this.getRoom().getDissolveRoom()) ? this.getRoom().getDissolveRoom().getDissolveState(getPosID()) : -1);
            results.setSameIpState(Objects.nonNull(this.getRoom().getSameIpDissolveRoom()) ? this.getRoom().getSameIpDissolveRoom().getDissolveState(this.getPid()) : -1);

        }
        return results;
    }

    /**
     * 保存玩家分数
     */
    public void savaPlayerRoomAlonePoint(int endTime) {
        if (null == this.getPlayerRoomAloneBO()) {
            return;
        }
        // 保存分数
        this.getPlayerRoomAloneBO().savePoint(this.getPoint(), endTime);
    }

    /**
     * 更新玩家个人游戏记录
     */
    public void updatePlayerRoomAloneBO(int consumeValue, int sizeWinner, boolean isWinner, int dateTime, int endTime, double roomSportsPointConsume) {
        if (Objects.isNull(this.getPlayerRoomAloneBO())) {
            return;
        }
        BaseCreateRoom baseCreateRoom = this.getRoom().getBaseRoomConfigure().getBaseCreateRoom();
        // T:有结算F:第一局未打完
        boolean notExistOneSetNotFinished = this.getRoom().notExistOneSetNotFinished();
        Double sportsPoint = this.sportsPoint();
        sportsPoint = Objects.isNull(sportsPoint) ? 0D : sportsPoint;
        if (!notExistOneSetNotFinished) {
            notExistOneSetNotFinished = getRoom().getRoomPosMgr().calcAnyPlayerHasChangePoint();
        }
        double roomSportsConsume = getRoomSportsPointConsume(baseCreateRoom, sizeWinner, isWinner, roomSportsPointConsume);
        this.getPlayerRoomAloneBO().setValue(consumeValue);
        this.getPlayerRoomAloneBO().setPoint(this.getPoint());
        this.getPlayerRoomAloneBO().setWinner(this.isWinner(isWinner));
        this.getPlayerRoomAloneBO().setSetCount(this.getRoom().getCurSetID());
        this.getPlayerRoomAloneBO().setDateTime(dateTime);
        this.getPlayerRoomAloneBO().setEndTime(endTime);
        this.getPlayerRoomAloneBO().setSportsPoint(CommMath.FormatDouble(sportsPoint));
        this.getPlayerRoomAloneBO().setRoomSportsConsume(CommMath.FormatDouble(roomSportsConsume));
        if (notExistOneSetNotFinished) {
            this.getPlayerRoomAloneBO().setMemberId(this.memberId());
            this.getPlayerRoomAloneBO().setUpLevelId(this.upLevelId());
        }
        this.getPlayerRoomAloneBO().getBaseService().update(this.getPlayerRoomAloneBO().getUpdateKeyValue(), this.getPlayerRoomAloneBO().getId());
        // 保存竞技点
        if (getRoom().isWatchRoom()) {
            this.saveSportsPoint(0, roomSportsConsume);
        } else {
            this.saveSportsPoint(sportsPoint, roomSportsConsume);
        }
        if (Objects.nonNull(this.getPlayer())) {
            this.getPlayer().getExp().addVipExp();
        }
        // 亲友圈等级消耗
        this.clubLevelRoomLog(dateTime, isWinner, consumeValue, notExistOneSetNotFinished, sportsPoint, roomSportsConsume, roomSportsPointConsume, baseCreateRoom);
    }

    /**
     * 大赢家
     *
     * @param isWinner T:赢家,F:不是
     * @return
     */
    public int isWinner(boolean isWinner) {
        if (this.getRoom().existOneSetNotFinished() && this.getRoom().existWinnerOneSetDissolve()) {
            // 存在第一个未打完并且存在第一个局未打完解散不记录大赢家次数
            return 0;
        }
        return isWinner ? 1 : 0;
    }


    /**
     * 亲友圈等级消耗
     *
     * @param dateTime                  时间
     * @param isWinner                  是否大赢家
     * @param consumeValue              消耗值
     * @param notExistOneSetNotFinished 是否真实消耗
     * @param sportsPointConsume        比赛分消耗
     * @param roomSportsConsume         房费比赛分消耗
     * @param roomAvgSportsPointConsume 房费均值比赛分消耗
     */
    public void clubLevelRoomLog(int dateTime, boolean isWinner, int consumeValue, boolean notExistOneSetNotFinished, double sportsPointConsume, double roomSportsConsume, double roomAvgSportsPointConsume, BaseCreateRoom baseCreateRoom) {
        ClubMemberBO cMemberBO = this.getClubMemberBO();
        if (Objects.nonNull(cMemberBO) && notExistOneSetNotFinished) {
            int playingCount = this.getRoom().getPlayingCount();
            // 	玩家单房间消耗数=房间消耗/房间人数；
            double consume = CommMath.div(consumeValue, playingCount);
            // 是否赢家值
            int winnerInt = isWinner ? 1 : 0;
            // 时间
            String dateTimeStr = String.valueOf(dateTime);
            // 局数
            int setCount = 1;
            this.insertNoramlClubLevelRoomLog(dateTimeStr, winnerInt, consume, setCount, sportsPointConsume, roomSportsConsume, roomAvgSportsPointConsume, baseCreateRoom, playingCount);
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(cMemberBO.getClubID());
            UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
            if (Objects.nonNull(club)) {
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
            }
            if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                this.insertNoramlClubLevelRoomLogZhongZhi(this.getRoom().getDateTimeZhongZhi(), winnerInt, consume, setCount, sportsPointConsume, roomSportsConsume, roomAvgSportsPointConsume, baseCreateRoom, playingCount);
            }

        }

    }

    /**
     * 正常赛事插入数据
     *
     * @param dateTimeStr
     * @param winnerInt
     * @param consume
     * @param setCount
     * @param sportsPointConsume
     * @param roomSportsConsume
     * @param roomAvgSportsPointConsume
     * @param baseCreateRoom
     * @param playingCount
     */
    public void insertNoramlClubLevelRoomLog(String dateTimeStr, int winnerInt, double consume, int setCount, double sportsPointConsume, double roomSportsConsume, double roomAvgSportsPointConsume, BaseCreateRoom baseCreateRoom, int playingCount) {
        ClubMemberBO cMemberBO = this.getClubMemberBO();
        ClubLevelRoomLogFlow clubLevelRoomLogFlow = new ClubLevelRoomLogFlow();
        clubLevelRoomLogFlow.setServer_id(Constant.serverIid);
        clubLevelRoomLogFlow.setTimestamp(CommTime.nowSecond());
        clubLevelRoomLogFlow.setDate_time(dateTimeStr);
        clubLevelRoomLogFlow.setPid(getPid());
        clubLevelRoomLogFlow.setWinner(winnerInt);
        clubLevelRoomLogFlow.setConsume(consume);
        clubLevelRoomLogFlow.setRoomId(getRoom().getRoomID());
        clubLevelRoomLogFlow.setUpLevelId(cMemberBO.getUpLevelId());
        clubLevelRoomLogFlow.setMemberId(cMemberBO.getId());
        clubLevelRoomLogFlow.setPoint(getPoint());
        clubLevelRoomLogFlow.setClubId(cMemberBO.getClubID());
        clubLevelRoomLogFlow.setSetCount(setCount);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(cMemberBO.getClubID());
        clubLevelRoomLogFlow.setUnionId(club.getClubListBO().getUnionId());
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            clubLevelRoomLogFlow.setSportsPointConsume(sportsPointConsume);
            clubLevelRoomLogFlow.setRoomSportsPointConsume(roomSportsConsume);
            if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
                clubLevelRoomLogFlow.setRoomAvgSportsPointConsume(Math.max(0D, CommMath.div(roomAvgSportsPointConsume, playingCount)));
            } else {
                clubLevelRoomLogFlow.setRoomAvgSportsPointConsume(CommMath.FormatDouble(baseCreateRoom.getRoomSportsEveryoneConsume()));
            }
        }
        ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).insert(clubLevelRoomLogFlow.getInsertSql(), clubLevelRoomLogFlow.addToBatch());
    }

    /**
     * 中至赛事插入数据
     *
     * @param dateTimeStr
     * @param winnerInt
     * @param consume
     * @param setCount
     * @param sportsPointConsume
     * @param roomSportsConsume
     * @param roomAvgSportsPointConsume
     * @param baseCreateRoom
     * @param playingCount
     */
    public void insertNoramlClubLevelRoomLogZhongZhi(String dateTimeStr, int winnerInt, double consume, int setCount, double sportsPointConsume, double roomSportsConsume, double roomAvgSportsPointConsume, BaseCreateRoom baseCreateRoom, int playingCount) {
        ClubMemberBO cMemberBO = this.getClubMemberBO();
        ClubLevelRoomLogZhongZhiFlow clubLevelRoomLogZhongZhiFlow = new ClubLevelRoomLogZhongZhiFlow();
        clubLevelRoomLogZhongZhiFlow.setServer_id(Constant.serverIid);
        clubLevelRoomLogZhongZhiFlow.setTimestamp(CommTime.nowSecond());
        clubLevelRoomLogZhongZhiFlow.setDate_time(dateTimeStr);
        clubLevelRoomLogZhongZhiFlow.setPid(getPid());
        clubLevelRoomLogZhongZhiFlow.setWinner(winnerInt);
        clubLevelRoomLogZhongZhiFlow.setConsume(consume);
        clubLevelRoomLogZhongZhiFlow.setRoomId(getRoom().getRoomID());
        clubLevelRoomLogZhongZhiFlow.setUpLevelId(cMemberBO.getUpLevelId());
        clubLevelRoomLogZhongZhiFlow.setMemberId(cMemberBO.getId());
        clubLevelRoomLogZhongZhiFlow.setPoint(getPoint());
        clubLevelRoomLogZhongZhiFlow.setClubId(cMemberBO.getClubID());
        clubLevelRoomLogZhongZhiFlow.setSetCount(setCount);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(cMemberBO.getClubID());
        clubLevelRoomLogZhongZhiFlow.setUnionId(club.getClubListBO().getUnionId());
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            clubLevelRoomLogZhongZhiFlow.setSportsPointConsume(sportsPointConsume);
            clubLevelRoomLogZhongZhiFlow.setRoomSportsPointConsume(roomSportsConsume);
            if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
                clubLevelRoomLogZhongZhiFlow.setRoomAvgSportsPointConsume(Math.max(0D, CommMath.div(roomAvgSportsPointConsume, playingCount)));
            } else {
                clubLevelRoomLogZhongZhiFlow.setRoomAvgSportsPointConsume(CommMath.FormatDouble(baseCreateRoom.getRoomSportsEveryoneConsume()));
            }
        }
        ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).insert(clubLevelRoomLogZhongZhiFlow.getInsertSql(), clubLevelRoomLogZhongZhiFlow.addToBatch());
    }

    /**
     * 保存竞技点
     */
    public void saveSportsPoint(double sportsPointGame, double sportsPointRoom) {
        if (null != this.getClubMemberBO() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (sportsPointRoom > 0) {
                this.getClubMemberBO().saveRoomSportsPoint(player, this.getRoom().getSpecialRoomId(), -sportsPointRoom, this.getRoom().getBaseRoomConfigure().getGameType().getId(), 0, getRoom().getRoomID(), getRoom().getRoomKey());
            }
            if (0 != sportsPointGame) {
                this.getClubMemberBO().saveGameSportsPoint(player, this.getRoom().getSpecialRoomId(), sportsPointGame, this.getRoom().getBaseRoomConfigure().getGameType().getId(), 0, getRoom().getRoomID(), getRoom().getRoomKey());
            }
        }
    }

    /**
     * 获取房间内的竞技点消耗
     */
    public double getRoomSportsPointConsume(BaseCreateRoom baseCreateRoom, int sizeWinner, boolean isWinner, double roomSportsPointConsume) {
        Double roomSportsPointConsumeCalc;
        if (this.getRoom().getHistorySetSize() < 1 && PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())) {
            return 0D;
        }
        // 	赛事房间第一局没结算就解散的房间，消耗的钻石有返还，但是房间消耗的竞技点未返还；
        //    	需返还所设置的房间消耗的竞技点给对应的玩家；
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
                if (isWinner) {
                    roomSportsPointConsumeCalc = Math.max(0D, CommMath.div(roomSportsPointConsume, sizeWinner));
                    this.room.addFactRoomSportsConsume(roomSportsPointConsumeCalc);
                    return roomSportsPointConsumeCalc;
                }
                return 0D;
            } else {
                // 房间竞技点每人消耗
                roomSportsPointConsumeCalc = CommMath.FormatDouble(baseCreateRoom.getRoomSportsEveryoneConsume());
                this.room.addFactRoomSportsConsume(roomSportsPointConsumeCalc);
                return roomSportsPointConsumeCalc;
            }
        }
        return 0D;
    }

    /**
     * 成员Id
     *
     * @return
     */
    public long memberId() {
        ClubMemberBO clubMemberBO = this.getClubMemberBO();
        if (Objects.nonNull(clubMemberBO)) {
            return clubMemberBO.getId();
        }
        return 0L;
    }


    /**
     * 成员Id
     *
     * @return
     */
    public long upLevelId() {
        ClubMemberBO clubMemberBO = this.getClubMemberBO();
        if (Objects.nonNull(clubMemberBO)) {
            return clubMemberBO.getUpLevelId();
        }
        return 0L;
    }

    /**
     * 获取亲友圈Id
     *
     * @return
     */
    public long clubId() {
        if (null != this.getClubMemberBO()) {
            return this.getClubMemberBO().getClubID();
        }
        return 0L;
    }

    /**
     * 竞技点
     *
     * @return
     */
    public Double sportsPoint() {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            return CommMath.mul(this.getPoint(), Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble()));
        } else {
            return null;
        }
    }


    /**
     * 自动解散
     *
     * @param autoDismissValue 最低值
     * @return
     */
    public boolean isAutoDismiss(double autoDismissValue) {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            return this.getRoomSportsPointValue() < CommMath.FormatDouble(autoDismissValue);
        }
        return false;
    }


    /**
     * 当局竞技点
     *
     * @param value
     * @return
     */
    public Double setSportsPoint(int value) {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            return CommMath.mul(value, Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble()));
        } else {
            return null;
        }
    }

    /**
     * 亲友圈成员竞技点
     *
     * @return
     */
    public double getClubMemberSportsPoint() {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (Objects.nonNull(this.getClubMemberBO())) {
                return this.getClubMemberBO().getSportsPoint();
            }
        }
        return 0D;
    }


    /**
     * 预先扣除房间比赛分
     *
     * @return
     */
    public Double beforehandRoomSportsPointConsume() {
        BaseCreateRoom baseCreateRoom = this.getRoom().getBaseRoomConfigure().getBaseCreateRoom();
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (UnionDefine.UNION_ROOM_SPORTS_TYPE.BIG_WINNER.ordinal() == baseCreateRoom.getRoomSportsType()) {
                return 0D;
            } else {
                // 房间竞技点每人消耗
                return CommMath.FormatDouble(baseCreateRoom.getRoomSportsEveryoneConsume());
            }
        }
        return 0D;
    }


    /**
     * 预先扣除位置平分比赛分房费
     *
     * @param seatSportsPoint
     */
    public void beforehandSeatSportsPoint(Double seatSportsPoint) {
        this.seatSportsPoint = CommMath.subDouble(seatSportsPoint, this.beforehandRoomSportsPointConsume());
    }

    /**
     * 预先扣除平分比赛分房费
     *
     * @param roomSportsPoint
     */
    public void beforehandRoomSportsPoint(Double roomSportsPoint) {
        this.roomSportsPoint = CommMath.subDouble(roomSportsPoint, this.beforehandRoomSportsPointConsume());
    }

    /**
     * 计算房间分数
     */
    public void calcRoomPoint(int point) {
        this.point += (point);
        // 计算房间竞技点
        this.calcRoomSportsPoint(point, 0);
        //BaseDao.testSys(getPosID()+"增加分数:"+point);
    }

    /**
     * 计算房间竞技点值
     *
     * @param roomSportsPoint
     */
    public void calcRoomSportsPoint(double roomSportsPoint, double zeroPoint) {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (room.calcFenUseYiKao()) {
                this.setRoomSportsPoint(CommMath.addDouble(this.getRoomSportsPointValue(), zeroPoint));
                return;
            }
            this.setRoomSportsPoint(CommMath.addDouble(this.getRoomSportsPointValue(), CommMath.mul(roomSportsPoint, Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble()))));

        }

    }

    public double getRoomSportsPointValue() {
        return CommMath.FormatDouble(roomSportsPoint);
    }

    public Double getRoomSportsPoint() {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            return CommMath.FormatDouble(roomSportsPoint);
        } else {
            return null;
        }
    }


    /**
     * 构造函数
     *
     * @param posID 位置
     * @param room  房间信息
     */
    public AbsRoomPos(int posID, AbsBaseRoom room) {
        super();
        this.setPosID(posID);
        this.setRoom(room);
    }

    /**
     * 清除玩家位置信息
     */
    public void roomPosClear() {
        // 清除状态
        this.clear();
        // 清除房间数据
        this.setRoom(null);
    }

    /**
     * 清除状态
     */
    public void clear() {
        this.setPid(0);
        this.isReady = false;
        this.setGameReady(false);
        this.setDissolveRoom(false);
        this.setLostConnect(false);
        this.setShowLeave(false);
        this.setTrusteeship(false);
        this.setPoint(0);
        this.setLatelyOutCardTime(0L);
        this.setGoldValue(0);
        this.setRobotInfo(null);
        this.setPlayer(null);
        this.setResults(null);
        this.setName(null);
        this.setHeadImageUrl(null);
        this.setSex(0);
        this.setPlayerIP(null);
        this.setAccountID(0L);
        this.setPlayerRoomAloneBO(null);
        this.setPartnerPid(0L);
        this.setClubMemberBO(null);
        this.setRoomSportsPoint(0D);
        this.setJoinPlayTime(0);
        this.setClubName(null);
        this.isPlaying = false;
        this.liuzuoSec = null;
    }

    /**
     * 位置准备 只能普通玩家操作
     *
     * @param pid          玩家PID
     * @param clubMemberBO 亲友圈成员
     * @return
     */
    public boolean seat(long pid, ClubMemberBO clubMemberBO) {
        return this.seat(pid, 0, false, clubMemberBO);
    }

    /**
     * 位置准备
     *
     * @param pid          玩家PID
     * @param initPoint    初始分数
     * @param isRobot      T:机器人,F:普通玩家
     * @param clubMemberBO 亲友圈成员
     * @return
     */
    public boolean seat(long pid, int initPoint, boolean isRobot, ClubMemberBO clubMemberBO) {
        return doSeat(pid, initPoint, isRobot, false, clubMemberBO);
    }

    /**
     * 方便重写-位置准备
     *
     * @param pid       玩家PID
     * @param initPoint 初始分数
     * @param isRobot   T:机器人,F:普通玩家
     * @return
     */
    public boolean doSeat(long pid, int initPoint, boolean isRobot, boolean isReady, ClubMemberBO clubMemberBO) {
        // 玩家信息中pid存在。则该位置有人。
        if (this.getPid() > 0L) {
            CommLogD.error("seat PID:{}", this.getPid());
            return false;
        }
        // 设置PID
        this.setPid(pid);
        // 设置合伙人Pid
        this.setPartnerPid(null == clubMemberBO ? 0L : clubMemberBO.getPartnerPid());
        // 设置是否机器人
        this.setRobot(isRobot);
        // 加入游戏时间
        this.setJoinPlayTime(CommTime.nowSecond());
        // 玩家初始分数
        if (initPoint > 0) {
            // 设置初始化分数
            this.setPoint(initPoint);
        }
        // 检查是否机器人
        if (isRobot) {
            // 设置机器人信息
            this.setRobotInfo(RobotMgr.getInstance().getRobot((int) pid));
            // 检查机器人信息是否存在
            if (null == this.getRobotInfo()) {
                // 找不到机器人数据.
                CommLogD.error("null == this.getRobotInfo() seat PID:{}", this.getPid());
                return false;
            }
            // 设置练习分数
            this.setGoldValue(this.getRobotInfo().getGold());
            this.setPoint(this.getRobotInfo().getGold());
            this.setReady(true);
        } else {
            // 设置玩家信息
            this.setPlayer(PlayerMgr.getInstance().getPlayer(pid));
            // 检查玩家信息是否存在
            if (null == this.getPlayer()) {
                // 找不到玩家数据.
                CommLogD.error("null == this.getPlayer() seat PID:{}", this.getPid());
                return false;
            }
            // 设置统计分数
            this.setCountPoint(this.countPoint());
            // 设置练习分数
            this.setGoldValue(this.getPlayer().getPlayerBO().getGold());
            if (PrizeType.Gold.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())) {
                // 练习场分数用金币填充
                this.setPoint(this.getPlayer().getPlayerBO().getGold());
            }
            // 保存亲友圈成员信息
            this.setClubMemberBO(clubMemberBO);
            // 设置房间竞技点
            this.beforehandRoomSportsPoint(this.getClubMemberSportsPoint());
            //设置坐下位置时候的竞技点
            this.beforehandSeatSportsPoint(this.getClubMemberSportsPoint());
            // 设置玩家信息
            this.playerRoomInfo();
            // 自动准备游戏 玩家加入房间时，自动进行准备（自动准备只能客户端进入场景后发送准备）
            this.setReady(isReady);
        }
        // 设置玩家信息
        this.setPlayerInfo();
        // 房间人员状态发生改变
        this.getRoom().getRoomTyepImpl().roomPlayerChange(this);
        // 房间内位置更新
        this.getRoom().getRoomPosMgr().notify2AllPublic(this.getRoom().PosUpdate(this.getRoom().getRoomID(), this.getPosID(), this.getNotify_PosInfo(), 0));
        //更新所有人计时时间
        if (this.getRoom().getRoomPosMgr().getEmptyPos() == null) {
            this.getRoom().getRoomPosMgr().setAllEnterTime(CommTime.nowSecond());
        }
        // 记录玩家属于哪个亲友圈
        this.setClubName();
        getRoom().getRoomPosMgr().removeWatch(pid);
        return true;
    }

    /**
     * 方便重写-位置准备
     *
     * @param pid 玩家PID
     * @return
     */
    public boolean doWatch(long pid, ClubMemberBO clubMemberBO) {
        // 玩家信息中pid存在。则该位置有人。
        if (this.getPid() > 0L) {
            CommLogD.error("seat PID:{}", this.getPid());
            return false;
        }
        // 设置PID
        this.setPid(pid);
        // 设置玩家信息
        this.setPlayer(PlayerMgr.getInstance().getPlayer(pid));
        // 检查玩家信息是否存在
        if (null == this.getPlayer()) {
            // 找不到玩家数据.
            CommLogD.error("null == this.getPlayer() seat PID:{}", this.getPid());
            return false;
        }
        // 设置玩家信息
        this.setPlayerInfo();
        // 房间内位置更新
//        this.getRoom().getRoomPosMgr().notify2All(this.getRoom().PosUpdate(this.getRoom().getRoomID(), this.getPosID(), this.getNotify_PosInfo(), 0));
        // 记录玩家属于哪个亲友圈
        this.setClubName();
        this.setClubMemberBO(clubMemberBO);
        return true;
    }

    /**
     * 记录玩家属于哪个亲友圈
     */
    public final void setClubName() {
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId());
            this.clubName = Objects.nonNull(club) ? club.getClubListBO().getName() : null;
            this.clubID = clubId();
            if (Objects.isNull(club)) {
                return;
            }
            ClubMember upMember;
            if (clubMemberBO.getUpLevelId() == 0) {
                this.upLevelName = club.getOwnerPlayer().getName();
            } else {
                if (Config.isShare()) {
                    upMember = ShareClubMemberMgr.getInstance().getClubMember(this.clubMemberBO.getUpLevelId());
                } else {
                    upMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubMemberBO.getUpLevelId());
                }
                if (Objects.isNull(upMember)) {
                    return;
                }
                SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(upMember.getClubMemberBO().getPlayerID());
                if (Objects.isNull(sharePlayer)) {
                    return;
                }
                this.upLevelName = sharePlayer.getPlayerBO().getName();
            }

        }
    }

    /**
     * 记录玩家房间信息
     */
    public void playerRoomInfo() {
        ClubMemberBO memberBO = this.getClubMemberBO();
        if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            // 联赛Id
            this.getPlayer().getRoomInfo().setUnionId(this.getRoom().getRoomTyepImpl().getSpecialRoomId());
            // 亲友圈Id
            this.getPlayer().getRoomInfo().setClubId(Objects.isNull(memberBO) ? 0L : memberBO.getClubID());
        } else if (RoomTypeEnum.CLUB.equals(this.getRoom().getRoomTypeEnum())) {
            // 亲友圈Id
            this.getPlayer().getRoomInfo().setClubId(Objects.isNull(memberBO) ? 0L : memberBO.getClubID());
        }
        // 设置配置Id
        this.getPlayer().getRoomInfo().setConfigId(this.getRoom().getRoomTyepImpl().getConfigId());
        // 设置房间类型
        this.getPlayer().getRoomInfo().setRoomTypeEnum(this.getRoom().getRoomTyepImpl().getRoomTypeEnum());
        // 记录房间密码
        this.getPlayer().getRoomInfo().setPasswordDES(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPassword());
    }


    /**
     * 练习场结算分
     */
    public void subGold() {
        // if(!PrizeType.Gold.equals(this.room.getPrizeType())) return;
        // if(CreateType.ARENA.equals(this.room.getCreateType())) return;
        this.setPoint(this.getPoint() - this.getGoldValue());
    }

    /**
     * 位置操作准备
     *
     * @param isReady T:准备成功,F:准备失败
     */
    public boolean setReady(boolean isReady) {
        if (this.isReady == isReady) {
            return false;
        }
        // 设置位置准备
        this.isReady = isReady;
        this.getRoom().getRoomPosMgr()
                .notify2All(this.getRoom().PosReadyChg(this.getRoom().getRoomID(), this.getPosID(), isReady));
        return true;
    }

    /**
     * 位置继续游戏操作
     */
    public void setContinue() {
        // 清除最近操作时间
        this.setLatelyOutCardTime(0L);
        setTimeSec(0);
        // 设置继续游戏
        this.setGameReady(true);
//        BaseDao.stackTrace();
        this.getRoom().getRoomPosMgr()
                .notify2All(this.getRoom().PosContinueGame(this.getRoom().getRoomID(), this.getPosID()));
    }

    /**
     * 玩家离开位置
     *
     * @param isKick T:踢出，F:主动离开
     * @param oID    操作人
     * @return
     */
    public boolean leave(boolean isKick, long oID) {
        return this.leave(isKick, oID, CKickOutType.None);
    }

    /**
     * 玩家离开位置
     *
     * @param isKick      T:踢出，F:主动离开
     * @param oID         操作人
     * @param kickOutType 默认\系统
     * @return
     */
    public boolean leave(boolean isKick, long oID, CKickOutType kickOutType) {
        return this.leave(isKick, oID, kickOutType, null);
    }

    /**
     * 玩家离开位置
     *
     * @param isKick      T:踢出，F:主动离开
     * @param oID         操作人
     * @param kickOutType 默认\系统
     * @return
     */
    public boolean leave(boolean isKick, long oID, CKickOutType kickOutType, String msg) {
        if (this.getPid() == 0) {
            return false;
        }
        this.getRoom().getRoomPosMgr().notify2AllPublic(this.getRoom().PosLeave(SBase_PosLeave.make(this.getRoom().getRoomID(), this.pid, this.getPosID(), isKick, oID, kickOutType.value(), msg)));
        if (null != this.getPlayer()) {
            // 房卡回退
            this.roomCardGiveBack();
            if (this.isLostConnect()) {
                this.setLostConnect(false);
                this.getRoom().getRoomPosMgr().notify2All(this.getRoom().LostConnect(this.getRoom().getRoomID(),
                        this.getPid(), this.isLostConnect(), this.isShowLeave()));
            }
            this.getPlayer().onExitRoom(this.room.getRoomID());
            setTrusteeship(false, false);
        }

        if (getRoom().getWinOrLose().containsKey(this.getPid())) {
            AbsRoomPos pos = getRoom().getRoomPosMgr().addAll();
            pos.setPid(pid);
            pos.setPlayer(player);
            pos.setClubMemberBO(clubMemberBO);
        }
        // 退出后重新加载坐位信息
        if (null != getRoom().getCurSet() && getRoom().getCurSet().getState() == SetState.Init) {
            getRoom().getCurSet().reInit();
        }
        this.clear();
        this.getRoom().getRoomTyepImpl().roomPlayerChange(this);
        if (this.getRoom().existLeaveClearAllPosReady()) {
            // 存在有玩家离开、踢出清空所有玩家准备状态
            this.getRoom().getRoomPosMgr().clearAllPosReady();
        }
        this.getRoom().getRoomPosMgr().notify2All(this.getRoom().PosUpdate(this.getRoom().getRoomID(), this.getPosID(), this.getNotify_PosInfo(), 0));
        //修改共享房间
        if (Config.isShare()) {
            int playerNumber = (int) this.getRoom().getRoomPosMgr().posList.stream().filter(k -> k.getPid() > 0).count();
            if (playerNumber > 0) {
                ShareRoomMgr.getInstance().addShareRoom(this.getRoom());
            }
        }
        return true;
    }

    /**
     * 房卡回退
     */
    public void roomCardGiveBack() {
        if (this.getRoom().getBaseRoomConfigure().getBaseCreateRoom()
                .getPaymentRoomCardType() == PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value()) {
            return;
        }
        this.getPlayer().getFeature(PlayerRoom.class).giveBack(this.getRoom().getBaseRoomConfigure().getGameType(),
                this.getRoom().getValueType(), this.getRoom().getSpecialRoomId(), this.getRoom().getRoomID(), 0);
    }

    /**
     * 获取指定玩家的定位信息
     *
     * @return
     */
    public LocationInfo getLocationInfo() {
        if (null != this.getPlayer()) {
            LocationInfo lInfo = this.getPlayer().getLocationInfo();
            lInfo.setPid(this.getPid());
            lInfo.setPos(this.getPosID());
            return lInfo;
        }
        return null;
    }

    /**
     * 设置托管状态
     *
     * @param isTrusteeship 托管状态
     * @param isOwn         是否屏蔽自己
     */
    public void setTrusteeship(boolean isTrusteeship, boolean isOwn) {
        if (this.isTrusteeship() == isTrusteeship || null != liuzuoSec) {
            return;
        }
        this.setTrusteeship(isTrusteeship);
        if (isOwn) {
            this.getRoom().getRoomPosMgr().notify2ExcludePosID(this.getPosID(), this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
        } else {
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
        }
    }

    /**
     * 获取玩家位置信息
     *
     * @return
     */
    public RoomPlayerPos roomPlayerPos() {
        RoomPlayerPos rPlayerPos = new RoomPlayerPos();
        rPlayerPos.setPid(this.getPid());
        rPlayerPos.setPos(this.getPosID());
        rPlayerPos.setIconUrl(this.getHeadImageUrl());
        rPlayerPos.setName(this.getName());
        rPlayerPos.setSex(this.getSex());
        rPlayerPos.setPoint(this.getPoint());
        rPlayerPos.setSportsPoint(this.sportsPoint());
        rPlayerPos.setClubName(this.getClubName());
        return rPlayerPos;
    }

    /**
     * 获取玩家信息
     *
     * @return
     */
    public ShortPlayer getShortPlayer() {
        if (this.isRobot) {
            ShortPlayer ret = new ShortPlayer();
            ret.setName(this.robot.getName());
            ret.setPid(getPid());
            ret.setIconUrl(this.robot.getUrl());
            ret.setAccountID(getPid());
            return ret;
        } else {
            return this.player.getShortPlayer();
        }
    }

    public PlayerPosInfo getPlayerPosInfo() {
        return new PlayerPosInfo(this.getPid(), this.getPoint(), this.getPosID(), this.getRoomSportsPoint());
    }

    /**
     * 新房间位置信息
     *
     * @return
     */
    public RoomPosInfo newRoomPosInfo() {
        return new RoomPosInfo();
    }

    /**
     * 当前房间玩家的数据信息。
     *
     * @return
     */
    public RoomPosInfo getRoomPosInfo() {
        RoomPosInfo tmPos = this.newRoomPosInfo();
        tmPos.setPid(this.pid);
        tmPos.setPos(this.posID);
        tmPos.setLostConnect(this.isLostConnect);
        tmPos.setGameReady(this.isGameReady);
        tmPos.setRoomReady(this.isReady);
        tmPos.setPoint(this.point);
        tmPos.setGold(this.goldValue);
        tmPos.setLostConnect(this.isLostConnect);
        tmPos.setShowLeave(this.isShowLeave);
        tmPos.setPlaying(this.isPlayTheGame);
        tmPos.setAccountID(this.accountID);
        tmPos.setName(this.name);
        tmPos.setSex(this.sex);
        tmPos.setHeadImageUrl(this.headImageUrl);
        tmPos.setPlayerIP(this.playerIP);
        tmPos.setSportsPoint(this.getRoomSportsPoint());
        Double realPoint = this.sportsPoint();
        tmPos.setRealPoint(realPoint == null ? this.point : realPoint);
        tmPos.setTrusteeship(this.isTrusteeship());
        if (null != liuzuoSec) {
            tmPos.setLiuzuoSec(120 - CommTime.nowSecond() + liuzuoSec);
        }
        return tmPos;
    }

    /**
     * 当前房间玩家的数据信息。
     *
     * @return
     */
    public RoomPosInfo getNotify_PosInfo() {
        RoomPosInfo tmPos = this.getRoomPosInfo();
        return tmPos;
    }

    public RoomPosInfoShort getRoomPosInfoShort() {
        RoomPosInfoShort tmPos = new RoomPosInfoShort();
        tmPos.setPid(this.pid);
        tmPos.setPos(this.posID);
        tmPos.setLostConnect(this.isLostConnect);
        tmPos.setRoomReady(this.isReady);
        tmPos.setLostConnect(this.isLostConnect);
        tmPos.setName(this.name);
        tmPos.setHeadImageUrl(this.headImageUrl);
        tmPos.setClubName(this.clubName);
        tmPos.setUpLevelName(this.upLevelName);
        tmPos.setClubID(this.clubID);
        return tmPos;
    }

    /**
     * 设置玩家信息
     */
    protected void setPlayerInfo() {
        if (this.isRobot()) {
            if (null != this.getRobotInfo()) {
                this.setName(this.getRobotInfo().getName());
                this.setHeadImageUrl(this.getRobotInfo().getUrl());
                this.setSex(this.getRobotInfo().getSex());
                this.setAccountID(this.getRobotInfo().getPid());
            }
        } else {
            if (null != this.getPlayer()) {
                this.setName(this.getPlayer().getName());
                this.setHeadImageUrl(this.getPlayer().getHeadImageUrl());
                this.setSex(this.getPlayer().getPlayerBO().getSex());
                this.setAccountID(this.getPlayer().getAccountID());
                this.setPlayerIP(this.getPlayer().getIp());
            }
        }
    }

    public Robot getRobotInfo() {
        return this.robot;
    }

    public void setRobotInfo(Robot robot) {
        this.robot = robot;
    }

    /**
     * 重新设置PosID
     *
     * @param posID
     */
    public void resetPosID(int posID) {
        this.setPosID(posID);
        this.getRoom().getRoomPosMgr().notify2All(
                this.getRoom().PosUpdate(this.getRoom().getRoomID(), this.getPosID(), this.getNotify_PosInfo(), 0));
    }

    /**
     * 统计分数
     *
     * @return
     */
    public int countPoint() {
        return 0;
    }

    /**
     * 获取统计分数
     *
     * @return
     */
    public int getCountPoint() {
        return countPoint;
    }

    /**
     * 设置统计分数
     *
     * @param countPoint
     */
    public void addCountPoint(int countPoint) {
        // 不作处理
    }

    /**
     * 设置是否离线
     *
     * @param isLostConnect T: 离线，F:正常
     */
    public void setLostConnect(boolean isLostConnect) {
        this.isLostConnect = isLostConnect;
        // 玩家离线 == 显示离开，玩家正常 == 显示正常
        this.setShowLeave(isLostConnect);
    }


    /**
     * 检查自动准备游戏超时
     */
    public void autoReadyGameOvertime() {
        if (this.isReady() || this.getPid() <= 0L) {
            // 已经准备了不用管
            return;
        }
        // 当前时间和进入房间时间相差 大于10秒
        if (CommTime.nowSecond() - this.getJoinPlayTime() >= 10) {
            this.setReady(true);
        }
    }


    public long getTimeSec() {
        return timeSec;
    }

    public void setTimeSec(long timeSec) {
        this.timeSec = timeSec;
    }

    public int getDissolveCount() {
        return dissolveCount;
    }

    public void addDissolveCount() {
        this.dissolveCount++;
    }

    public void setDissolveCount(int dissolveCount) {
//        this.dissolveCount = dissolveCount;
    }

    public void setDissolveCount1(int dissolveCount) {
        this.dissolveCount = dissolveCount;
    }

    /**
     * 初始化并返回结果
     *
     * @param result 结果
     * @return {@link T}
     */
    public <T extends BaseResults> T initAndReturnResult(T result) {
        if (Objects.isNull(getResults())) {
            this.setResults(result);
        }
        return (T) getResults();
    }

    @Override
    public String toString() {
        return "AbsRoomPos [posID=" + posID + ", pid=" + pid + ", accountID=" + accountID + "]";
    }

    public Long getUpLevelId() {
        if (!room.isNeedPromotionUpLevelID()) {
            return null;
        }
        AbsBaseRoom room = this.getRoom();
        //是亲友圈房间或者联赛房间
        if (RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum()) || RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            long upLevelId = this.upLevelId();
            ClubMember upClubMember;
            //如果有上级id的话 就去找
            if (upLevelId > 0L) {
                if (Config.isShare()) {
                    upClubMember = ShareClubMemberMgr.getInstance().getClubMember(upLevelId);
                } else {
                    upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(upLevelId);
                }
                if (Objects.nonNull(upClubMember)) {
                    return upClubMember.getClubMemberBO().getPlayerID();
                }
            } else {
                //没有上级id  找亲友圈创建者id
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(room.getBaseRoomConfigure().getBaseCreateRoom().getClubId());
                if (Objects.nonNull(club)) {
                    return club.getOwnerPlayer().getPid();
                }
            }
        }
        return 0L;
    }

    /**
     * 获取房间真正计算比赛分的结果
     * 坐下时分数 减去房间内的其他消耗(正数)
     *
     * @return
     */
    public Double getCalcSportPoint() {
        return this.getSeatSportsPoint();
        //取消减掉其他消耗
//        return this.getSeatSportsPoint()-this.getOtherSportsPointConsume();
    }


    public boolean setPlayerIP(String playerIP) {
        if (StringUtils.isNotEmpty(playerIP) && playerIP.equals(this.playerIP)) {
            return false;
        }
        // 重置成功
        this.playerIP = playerIP;
        return true;
    }
}
