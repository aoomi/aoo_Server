package core.db.entity.clarkGame;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.rocketmq.bo.MqClubMemberUpdateNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ConstEnum.ResOpType;
import cenum.ItemFlow;
import cenum.RoomTypeEnum;
import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.Config;
import com.ddm.server.common.lock.AtomicBooleanLock;
import com.ddm.server.common.redis.DistributedRedisLock;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.BaseEntity;
import core.db.entity.clarkLog.XiPaiLogFlow;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberLatylyConfigIDBOService;
import core.db.service.clarkLog.XiPaiLogFlowService;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.union.ClubNotify2AllByManageEvent;
import core.dispatch.event.union.UnionNotify2AllByManageEvent;
import core.dispatch.event.union.UnionNotify2PlayerExecSportsPointEvent;
import core.dispatch.event.union.UnionNotify2PlayerSportsPointEvent;
import core.ioc.Constant;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.ClubMemberLatelyConfigIDItem;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionApplyOperateItem;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.SClub_MemberInfoChange;
import jsproto.c2s.iclass.union.SUnion_MemberInfoChange;
import jsproto.c2s.iclass.union.SUnion_SportsPoint;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@TableName(value = "dbClubmember")
@Data
public class ClubMemberBO extends BaseEntity<ClubMemberBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    // 俱乐部编号
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    // 玩家游戏ID 长的
    @DataBaseField(type = "bigint(20)", fieldname = "playerID", comment = "玩家游戏短ID 长的")
    private long playerID;
    // 0x01未批准,0x02已拒绝,0x04为已加入,0x08为已踢出,0x10已邀请,0x20邀请被拒
    @DataBaseField(type = "int(4)", fieldname = "status", comment = "0x01未批准,0x02已拒绝加入,0x04为已加入,0x08为已踢出,0x10为已邀请,0x20为拒绝邀请,0x40已退出")
    private int status;
    @DataBaseField(type = "int(4)", fieldname = "isminister", comment = "职务 0普通会员 1管理 2创建者 3赛事管理员")
    private int isminister;// 是否是管理,不是为null,是为1
    @DataBaseField(type = "int(11)", fieldname = "creattime", comment = "申请时间")
    private int creattime;// 申请时间
    @DataBaseField(type = "int(11)", fieldname = "updatetime", comment = "处理时间")
    private int updatetime;// 处理时间
    @DataBaseField(type = "int(11)", fieldname = "deletetime", comment = "踢出时间")
    private int deletetime;// 踢出时间
    @DataBaseField(type = "int(11)", fieldname = "clubRoomCard", comment = "玩家俱乐部消耗的房卡")
    private int clubRoomCard;// 俱乐部房卡
    @DataBaseField(type = "varchar(15)", fieldname = "changeRoomCardTime", comment = "玩家俱乐部消耗的房卡时间")
    private String changeRoomCardTime = "";// 俱乐部改变房卡时间
    @DataBaseField(type = "varchar(15)", fieldname = "Image", comment = "玩家俱乐部上传的图片")
    private String Image = "";// 玩家俱乐部上传的图片
    @DataBaseField(type = "int(2)", fieldname = "banGame", comment = "禁止游戏（0:正常,1:禁止游戏）")
    private int banGame;// 禁止游戏
    @DataBaseField(type = "int(2)", fieldname = "partner", comment = "合作伙伴（0:不是,1:合作伙伴）")
    private int partner;// 合作伙伴
    @DataBaseField(type = "bigint(20)", fieldname = "partnerPid", comment = "合作伙伴PID")
    private long partnerPid;//
    @DataBaseField(type = "int(11)", fieldname = "topTime", comment = "置顶时间")
    private int topTime;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPoint", comment = "比赛分")
    private double sportsPoint;
    @DataBaseField(type = "int(11)", fieldname = "fatigueTime", comment = "比赛分时间")
    private int fatigueTime;
    @DataBaseField(type = "bigint(20)", fieldname = "invitationPid", comment = "发送邀请的玩家Pid")
    private long invitationPid;
    @DataBaseField(type = "varchar(500)", fieldname = "unionNotGames", comment = "赛事非勾选游戏列表")
    private String unionNotGames = "";
    private List<Long> unionNotGameList = null;
    @DataBaseField(type = "int(2)", fieldname = "isHideStartRoom", comment = "是否隐藏开始的房间(0:否,1:是)")
    private int isHideStartRoom;
    @DataBaseField(type = "double(11,2)", fieldname = "scorePoint", comment = "分数-收益总分数")
    private double scorePoint;
    @DataBaseField(type = "int(11)", fieldname = "scorePercent", comment = "分数百分比")
    private int scorePercent;
    @DataBaseField(type = "int(11)", fieldname = "unionBanGame", comment = "赛事禁止游戏（0:正常,1:禁止游戏）")
    private int unionBanGame;
    @DataBaseField(type = "double(11,2)", fieldname = "scoreDividedInto", comment = "分数分成值")
    private double scoreDividedInto;
    @DataBaseField(type = "int(2)", fieldname = "unionState", comment = "比赛匹配状态(0:初始状态,1:比赛进行中,2:复赛申请中,3:退赛申请中)")
    private int unionState;
    @DataBaseField(type = "int(11)", fieldname = "resetTime", comment = "重置比赛匹配状态时间")
    private int resetTime;
    @DataBaseField(type = "varchar(50)", fieldname = "rankingReward", comment = "排名奖励")
    private String rankingReward = "";
    @DataBaseField(type = "int(11)", fieldname = "roundId", comment = "回合Id")
    private int roundId;
    @DataBaseField(type = "int(2)", fieldname = "promotion", comment = "推广员状态(0不是推广员,1任命,2卸任)")
    private int promotion;
    @DataBaseField(type = "double(11,2)", fieldname = "calcActive", comment = "计算活跃值")
    private double calcActive;
    @DataBaseField(type = "double(11,2)", fieldname = "activePoint", comment = "当前活跃值")
    private double activePoint;
    @DataBaseField(type = "double(11,2)", fieldname = "sumActivePoint", comment = "总活跃值")
    private double sumActivePoint;
    @DataBaseField(type = "double(11,2)", fieldname = "dayActivePoint", comment = "天活跃值")
    private double dayActivePoint;
    @DataBaseField(type = "int(11)", fieldname = "level", comment = "等级(0:默认普通成员)")
    private int level;
    @DataBaseField(type = "bigint(20)", fieldname = "upLevelId", comment = "上个等级id")
    private long upLevelId;//
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "最近配置Id")
    private long configId;
    @DataBaseField(type = "double(11,2)", fieldname = "shareValue", comment = "代理分成百分比值")
    private double shareValue;
    @DataBaseField(type = "double(11,2)", fieldname = "shareFixedValue", comment = "代理分成固定值")
    private double shareFixedValue;
    @DataBaseField(type = "int(11)", fieldname = "shareType", comment = "代理分成类型")
    private int shareType;
    private double sportsPointTemp;// 比赛分临时存放点
    @DataBaseField(type = "int(2)", fieldname = "promotionManage", comment = "推广员管理（0:不是,1:是）")
    private int promotionManage;
    @DataBaseField(type = "int(2)", fieldname = "kicking", comment = "踢人（0:不允许,1:允许）")
    private int kicking;
    @DataBaseField(type = "int(2)", fieldname = "modifyValue", comment = "从属修改（0:不允许,1:允许）")
    private int modifyValue = 1;
    @DataBaseField(type = "int(2)", fieldname = "showShare", comment = "显示分成（0:不允许,1:允许）")
    private int showShare;
    @DataBaseField(type = "int(2)", fieldname = "warnStatus", comment = "预警状态（0:不预警,1:预警）")
    private int warnStatus;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPointWarning", comment = "预警值")
    private double sportsPointWarning;
    @DataBaseField(type = "int(2)", fieldname = "personalWarnStatus", comment = "个人预警状态（0:不预警,1:预警）")
    private int personalWarnStatus = 0;
    @DataBaseField(type = "double(11,2)", fieldname = "personalSportsPointWarning", comment = "个人预警值")
    private double personalSportsPointWarning = 0;
    @DataBaseField(type = "double(11,2)", fieldname = "caseSportsPoint", comment = "保险箱比赛分")
    private double caseSportsPoint;
    @DataBaseField(type = "int(2)", fieldname = "invite", defaultValue = "1", comment = "邀请（0:不允许,1:允许）")
    private int invite = 1;
    @DataBaseField(type = "double(11,2)", fieldname = "eliminatePoint", comment = "个人淘汰分")
    private double eliminatePoint = 0d;
    @DataBaseField(type = "double(11,2)", fieldname = "alivePoint", comment = "生存积分")
    private double alivePoint = 0d;
    @DataBaseField(type = "int(2)", fieldname = "alivePointStatus", comment = "生存积分状态（0:不开启,1:开启）")
    private int alivePointStatus = 0;
    @DataBaseField(type = "int(11)", fieldname = "levelZhongZhi", comment = "中至等级(显示用 无意义)")
    private int levelZhongZhi = 0;
    /**
     * 比赛分竞争锁
     */
    private AtomicBooleanLock booleanLock = new AtomicBooleanLock();

    /**
     * 活跃值竞争锁
     */
    private AtomicBooleanLock activeLock = new AtomicBooleanLock();


    public ClubMemberBO() {
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `dbClubmember` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部编号',"
                + "`playerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家游戏长ID',"
                + "`status` int(4) NOT NULL DEFAULT '0' COMMENT '玩家状态 0x01未批准,0x02已拒绝加入,0x04为已加入,0x08为已踢出,0x10为已邀请,0x20为拒绝邀请,0x40已退出',"
                + "`isminister` int(4) NOT NULL DEFAULT 0  COMMENT '职务 0普通会员 1管理 2创建者',"
                + "`creattime` int(11) NOT NULL DEFAULT '0'  COMMENT '申请时间',"
                + "`updatetime` int(11) NOT NULL DEFAULT '0'  COMMENT '处理时间',"
                + "`deletetime` int(11) NOT NULL DEFAULT '0'  COMMENT '踢出时间',"
                + "`clubRoomCard` int(11) NOT NULL DEFAULT '0' COMMENT '玩家俱乐部消耗的房卡',"
                + "`changeRoomCardTime` varchar(15) NOT NULL DEFAULT ''  COMMENT '玩家俱乐部消耗的房卡时间',"
                + "`Image` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '玩家俱乐部上传的图片',"
                + "`banGame` int(2) NOT NULL DEFAULT 0  COMMENT '禁止游戏（0:正常,1:禁止游戏）',"
                + "`partner` int(2) NOT NULL DEFAULT 0  COMMENT '合作伙伴（0:不是,1:合作伙伴）',"
                + "`partnerPid` bigint(20) NOT NULL DEFAULT 0  COMMENT '合作伙伴PID',"
                + "`topTime` int(11) NOT NULL DEFAULT '0'  COMMENT '置顶时间',"
                + "`sportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '比赛分',"
                + "`fatigueTime` int(11) NOT NULL DEFAULT '0'  COMMENT '比赛分时间',"
                + "`invitationPid` bigint(20) NOT NULL DEFAULT 0  COMMENT '发送邀请的玩家Pid',"
                + "`unionNotGames` varchar(500) NOT NULL DEFAULT ''  COMMENT '赛事非勾选游戏列表',"
                + "`isHideStartRoom` int(2) NOT NULL DEFAULT 0  COMMENT '是否隐藏开始的房间(0:否,1:是)',"
                + "`scorePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分数-收益总分数',"
                + "`scorePercent` int(11) NOT NULL DEFAULT 0  COMMENT '分数百分比',"
                + "`unionBanGame` int(2) NOT NULL DEFAULT 0  COMMENT '赛事禁止游戏（0:正常,1:禁止游戏）',"
                + "`scoreDividedInto` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分数分成值',"
                + "`unionState` int(2) NOT NULL DEFAULT 1 COMMENT '比赛匹配状态(0:初始状态,1:比赛进行中,2:复赛申请中,3:退赛申请中)',"
                + "`resetTime` int(11) NOT NULL DEFAULT 0  COMMENT '重置比赛匹配状态时间',"
                + "`rankingReward` varchar(50) NOT NULL DEFAULT ''  COMMENT '排名奖励',"
                + "`roundId` int(11) NOT NULL DEFAULT '0'  COMMENT '回合Id',"
                + "`promotion` int(2) NOT NULL DEFAULT '0'  COMMENT '推广员状态',"
                + "`calcActive` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '计算活跃值',"
                + "`activePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '当前活跃值',"
                + "`sumActivePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '总活跃值',"
                + "`dayActivePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '天活跃值',"
                + "`shareValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '代理分成百分值',"
                + "`shareFixedValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '代理分成固定值',"
                + "`shareType` int(2) NOT NULL DEFAULT '0'  COMMENT '代理分成类型',"
                + "`level` int(11) NOT NULL DEFAULT 0  COMMENT '等级',"
                + "`levelZhongZhi` int(11) NOT NULL DEFAULT 0  COMMENT '中至等级(显示用 无意义)',"
                + "`upLevelId` bigint(20) NOT NULL DEFAULT 0  COMMENT '上个等级id',"
                + "`promotionManage` int(2) NOT NULL DEFAULT 0  COMMENT '推广员管理（0:不是,1:是）',"
                + "`kicking` int(2) NOT NULL DEFAULT 0  COMMENT '踢人（0:不允许,1:允许）',"
                + "`modifyValue` int(2) NOT NULL DEFAULT 0  COMMENT '从属修改（0:不允许,1:允许）',"
                + "`showShare` int(2) NOT NULL DEFAULT 0  COMMENT '显示分成（0:不允许,1:允许）',"
                + "`warnStatus` int(2) NOT NULL DEFAULT 0  COMMENT '预警状态（0:不预警,1:预警）',"
                + "`sportsPointWarning` double(11,2) NOT NULL DEFAULT 0  COMMENT '预警值',"
                + "`personalWarnStatus` int(2) NOT NULL DEFAULT 0  COMMENT '个人预警状态（0:不预警,1:预警）',"
                + "`personalSportsPointWarning` double(11,2) NOT NULL DEFAULT 0  COMMENT '个人预警值',"
                + "`caseSportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '保险箱比赛分',"
                + "`invite` int(2) NOT NULL DEFAULT 1  COMMENT '邀请（0:不允许,1:允许）',"
                + "`eliminatePoint` double(11,2) NOT NULL DEFAULT 0  COMMENT '个人淘汰分',"
                + "`alivePoint` double(11,2) NOT NULL DEFAULT 0  COMMENT '生存积分',"
                + "`alivePointStatus` int(2) NOT NULL DEFAULT 1  COMMENT '生存积分状态（0:不开启,1:开启）',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `c_p_id` (`clubID`,`playerID`) USING BTREE,"
                + "KEY `cs` (`clubID`,`status`) USING BTREE"
                + ") COMMENT='亲友圈成员表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    public void saveRankingReward(String rankingReward) {
        this.rankingReward = rankingReward;
        getBaseService().update("rankingReward", rankingReward, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "rankingReward");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveAlivePointStatus(int alivePointStatus) {
        if (alivePointStatus == this.alivePointStatus) {
            return;
        }
        this.alivePointStatus = alivePointStatus;
        getBaseService().update("alivePointStatus", alivePointStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubMemberMgr.getInstance().updateField(this, "alivePointStatus");
        }
    }

    /**
     * 更新回合id
     *
     * @param roundId 回合id
     */
    public void saveRoundId(int roundId) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        if (roundId != this.roundId) {
            this.roundId = roundId;
            map.put("roundId", roundId);
            this.rankingReward = "";
            map.put("rankingReward", this.rankingReward);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "roundId", "rankingReward");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }


    }

    public int getUnionState() {
        if (unionState <= UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value()) {
            return UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
        }
        return unionState;
    }

    public void setUnionState(int unionState) {
        this.unionState = unionState;
    }

    public int getUnionState(double outSportsPoint, long unionId) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            outSportsPoint = this.getEliminatePoint();
        }
        // 检查比赛场状态
        unionState = unionState <= UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value() ? UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value() : unionState;
        if (UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value() == unionState && this.getSportsPoint() < outSportsPoint) {
            // 比赛状态中并且身上比赛分 < 淘汰分，直接转入复赛申请中
            unionState = UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value();
            getBaseService().update("unionState", unionState, id, new AsyncInfo(getId()));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "unionState");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        } else if (UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value() == unionState && this.getSportsPoint() >= outSportsPoint) {
            // 比赛状态中并且身上比赛分 >= 淘汰分，直接转入比赛进行中
            unionState = UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
            getBaseService().update("unionState", unionState, id, new AsyncInfo(getId()));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "unionState");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }
        return unionState;
    }

    public int getResetTime() {
        return resetTime;
    }

    public void updateStatus() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("status", this.getStatus());
        map.put("creattime", this.getCreattime());
        map.put("updatetime", this.getUpdatetime());
        map.put("deletetime", this.getDeletetime());
        map.put("clubRoomCard", this.getClubRoomCard());
        this.getBaseService().update(map, getId());
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, map);
            //推送到MQ
            MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveUnionState(int unionState) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        if (unionState != this.unionState) {
            this.unionState = unionState;
            map.put("unionState", unionState);
        }
        this.resetTime = CommTime.nowSecond();
        map.put("resetTime", this.resetTime);
        getBaseService().update(map, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, map);
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }


    public void saveCaseSportsPoint(int caseSportsPoint) {
        if (caseSportsPoint == this.caseSportsPoint) {
            return;
        }
        this.caseSportsPoint = caseSportsPoint;
        getBaseService().update("caseSportsPoint", caseSportsPoint, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "caseSportsPoint");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveInvite(int invite) {
        this.invite = invite;
        getBaseService().update("invite", invite, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "invite");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveTopTime(int topTime) {
        if (topTime == this.topTime) {
            return;
        }
        this.topTime = topTime;
        getBaseService().update("topTime", topTime, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "topTime");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveWarnStatus(int warnStatus) {
        if (warnStatus == this.warnStatus) {
            return;
        }
        this.warnStatus = warnStatus;
        getBaseService().update("warnStatus", warnStatus, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "warnStatus");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void savePersonalWarnStatus(int personalWarnStatus) {
        if (personalWarnStatus == this.personalWarnStatus) {
            return;
        }
        this.personalWarnStatus = personalWarnStatus;
        getBaseService().update("personalWarnStatus", personalWarnStatus, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "personalWarnStatus");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void savePersonalSportsPointWarning(double personalSportsPointWarning) {
        if (personalSportsPointWarning == this.personalSportsPointWarning) {
            return;
        }
        this.personalSportsPointWarning = personalSportsPointWarning;
        getBaseService().update("personalSportsPointWarning", personalSportsPointWarning, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "personalSportsPointWarning");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveSportsPointWarning(double sportsPointWarning) {
        if (sportsPointWarning == this.sportsPointWarning) {
            return;
        }
        this.sportsPointWarning = sportsPointWarning;
        getBaseService().update("sportsPointWarning", sportsPointWarning, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "sportsPointWarning");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void savePromotionManage(int promotionManage) {
        if (promotionManage == this.promotionManage) {
            return;
        }
        this.promotionManage = promotionManage;
        getBaseService().update("promotionManage", promotionManage, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "promotionManage");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveEliminatePoint(double eliminatePoint) {
        if (eliminatePoint == this.eliminatePoint) {
            return;
        }
        this.eliminatePoint = eliminatePoint;
        getBaseService().update("eliminatePoint", eliminatePoint, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "eliminatePoint");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveAlivePoint(double alivePoint) {
        if (alivePoint == this.alivePoint) {
            return;
        }
        this.alivePoint = alivePoint;
        getBaseService().update("alivePoint", alivePoint, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "alivePoint");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveStatus(int status) {
        if (status == this.status) {
            return;
        }
        this.status = status;
        getBaseService().update("status", status, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "status");
            //推送到MQ
            MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveShareType(int shareType) {
        //亲友圈可能存在同时储存 先取消这个判断
//        if (shareType == this.shareType) {
//            return;
//        }
        this.shareType = shareType;
        getBaseService().update("shareType", shareType, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "shareType");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveShareFixedValue(double shareFixedValue) {
        if (shareFixedValue == this.shareFixedValue) {
            return;
        }
        this.shareFixedValue = shareFixedValue;
        getBaseService().update("shareFixedValue", shareFixedValue, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "shareFixedValue");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveShareValue(double shareValue) {
        if (shareValue == this.shareValue) {
            return;
        }
        this.shareValue = shareValue;
        getBaseService().update("shareValue", shareValue, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "shareValue");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveIsminister(int isminister) {
        if (isminister == this.isminister) {
            return;
        }
        this.isminister = isminister;
        getBaseService().update("isminister", isminister, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "isminister");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveConfigId(long configId, long unionId) {
        //保存最近的配置
        ClubMemberLatelyConfigIDItem item = ContainerMgr.get().getComponent(ClubMemberLatylyConfigIDBOService.class).getDefaultDao().findOne(Restrictions.and(Restrictions.eq("unionID", unionId), Restrictions.eq("clubID", clubID), Restrictions.eq("memberID", id), Restrictions.eq("configID", configId)), ClubMemberLatelyConfigIDItem.class, ClubMemberLatelyConfigIDItem.getItemsNameUid());
        ClubMemberLatelyConfigIdBO bo = new ClubMemberLatelyConfigIdBO();
        if (Objects.nonNull(item)) {
            bo.setId(item.getId());
        }
        bo.setClubID(clubID);
        bo.setConfigID(configId);
        bo.setMemberID(id);
        bo.setUnionID(unionId);
        bo.setUpdateTime(CommTime.nowSecond());
        bo.setStartTime(CommTime.nowSecond());
        ContainerMgr.get().getComponent(ClubMemberLatylyConfigIDBOService.class).saveOrUpDate(bo);
        if (this.configId == configId) {
            return;
        }
        this.configId = configId;
        getBaseService().update("configId", configId, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "configId");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void savePromotion(int promotion, long partnerPid) {
        HashMap<String, Object> _fieldMap = new HashMap<>(3);
        this.promotion = promotion;
        this.partnerPid = partnerPid;
        _fieldMap.put("promotion", promotion);
        _fieldMap.put("partnerPid", partnerPid);
        getBaseService().update(_fieldMap, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, _fieldMap);
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void savePartnerPid(long partnerPid) {
        if (partnerPid == this.partnerPid) {
            return;
        }
        this.partnerPid = partnerPid;
        getBaseService().update("partnerPid", partnerPid, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "partnerPid");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }

    }

    /**
     * 邀请
     *
     * @param invitationPid 邀请pid
     */
    public void saveInvitationPid(long invitationPid) {
        if (invitationPid == this.invitationPid) {
            return;
        }
        this.invitationPid = invitationPid;
        getBaseService().update("invitationPid", invitationPid, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "invitationPid");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    /**
     * 游戏列表字符串转列表
     */
    public void unionNotGameListStr2List() {
        // 初始化
        if (null == this.getUnionNotGameList()) {
            if (StringUtils.isEmpty(this.getUnionNotGames())) {
                this.setUnionNotGameList(new ArrayList<>());
            } else {
                this.setUnionNotGameList(new Gson().fromJson(this.getUnionNotGames(), new TypeToken<List<Long>>() {
                }.getType()));
            }
        } else {
            // 设置赛事游戏列表字符串
            this.setUnionNotGames(null);
        }
    }

    /**
     * 保存赛事非勾选游戏列表
     */
    public void saveUnionNotGameList(List<Long> unionNotGameList) {
        this.unionNotGameListStr2List();
        if (CollectionUtils.isNotEmpty(unionNotGameList) && this.getUnionNotGameList().size() == unionNotGameList.size() && this.getUnionNotGameList().containsAll(unionNotGameList)) {
            // 长度一样 && 值一样
            return;
        }
        this.setUnionNotGameList(unionNotGameList.stream().distinct().collect(Collectors.toList()));
        getBaseService().update("unionNotGames", this.getUnionNotGameList().toString(), id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "unionNotGames");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public double getCalcActive() {
        return CommMath.FormatDouble(this.calcActive);
    }

    public void saveCalcActive(double calcActive) {
        final double finalValue = CommMath.FormatDouble(calcActive);

        if (finalValue == this.calcActive) {
            return;
        }
        this.calcActive = finalValue;
        getBaseService().update("calcActive", finalValue, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "calcActive");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public double getActivePoint() {
        return CommMath.FormatDouble(this.activePoint);
    }

    public double getSumActivePoint() {
        return CommMath.FormatDouble(this.sumActivePoint);
    }

    public double getDayActivePoint() {
        return CommMath.FormatDouble(this.dayActivePoint);
    }

    /**
     * 保存等级
     *
     * @param level
     */
    public void saveLevel(int level) {
        if (this.level == level) {
            return;
        }
        int oldLevel = this.level;
        this.setLevel(level);
        getBaseService().update("level", level, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "level");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            // 移除亲友圈推广员列表变更的缓存数据
            this.removeClubPromotionLevelCacheKey(this.upLevelId, oldLevel);
        }
    }

    /**
     * 保存等级
     *
     * @param levelZhongZhi
     */
    public void saveLevelZhongZhi(int levelZhongZhi) {
        if (this.levelZhongZhi == levelZhongZhi) {
            return;
        }
        this.setLevelZhongZhi(levelZhongZhi);
        getBaseService().update("levelZhongZhi", levelZhongZhi, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "levelZhongZhi");

        }
    }

    /**
     * 保存等级
     *
     * @param upLevelId
     */
    public void saveUpLevelId(long upLevelId) {
        if (this.upLevelId == upLevelId) {
            return;
        }
        long oldUpLevelId = this.upLevelId;
        this.setUpLevelId(upLevelId);
        getBaseService().update("upLevelId", upLevelId, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateUpLevelId(this.getId(), upLevelId, this.level);
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            // 移除亲友圈推广员列表变更的缓存数据
            this.removeClubPromotionLevelCacheKey(oldUpLevelId, this.level);
        }
    }

    /**
     * 修改等级id和上级推广员id
     *
     * @param upLevelId
     */
    public void saveLevelAndUpLevelId(int level, long upLevelId) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        int oldLevel = this.level;
        long oldUpLevelId = this.upLevelId;
        if (this.upLevelId != upLevelId) {
            this.setUpLevelId(upLevelId);
            map.put("upLevelId", upLevelId);
        }
        if (this.level != level) {
            this.setLevel(level);
            map.put("level", level);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //单独执行uplevelId
                ShareClubMemberMgr.getInstance().updateUpLevelId(this.getId(), upLevelId, level);
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
                // 移除亲友圈推广员列表变更的缓存数据
                this.removeClubPromotionLevelCacheKey(oldUpLevelId, oldLevel);
            }
        }
    }

    /**
     * 移除亲友圈推广员列表变更的缓存数据
     *
     * @return
     */
    private void removeClubPromotionLevelCacheKey(long oldUpLevelId, int oldLevel) {
        String cacheKey = null;
        if (Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == this.getIsminister()) {
            cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_GENERAL.getKey(CommTime.getNowTimeStringYMD(), clubID, oldUpLevelId, id, oldLevel);
        } else {
            cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_LEVEL.getKey(CommTime.getNowTimeStringYMD(), clubID, oldUpLevelId, id, oldLevel);
        }
        ContainerMgr.get().getRedis().remove(cacheKey);
    }

    /**
     * 赛事房间是否通知
     *
     * @return T:通知,F:不通知
     */
    public boolean isUnionNotify2Room(long unionGameCfgId) {
        this.unionNotGameListStr2List();
        return !this.getUnionNotGameList().contains(unionGameCfgId);
    }


    /**
     * 保存分数百分比
     *
     * @param scorePercent 分数百分比
     */
    public void saveScorePercent(long unionId, int scorePercent) {
        if (this.scorePercent == scorePercent) {
            return;
        }
        this.scorePercent = scorePercent;
        getBaseService().update("scorePercent", scorePercent, id, new AsyncInfo(id));
        DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.clubID, this.getSportsPoint(), this.getScorePercent(), this.getPlayerID())));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "scorePercent");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }


    /**
     * 保存分数百分比
     *
     * @param scoreDividedInto 分数百分比
     */
    public void saveScoreDividedInto(long unionId, double scoreDividedInto) {
        final double finalValue = CommMath.FormatDouble(scoreDividedInto);
        if (this.scoreDividedInto == finalValue) {
            return;
        }
        this.scoreDividedInto = finalValue;
        getBaseService().update("scoreDividedInto", finalValue, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "scoreDividedInto");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
        DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.clubID, this.getSportsPoint(), this.getScoreDividedInto(), this.getPlayerID())));
    }

    /**
     * 保存分数百分比
     *
     * @param scoreDividedInto 分数百分比
     */
    public void saveShareValue(long unionId, double scoreDividedInto, int shareType) {
        final double finalValue = CommMath.FormatDouble(scoreDividedInto);
        if (this.shareType != shareType) {
            this.shareType = shareType;
            getBaseService().update("shareType", shareType, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "shareType");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }
        if (UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() == shareType) {
            if (this.shareValue == finalValue) {
                return;
            }
            this.shareValue = finalValue;
            getBaseService().update("shareValue", shareValue, id, new AsyncInfo(id));
            DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.clubID, this.getSportsPoint(), this.getShareValue(), this.getPlayerID(), shareType, this.shareValue, this.shareFixedValue)));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "shareValue");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        } else if (UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType) {
            if (this.shareFixedValue == finalValue) {
                return;
            }
            this.shareFixedValue = finalValue;
            getBaseService().update("shareFixedValue", shareFixedValue, id, new AsyncInfo(id));
            DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.clubID, this.getSportsPoint(), this.getShareFixedValue(), this.getPlayerID(), shareType, this.shareValue, this.shareFixedValue)));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, "shareFixedValue");
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        } else if (UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal() == shareType) {
            DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.clubID, this.getSportsPoint(), this.getShareFixedValue(), this.getPlayerID(), shareType, this.shareValue, this.shareFixedValue)));
        }


    }


    /**
     * 保存分数百分比
     *
     * @param value 值
     */
    public void saveShareValue(double value, int shareType) {
        final double finalValue = CommMath.FormatDoubleOnePoint(value);
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(3);
        if (this.shareType != shareType) {
            this.shareType = shareType;
            map.put("shareType", this.shareType);
            getBaseService().update("shareType", shareType, id, new AsyncInfo(id));
        }
        if (UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() == shareType) {
            if (this.shareValue != finalValue) {
                this.shareValue = finalValue;
                map.put("shareValue", this.shareValue);
            }
            getBaseService().update("shareValue", shareValue, id, new AsyncInfo(id));
        } else if (UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType) {
            if (this.shareFixedValue != finalValue) {
                this.shareFixedValue = finalValue;
                map.put("shareFixedValue", this.shareFixedValue);
                getBaseService().update("shareFixedValue", shareFixedValue, id, new AsyncInfo(id));
            }
        }
        //更新共享亲友圈成员信息
        if (Config.isShare() && !map.isEmpty()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, map);
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }

    }

    /**
     * 保存比赛分游戏消耗
     *
     * @param unionId 赛事Id
     * @param value   消耗、收益值
     * @param gameId  游戏ID
     * @param classificationRegionId  玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveGameSportsPoint(Player player, long unionId, double value, int gameId, int classificationRegionId, long roomId, String roomKey) {
        final double finalValue = CommMath.FormatDouble(value);
        if (finalValue == 0D) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ResOpType resOpType = finalValue <= 0 ? ResOpType.Lose : ResOpType.Gain;
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
//                double curRemainder = 0;
//                if (null != roomSportsPoint) {
//                    curRemainder = CommMath.addDouble(roomSportsPoint, finalValue);
//                    clubMember.getClubMemberBO().setSportsPoint(curRemainder);
//                    ShareClubMemberMgr.getInstance().addClubMember(clubMember);
//                } else {
//                    final double preValue = clubMember.getSportsPoint();
//                    // 当前值
//                    curRemainder = CommMath.addDouble(preValue, finalValue);
//                }
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                this.getBaseService().update("sportsPoint", curRemainder, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.SPORTS_POINT_GAME.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId);
                UnionDynamicBO.insertRoomSportsPoint(player.getPid(), this.clubID, CommTime.nowSecond(), ResOpType.Lose.equals(resOpType) ? UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_MINUS.value() : UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ADD.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), roomKey);
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(player.getPid(), clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 保存比赛分房费消耗
     *
     * @param unionId 赛事Id
     * @param value   消耗、收益值
     * @param gameId  游戏ID
     * @param classificationRegionId  玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveRoomSportsPoint(Player player, long unionId, double value, int gameId, int classificationRegionId, long roomId, String roomKey) {
        final double finalValue = CommMath.FormatDouble(value);
        if (finalValue == 0D) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                this.getBaseService().update("sportsPoint", curRemainder, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.SPORTS_POINT_ROOM.value(), finalValue, curRemainder, preValue, ResOpType.Lose.ordinal(), gameId, classificationRegionId, roomId);
                UnionDynamicBO.insertRoomSportsPoint(player.getPid(), this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), roomKey);

                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(player.getPid(), clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 保存报名费收益
     * 亲友圈-具体到人
     *
     * @param unionId    赛事Id
     * @param value      收益值
     * @param sourceType 来源类型
     * @param roomName   房间名称
     * @param roomKey    房间key
     * @param gameId     游戏Id
     * @param classificationRegionId     玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveUnionSportsPointProfitPromotion(long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ResOpType resOpType, long roomId, String dateTime, long execPid, long reasonPid, String dateTimeZhongZhi) {
        final double finalValue = CommMath.FormatDouble(value);
        if (0D == finalValue) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                //中至赛事添加判断
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    // 设置比赛分
                    this.setSportsPoint(curRemainder);
                    clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                    getBaseService().update("sportsPoint", curRemainder, id, new AsyncInfo(id));
                } else {
                    curRemainder = preValue;
                }
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId);
                // 推广员分成每日一表记录
                FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, execPid, roomName, "", String.valueOf(roomKey), reasonPid);
                FlowLogger.clubLevelRoomLogShareValue(dateTime, this.getPlayerID(), 0, 0, roomId, 0, this.getId(), 0, 0, 0, 0, 0, 0, clubID, unionId, finalValue, execPid);
                //中至模式数据记录
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                    FlowLogger.clubLevelRoomLogShareValueZhongZhi(dateTimeZhongZhi, this.getPlayerID(), 0, 0, roomId, 0, this.getId(), 0, 0, 0, 0, 0, 0, clubID, unionId, finalValue, execPid);
                }
                UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), String.valueOf(roomKey));
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(playerID, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 保存报名费收益
     * 整个亲友圈多少
     *
     * @param unionId    赛事Id
     * @param value      收益值
     * @param sourceType 来源类型
     * @param roomName   房间名称
     * @param roomKey    房间key
     * @param gameId     游戏Id
     * @param classificationRegionId     玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveUnionSportsPointProfitClub(long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ResOpType resOpType, long roomId) {
        final double finalValue = CommMath.FormatDouble(value);
        if (0D == finalValue) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                //中至赛事添加判断
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                double preValue = CommMath.FormatDouble(clubMember.getClubMemberBO().getScorePoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);

                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    this.setScorePoint(curRemainder);
                    clubMember.getClubMemberBO().setScorePoint(curRemainder);
                    getBaseService().update("scorePoint", this.getScorePoint(), id, new AsyncInfo(id));
                }

                //亲友群 赛事比赛分收益记录
                FlowLogger.unionSportsPointProfitLog(unionId, this.clubID, finalValue, sourceType, roomName, roomKey, roomId);
//            UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_INCOME.value(), unionId, String.valueOf(finalValue), String.valueOf(this.getScorePoint()), String.valueOf(roomKey));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(playerID, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 保存报名费收益
     *
     * @param unionId    赛事Id
     * @param value      收益值
     * @param sourceType 来源类型
     * @param roomName   房间名称
     * @param roomKey    房间key
     * @param gameId     游戏Id
     * @param classificationRegionId     玩法地区分类统计ID，不参与身份、资产或路由
     *                   needRecord 是否需要记录每日一表中的统计数据
     */
    public void saveUnionSportsPointProfit(long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ResOpType resOpType, long roomId, boolean needRecord, Map<Long, Double> clubOwnerProfit) {
        final double finalValue = CommMath.FormatDouble(value);
        if (0D == finalValue) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                //中至赛事添加判断
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    // 设置比赛分
                    this.setSportsPoint(curRemainder);
                    clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                    getBaseService().update("sportsPoint", curRemainder, id, new AsyncInfo(id));
                } else {
                    curRemainder = preValue;
                }
                double finalCurRemainder = curRemainder;
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.SPORTS_POINT_PROFIT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId);
                //赢的那条 推广员分成每日一表记录 只记录盟主收的那条
                if (value > 0) {
                    FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), value, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, 0, roomName, "", String.valueOf(roomKey), 0);
                } else {
                    // // 推广员分成每日一表记录 根据圈主记录map循环记录
                    if (MapUtils.isNotEmpty(clubOwnerProfit)) {
                        clubOwnerProfit.entrySet().forEach(k -> {
                                    if (k.getValue() != 0) {
                                        // 推广员分成每日一表记录
                                        FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), k.getValue(), finalCurRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, 0, roomName, "", String.valueOf(roomKey), k.getKey());
                                    }
                                }
                        );
                    }
                }
                // 赛事比赛分收益记录
                FlowLogger.unionSportsPointProfitLog(unionId, this.clubID, finalValue, sourceType, roomName, roomKey, roomId);
                UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_INCOME.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), String.valueOf(roomKey));

                // 前置值
                double scorePointPreValue = CommMath.FormatDouble(clubMember.getClubMemberBO().getScorePoint());
                // 当前值
                double scorePointCurRemainder = CommMath.addDouble(scorePointPreValue, finalValue);

                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    this.setScorePoint(scorePointCurRemainder);
                    clubMember.getClubMemberBO().setScorePoint(scorePointCurRemainder);
                    getBaseService().update("scorePoint", this.getScorePoint(), id, new AsyncInfo(id));
                }

                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(playerID, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 执行比赛分更新
     *
     * @param unionId  赛事Id
     * @param value    值
     * @param itemFlow
     * @return
     */
    public boolean execSportsPointUpdate(long unionId, double value, ItemFlow itemFlow, RoomTypeEnum roomTypeEnum, double outSports) {
        final double finalValue = CommMath.FormatDouble(value);
        //审核比较特殊  是0的话也要处理
        if (finalValue == 0D && !ItemFlow.CLUB_SPORTS_POINT_EXAMINE.equals(itemFlow)) {
            return false;
        }
        ResOpType resOpType = finalValue < 0D ? ResOpType.Lose : ResOpType.Gain;
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                // 操作类型
                if (ResOpType.Lose.equals(resOpType)) {

                    double limit = 0D;
                    if (!ItemFlow.CLUB_SPORTS_POINT_EXAMINE.equals(itemFlow)) {
                        //不是审核的时候才有0分限制
                        //添加推广员限制
                        if (this.getLevel() > 0 && UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == this.getWarnStatus() && this.getSportsPointWarning() < 0D) {
                            //是推广员的话 并且预警值有打开  并且预警值设置的是0
                            limit = this.getSportsPointWarning();
                        }
                        if (curRemainder < limit) {
                            return false;
                        }
                    }
                } else if (ResOpType.Gain.equals(resOpType)) {
                    // 获取
                } else {
                    return false;
                }
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);

                // 保存比赛分和比赛状态
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(3);
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                    if (curRemainder >= this.getEliminatePoint()) {
                        if (this.unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                            this.unionState = UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
                            this.resetTime = CommTime.nowSecond();
                            clubMember.getClubMemberBO().setUnionState(this.unionState);
                            clubMember.getClubMemberBO().setResetTime(this.resetTime);
                            map.put("unionState", this.unionState);
                            map.put("resetTime", this.resetTime);
                        }
                    }
                    //记录日志
                    FlowLogger.sportsPointChangeZhongZhiLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                } else {
                    if (curRemainder >= outSports) {
                        if (this.unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                            this.unionState = UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
                            this.resetTime = CommTime.nowSecond();
                            clubMember.getClubMemberBO().setUnionState(this.unionState);
                            clubMember.getClubMemberBO().setResetTime(this.resetTime);
                            map.put("unionState", this.unionState);
                            map.put("resetTime", this.resetTime);
                        }
                    }
                }
                map.put("sportsPoint", curRemainder);
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update(map, id, new AsyncInfo(id));
                //更新共享数据
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                return true;
            })) {
                if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
                    DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.getClubID(), this.getSportsPoint(), this.getScoreDividedInto(), this.getPlayerID())));
                } else if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
                    DispatcherComponent.getInstance().publish(new ClubNotify2AllByManageEvent(clubID, SClub_MemberInfoChange.make(this.getClubID(), this.getSportsPoint(), this.getPlayerID())));
                }
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));
                return true;
            }
            return false;
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 执行比赛分更新
     *
     * @param unionId  赛事Id
     * @param value    值
     * @param itemFlow
     * @return
     */
    public boolean execSportsPointExamine(long unionId, double value, ItemFlow itemFlow, RoomTypeEnum roomTypeEnum, double outSports) {
        final double finalValue = CommMath.FormatDouble(value);
        if (finalValue == 0D) {
            return false;
        }
        ResOpType resOpType = finalValue < 0D ? ResOpType.Lose : ResOpType.Gain;
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                // 操作类型
                if (ResOpType.Lose.equals(resOpType)) {

                } else if (ResOpType.Gain.equals(resOpType)) {
                    // 获取
                } else {
                    return false;
                }
                // 保存比赛分和比赛状态
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(3);
                if (curRemainder >= outSports) {
                    if (this.unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                        this.unionState = UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
                        this.resetTime = CommTime.nowSecond();
                        clubMember.getClubMemberBO().setUnionState(this.unionState);
                        clubMember.getClubMemberBO().setResetTime(this.resetTime);
                        map.put("unionState", this.unionState);
                        map.put("resetTime", this.resetTime);
                    }
                }
                map.put("sportsPoint", curRemainder);
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update(map, id, new AsyncInfo(id));
                //更新共享数据
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                return true;
            })) {
                if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
                    DispatcherComponent.getInstance().publish(new UnionNotify2AllByManageEvent(unionId, SUnion_MemberInfoChange.make(unionId, this.getClubID(), this.getSportsPoint(), this.getScoreDividedInto(), this.getPlayerID())));
                } else if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
                    DispatcherComponent.getInstance().publish(new ClubNotify2AllByManageEvent(clubID, SClub_MemberInfoChange.make(this.getClubID(), this.getSportsPoint(), this.getPlayerID())));
                }
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));
                return true;
            }
            return false;
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 执行比赛分初始
     *
     * @param unionId  赛事Id
     * @param value    值
     * @param itemFlow
     * @return
     */
    public void execSportsPointInit(long unionId, double value, ItemFlow itemFlow) {
        final double finalValue = CommMath.FormatDouble(value);
        if (finalValue == 0D) {
            return;
        }
        ResOpType resOpType = finalValue < 0D ? ResOpType.Lose : ResOpType.Gain;
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                    //记录日志
                    FlowLogger.sportsPointChangeZhongZhiLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                }
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 执行比赛分清空
     *
     * @param unionId 赛事Id
     * @return
     */
    public void execSportsPointClear(long unionId) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = 0D;
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_CLEAR_SPORTS.value(), preValue, curRemainder, preValue, ResOpType.Lose.ordinal(), -1, -1, -1);
                return true;
            })) {
                return;
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 该玩家的比赛分清零
     * 申请退赛
     *
     * @param unionId 赛事Id
     * @return
     */
    public UnionApplyOperateItem execSportsPointBackOff(long unionId) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (UnionApplyOperateItem) this.getBooleanLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 最终扣除的值
                double finalValue = -preValue;
                // 资源
                ResOpType resOpType = finalValue <= 0D ? ResOpType.Lose : ResOpType.Gain;
                // 当前值
                double curRemainder = 0D;
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                    //记录日志
                    FlowLogger.sportsPointChangeZhongZhiLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_APPLY_REMATCH.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                }
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_BACK_OFF.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));

                return new UnionApplyOperateItem(finalValue, preValue, 0D, resOpType);
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 该玩家的比赛分清零
     * 申请退赛
     *
     * @param unionId 赛事Id
     * @return
     */
    public UnionApplyOperateItem execSportsPointBackOffByZhongZhi(long unionId, double outSports) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (UnionApplyOperateItem) this.getBooleanLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 最终扣除的值
                double finalValue = -preValue;
                // 资源
                ResOpType resOpType = finalValue <= 0D ? ResOpType.Lose : ResOpType.Gain;
                // 当前值
                double curRemainder = 0D;
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_BACK_OFF.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));

                return new UnionApplyOperateItem(finalValue, preValue, 0D, resOpType);
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 回退值
     *
     * @param unionId  赛事Id
     * @param value    回退值
     * @param itemFlow
     */
    public void execRollbackValue(long unionId, double value, ItemFlow itemFlow) {
        final double finalValue = CommMath.FormatDouble(value);
        ResOpType resOpType = finalValue < 0D ? ResOpType.Lose : ResOpType.Gain;
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = clubMember.getSportsPoint();
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);

                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 该玩家的比赛分清零
     * 复赛申请
     *
     * @param unionId 赛事Id
     * @return
     */
    public UnionApplyOperateItem execSportsPointApplyRematch(long unionId, double outSportsPoint) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (UnionApplyOperateItem) this.getBooleanLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 最终扣除的值
                double finalValue = 0D;
                // 前置值
                double preValue = 0D;
                // 淘汰值
                double outPoint = outSportsPoint <= 0D ? 0D : outSportsPoint;
                // 资源
                ResOpType resOpType = ResOpType.None;
                // 1、先判断 淘汰值 <= 0 ? 0D:淘汰值;
                // 前置值
                preValue = clubMember.getSportsPoint();
                if (preValue >= outPoint) {
                    // 身上分数 >= 淘汰分
                    return new UnionApplyOperateItem(0D, preValue, outPoint, resOpType);
                } else {
                    // 身上分数 < 淘汰分
                    finalValue = CommMath.subDouble(outPoint, preValue);
                    resOpType = ResOpType.Gain;
                }
                double valueD = CommMath.addDouble(preValue, finalValue);
                if (valueD != outPoint) {
                    CommLog.error("execSportsPointApplyRematch id:{%d},ClubId:{%d},finalValue:{%f},valueD:{%f},preValue:{%f},resOpType:{%s}", id, clubID, finalValue, valueD, preValue, resOpType);
                    return null;
                }
                // 当前值
                double curRemainder = outPoint;
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                    //记录日志
                    FlowLogger.sportsPointChangeZhongZhiLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_APPLY_REMATCH.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                }
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_APPLY_REMATCH.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));

                return new UnionApplyOperateItem(finalValue, preValue, outPoint, resOpType);
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 该玩家的比赛分清零
     * 复赛申请
     *
     * @param unionId 赛事Id
     * @return
     */
    public UnionApplyOperateItem execSportsPointApplyRematchZhongZhi(long unionId, double outSportsPoint) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (UnionApplyOperateItem) this.getBooleanLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 最终扣除的值
                double finalValue = 0D;
                // 前置值
                double preValue = 0D;
                // 淘汰值
                double outPoint = outSportsPoint <= 0D ? 0D : outSportsPoint;
                // 资源
                ResOpType resOpType = ResOpType.None;
                // 1、先判断 淘汰值 <= 0 ? 0D:淘汰值;
                // 前置值
                preValue = clubMember.getSportsPoint();
                if (preValue >= outPoint) {
                    // 身上分数 >= 淘汰分
                    return new UnionApplyOperateItem(0D, preValue, outPoint, resOpType);
                } else {
                    // 身上分数 < 淘汰分
                    finalValue = CommMath.subDouble(outPoint, preValue);
                    resOpType = ResOpType.Gain;
                }
                double valueD = CommMath.addDouble(preValue, finalValue);
                if (valueD != outPoint) {
                    CommLog.error("execSportsPointApplyRematch id:{%d},ClubId:{%d},finalValue:{%f},valueD:{%f},preValue:{%f},resOpType:{%s}", id, clubID, finalValue, valueD, preValue, resOpType);
                    return null;
                }
                // 当前值
                double curRemainder = outPoint;
                // 设置比赛分
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                getBaseService().update("sportsPoint", curRemainder, getId(), new AsyncInfo(getId()));
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                    //记录日志
                    FlowLogger.sportsPointChangeZhongZhiLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_APPLY_REMATCH.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                }
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, ItemFlow.UNION_APPLY_REMATCH.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), -1, -1, -1);
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerExecSportsPointEvent(getPlayerID(), getId(), SUnion_SportsPoint.make(getClubID(), getPlayerID(), this.getSportsPoint(), this.getUnionState()), resOpType, finalValue));

                return new UnionApplyOperateItem(finalValue, preValue, outPoint, resOpType);
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 重置每天的活跃值
     *
     * @return
     */
    @Deprecated
    public double clearDayActivePoint() {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (double) this.getActiveLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                final double finalValue = clubMember.getClubMemberBO().getDayActivePoint();
                this.setDayActivePoint(0D);
                clubMember.getClubMemberBO().setDayActivePoint(0D);
                getBaseService().update("dayActivePoint", 0D, getId(), new AsyncInfo(getId()));
                FlowLogger.clubPromotionDayActiveChargeLog(getPlayerID(), getClubID(), 0L, ItemFlow.CLUB_PROMOTION_ACTIVE_CLEAR.value(), finalValue, 0D, -finalValue, ResOpType.Lose.ordinal(), getPartnerPid());
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return finalValue;
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 执行推广员活跃值改变
     *
     * @param value 回退值
     */
    @Deprecated
    public double execSumPromotionActiveValue(double value) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (double) this.getActiveLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                final double finalValue = CommMath.FormatDouble(Math.abs(value));
                // 前值累计活跃值
                double preSumValue = clubMember.getClubMemberBO().getSumActivePoint();
                // 当前累计活跃值
                double curSumRemainder = CommMath.addDouble(preSumValue, finalValue);
                this.setSumActivePoint(curSumRemainder);
                clubMember.getClubMemberBO().setSumActivePoint(curSumRemainder);
                // 前值累计当天活跃值
                double preDayValue = clubMember.getClubMemberBO().getDayActivePoint();
                // 累计当天活跃值
                double curDayRemainder = CommMath.addDouble(preDayValue, finalValue);
                this.setDayActivePoint(curDayRemainder);
                clubMember.getClubMemberBO().setDayActivePoint(curDayRemainder);
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
                map.put("sumActivePoint", curSumRemainder);
                map.put("dayActivePoint", curDayRemainder);
                getBaseService().update(map, getId(), new AsyncInfo(getId()));
                FlowLogger.clubPromotionDayActiveChargeLog(getPlayerID(), getClubID(), 0L, ItemFlow.CLUB_PROMOTION_ACTIVE_ROOM.value(), finalValue, curDayRemainder, preDayValue, ResOpType.Gain.ordinal(), getPartnerPid());
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return curSumRemainder;
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 执行推广员活跃值改变
     *
     * @param value 回退值
     */
    @Deprecated
    public double execPromotionActiveValue(double value) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (double) this.getActiveLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 数量
                final double finalValue = CommMath.FormatDouble(Math.abs(value));
                // 前值活跃值
                double preValue = clubMember.getClubMemberBO().getActivePoint();
                // 当前活跃值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                this.setActivePoint(curRemainder);
                clubMember.getClubMemberBO().setActivePoint(curRemainder);
                getBaseService().update("activePoint", curRemainder, getId(), new AsyncInfo(getId()));
                FlowLogger.clubPromotionActiveChargeLog(getPlayerID(), getClubID(), 0L, ItemFlow.CLUB_PROMOTION_ACTIVE_ROOM.value(), finalValue, curRemainder, preValue, ResOpType.Gain.ordinal(), getPartnerPid());
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return curRemainder;
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 执行推广员活跃值改变
     *
     * @param value 回退值
     */
    public double execPromotionActiveValue(double value, int type) {
        String uuid = UUID.randomUUID().toString();
        try {
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            return (double) this.getActiveLock().booleanValueLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 操作类型
                ResOpType resOpType = type == 0 ? ResOpType.Gain : ResOpType.Lose;
                // 消耗数量
                final double finalValue = CommMath.FormatDouble(ResOpType.Gain.equals(resOpType) ? Math.abs(value) : -Math.abs(value));
                // 前值活跃值
                double preValue = clubMember.getClubMemberBO().getActivePoint();
                // 当前活跃值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                this.setActivePoint(curRemainder);
                clubMember.getClubMemberBO().setActivePoint(curRemainder);
                // 前值累计活跃值
                double preSumValue = clubMember.getClubMemberBO().getSumActivePoint();
                // 当前累计活跃值
                double curSumRemainder = CommMath.addDouble(preSumValue, finalValue);
                this.setSumActivePoint(curSumRemainder);
                clubMember.getClubMemberBO().setSumActivePoint(curSumRemainder);
                // 前值累计当天活跃值
                double preDayValue = clubMember.getClubMemberBO().getDayActivePoint();
                // 累计当天活跃值
                double curDayRemainder = CommMath.addDouble(preDayValue, finalValue);
                this.setDayActivePoint(curDayRemainder);
                clubMember.getClubMemberBO().setDayActivePoint(curDayRemainder);
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(3);
                map.put("activePoint", curRemainder);
                map.put("sumActivePoint", curSumRemainder);
                map.put("dayActivePoint", curDayRemainder);
                getBaseService().update(map, getId(), new AsyncInfo(getId()));
                FlowLogger.clubPromotionActiveChargeLog(getPlayerID(), getClubID(), 0L, ItemFlow.CLUB_PROMOTION_ACTIVE_CREATE.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), getPartnerPid());
                FlowLogger.clubPromotionDayActiveChargeLog(getPlayerID(), getClubID(), 0L, ItemFlow.CLUB_PROMOTION_ACTIVE_CREATE.value(), finalValue, curDayRemainder, preDayValue, resOpType.ordinal(), getPartnerPid());
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return curRemainder;
            });
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }


    /**
     * 清空亲友圈成员有关赛事的信息
     */
    public void clearUnionInfo(int roundId) {
        this.getBooleanLock().booleanLock(() -> {
            HashMap<String, Object> updateMap = new HashMap<>();
            if (StringUtils.isNotEmpty(this.unionNotGames)) {
                // 清空赛事非勾选游戏列表
                this.unionNotGames = "";
                updateMap.put("unionNotGames", unionNotGames);
            }
            if (CollectionUtils.isNotEmpty(unionNotGameList)) {
                this.unionNotGameList.clear();
            }
            if (this.getIsHideStartRoom() != 0) {
                // 清空是否隐藏开始的房间(0:否,1:是)
                this.isHideStartRoom = 0;
                updateMap.put("isHideStartRoom", this.isHideStartRoom);
            }
            if (this.scorePoint != 0D) {
                // 分数-收益总分数
                this.scorePoint = 0D;
                updateMap.put("scorePoint", this.scorePoint);
            }
            if (this.scoreDividedInto != 0D) {
                // 分数分成值
                this.scoreDividedInto = 0D;
                updateMap.put("scoreDividedInto", this.scoreDividedInto);
            }
            if (this.unionBanGame != 0) {
                // 解除联盟禁止游戏
                this.unionBanGame = 0;
                updateMap.put("unionBanGame", this.unionBanGame);
            }
            if (this.unionState != 0) {
                this.unionState = 0;
                updateMap.put("unionState", this.unionState);
            }
            if (this.roundId != roundId) {
                this.roundId = roundId;
                updateMap.put("roundId", this.roundId);
            }

            if (this.calcActive != 0D) {
                this.calcActive = 0D;
                updateMap.put("calcActive", this.calcActive);
            }

            if (this.configId != 0L) {
                this.configId = 0L;
                updateMap.put("configId", this.configId);
            }

            if (MapUtils.isNotEmpty(updateMap)) {
                getBaseService().update(updateMap, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                if (Config.isShare()) {
                    //更新共享字段
                    ShareClubMemberMgr.getInstance().updateField(this, updateMap);
                    //推送到MQ
                    //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
                }
            }

            return true;
        });
    }

    public void saveBanGame(int banGame) {
        if (this.banGame == banGame) {
            return;
        }
        this.banGame = banGame;
        getBaseService().update("banGame", banGame, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "banGame");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }


    public boolean setUnionBanGame(int unionBanGame) {
        if (this.unionBanGame == unionBanGame) {
            return false;
        }
        this.unionBanGame = unionBanGame;
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "unionBanGame");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
        return true;
    }

    public void saveUnionBanGame(int unionBanGame) {
        if (this.unionBanGame == unionBanGame) {
            return;
        }
        this.unionBanGame = unionBanGame;
        getBaseService().update("unionBanGame", unionBanGame, id, new AsyncInfo(id));
    }


    public double getSportsPoint() {
        return CommMath.FormatDouble(sportsPoint);
    }

    public double getSportsPointRedis() {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getClubMember(id).getClubMemberBO().getSportsPoint();
        }
        return CommMath.FormatDouble(sportsPoint);
    }

    public double getScorePoint() {
        return CommMath.FormatDouble(scorePoint);

    }

    public double getScoreDividedInto() {
        return CommMath.FormatDouble(scoreDividedInto);

    }

    /**
     * 切换创建者身份
     */
    public void changeCreate() {
        Map<String, Object> map = new HashMap<>();
        if (this.isminister != Club_define.Club_MINISTER.Club_MINISTER_CREATER.value()) {
            this.isminister = Club_define.Club_MINISTER.Club_MINISTER_CREATER.value();
            map.put("isminister", this.isminister);
        }

        if (0 != this.deletetime) {
            this.deletetime = 0;
            map.put("deletetime", this.deletetime);
        }

        if (0 != this.banGame) {
            this.banGame = 0;
            map.put("banGame", this.banGame);
        }

        if (0 != this.unionBanGame) {
            this.unionBanGame = 0;
            map.put("unionBanGame", this.unionBanGame);
        }
        if (this.status != Club_define.Club_Player_Status.PLAYER_JIARU.value()) {
            this.status = Club_define.Club_Player_Status.PLAYER_JIARU.value();
            map.put("status", this.status);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, map);
                //推送到MQ
                MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }


    }

    /**
     * 切换成员身份
     */
    public void changeGeneral() {
        Map<String, Object> map = new HashMap<>();
        if (this.isminister != Club_define.Club_MINISTER.Club_MINISTER_GENERAL.value()) {
            this.isminister = Club_define.Club_MINISTER.Club_MINISTER_GENERAL.value();
            map.put("isminister", this.isminister);
        }

        if (0 != this.deletetime) {
            this.deletetime = 0;
            map.put("deletetime", this.deletetime);
        }

        if (0 != this.banGame) {
            this.banGame = 0;
            map.put("banGame", this.banGame);
        }

        if (0 != this.unionBanGame) {
            this.unionBanGame = 0;
            map.put("unionBanGame", this.unionBanGame);
        }
        if (this.status != Club_define.Club_Player_Status.PLAYER_JIARU.value()) {
            this.status = Club_define.Club_Player_Status.PLAYER_JIARU.value();
            map.put("status", this.status);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, map);
                //推送到MQ
                MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }

    }

    /**
     * 清空合伙人
     */
    public void clearPartner() {
        if (this.partner == 1 && promotion <= 0) {
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
            this.partner = 0;
            this.partnerPid = 0L;
            map.put("partner", 0);
            map.put("partnerPid", 0L);
            getBaseService().update(map, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, map);
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }
    }

    public void saveShareTypeInit() {
        this.shareType = 0;
        this.shareValue = 100;
        getBaseService().update("shareType", shareType, id, new AsyncInfo(id));
        getBaseService().update("shareValue", shareValue, id, new AsyncInfo(id));
        //更新共享亲友圈成员信息
        if (Config.isShare()) {
            //更新共享字段
            ShareClubMemberMgr.getInstance().updateField(this, "shareType", "shareValue");
            //推送到MQ
            //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
    }

    public void saveClearPromotionLevelPowerOp() {
        HashMap<String, Object> updateMap = new HashMap<>();
        this.kicking = 0;
        updateMap.put("kicking", this.kicking);
        this.modifyValue = 0;
        updateMap.put("modifyValue", this.modifyValue);
        this.showShare = 0;
        updateMap.put("showShare", this.showShare);
        this.level = 0;
        updateMap.put("level", this.level);
        this.invite = 0;
        updateMap.put("invite", this.invite);
        if (MapUtils.isNotEmpty(updateMap)) {
            getBaseService().update(updateMap, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, updateMap);
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }

    }

    /**
     * @param kicking     踢出
     * @param modifyValue 修改从属
     * @param showShare   显示分成
     */
    public void savePromotionLevelPowerOp(int kicking, int modifyValue, int showShare, int invite) {
        HashMap<String, Object> updateMap = new HashMap<>();
        if (kicking != this.kicking) {
            this.kicking = kicking;
            updateMap.put("kicking", kicking);
        }
        if (modifyValue != this.modifyValue) {
            this.modifyValue = modifyValue;
            updateMap.put("modifyValue", modifyValue);
        }
        if (showShare != this.showShare) {
            this.showShare = showShare;
            updateMap.put("showShare", showShare);
        }
        if (invite != this.invite) {
            this.invite = invite;
            updateMap.put("invite", invite);
        }
        if (MapUtils.isNotEmpty(updateMap)) {
            getBaseService().update(updateMap, id, new AsyncInfo(id));
            //更新共享亲友圈成员信息
            if (Config.isShare()) {
                //更新共享字段
                ShareClubMemberMgr.getInstance().updateField(this, updateMap);
                //推送到MQ
                //MqProducerMgr.get().send(MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
            }
        }


    }


    /**
     * 直接不允许
     */
    public void saveKicking() {
        this.kicking = 0;
        getBaseService().update("kicking", kicking, id, new AsyncInfo(id));

    }

    /**
     * 保存比赛分房费消耗
     *
     * @param unionId 赛事Id
     * @param value   消耗、收益值
     * @param gameId  游戏ID
     * @param classificationRegionId  玩法地区分类统计ID，不参与身份、资产或路由
     */
    public boolean saveRoomSportsPointConsumeQiePai(ClubMemberBO clubMemberBO, long playerID, long unionId, double value, int gameId, int classificationRegionId, long roomId, String roomKey, double outSports) {
        final double finalValue = CommMath.FormatDouble(value);
        if (finalValue == 0D) {
            return false;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                this.setSportsPoint(curRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curRemainder);
                this.getBaseService().update("sportsPoint", curRemainder, id, new AsyncInfo(id));
                if (finalValue < 0) {
                    Map<String, Object> map = Maps.newHashMapWithExpectedSize(3);
                    if (curRemainder >= outSports) {
                        if (this.unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                            this.unionState = UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
                            this.resetTime = CommTime.nowSecond();
                            clubMember.getClubMemberBO().setUnionState(this.unionState);
                            clubMember.getClubMemberBO().setResetTime(this.resetTime);
                            map.put("unionState", this.unionState);
                            map.put("resetTime", this.resetTime);
                        }
                    }
                    getBaseService().update(map, id, new AsyncInfo(id));
                }
                //更新共享数据
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.QIEPAI_CONSUME_POINT_CHANGE.value(), finalValue, curRemainder, preValue, ResOpType.Lose.ordinal(), gameId, classificationRegionId, roomId);
                if (finalValue > 0) {
                    //execPid 0 具体谁洗牌去XiPaiLogFlow查看
                    UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, 0, clubMemberBO.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_INCOME.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), roomKey);
                    ContainerMgr.get().getComponent(XiPaiLogFlowService.class).save(XiPaiLogFlow.xiPaiLogInit(clubMemberBO.getPlayerID(), this.clubID, unionId, finalValue, roomKey));
                } else {
                    UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_CONSUME.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), roomKey);
                }
                return true;
            })) {
                SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(playerID);
                if (Objects.isNull(sharePlayer)) {
                    return false;
                }
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(sharePlayer, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
                return true;
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
        return false;
    }

    /**
     * 保存保险箱分数变更
     *
     * @param value 消耗、收益值
     */
    public void saveCaseSportsPoint(SharePlayer sharePlayer, double value, UnionDefine.UNION_EXEC_TYPE type, Long unionId) {
        final double valueFormat = CommMath.FormatDouble(value);
        double caseFinalValue;
        double sportFinalValue;
        if (valueFormat == 0D) {
            return;
        }
        ResOpType resOpType;
        ResOpType caseResOpType;
        ItemFlow itemFlow;
        if (type.equals(UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_ADD)) {
            sportFinalValue = (-valueFormat);
            caseFinalValue = valueFormat;
            caseResOpType = ResOpType.Gain;
            resOpType = ResOpType.Lose;
            itemFlow = ItemFlow.UNION_CASE_SPORT_ADD;
        } else {
            sportFinalValue = valueFormat;
            caseFinalValue = (-valueFormat);
            caseResOpType = ResOpType.Lose;
            resOpType = ResOpType.Gain;
            itemFlow = ItemFlow.UNION_CASE_SPORT_SUB;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {

                Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preSportsPointValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 竞技点修改 当前值
                double curSportsRemainder = CommMath.addDouble(preSportsPointValue, sportFinalValue);
                this.setSportsPoint(curSportsRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curSportsRemainder);

                map.put("sportsPoint", curSportsRemainder);
//                this.getBaseService().update("sportsPoint", curSportsRemainder, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), unionId, itemFlow.value(), valueFormat, curSportsRemainder, preSportsPointValue, resOpType.ordinal(), -1, -1, -1);
                // 保险箱前置
                double preCaseSportsPointValue = CommMath.FormatDouble(clubMember.getCaseSportsPoint());
                // 保险箱竞技点修改 当前值
                double curCaseSportsRemainder = CommMath.addDouble(preCaseSportsPointValue, caseFinalValue);
                this.setCaseSportsPoint(curCaseSportsRemainder);
                clubMember.getClubMemberBO().setCaseSportsPoint(curCaseSportsRemainder);
                map.put("caseSportsPoint", curCaseSportsRemainder);
//                this.getBaseService().update("caseSportsPoint", curCaseSportsRemainder, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                getBaseService().update(map, id, new AsyncInfo(id));
//                if (Config.isShare()) {
//                    //更新共享字段
//                    ShareClubMemberMgr.getInstance().updateField(this, map);
//                }
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                FlowLogger.casePointChargeLog(this.getPlayerID(), this.getClubID(), unionId, valueFormat, curSportsRemainder, preSportsPointValue, preCaseSportsPointValue, curCaseSportsRemainder, caseResOpType.ordinal());
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(sharePlayer, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 关闭保险箱分数变更
     *
     * @param player 对应玩家
     */
    public void closeCaseSportsPoint(Player player, UnionDefine.UNION_EXEC_TYPE type) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                double preCaseSportsPointValue = CommMath.FormatDouble(clubMember.getCaseSportsPoint());
                final double changeValue = preCaseSportsPointValue;
                // 前置值
                final double preSportsPointValue = CommMath.FormatDouble(clubMember.getSportsPoint());
                // 竞技点修改 当前值
                double curSportsRemainder = CommMath.addDouble(preSportsPointValue, changeValue);
                this.setSportsPoint(curSportsRemainder);
                clubMember.getClubMemberBO().setSportsPoint(curSportsRemainder);
                map.put("sportsPoint", curSportsRemainder);
//                this.getBaseService().update("sportsPoint", curSportsRemainder, id, new AsyncInfo(id));
                FlowLogger.sportsPointChargeLog(this.getPlayerID(), this.getClubID(), -1, ItemFlow.UNION_CASE_SPORT_SUB.value(), changeValue, curSportsRemainder, preSportsPointValue, ResOpType.Lose.ordinal(), -1, -1, -1);
                // 保险箱竞技点修改 当前值
                double curCaseSportsRemainder = 0D;
                this.setCaseSportsPoint(curCaseSportsRemainder);
                clubMember.getClubMemberBO().setCaseSportsPoint(curCaseSportsRemainder);
                map.put("caseSportsPoint", curCaseSportsRemainder);
//                this.getBaseService().update("caseSportsPoint", curCaseSportsRemainder, id, new AsyncInfo(id));
                //更新共享亲友圈成员信息
                getBaseService().update(map, id, new AsyncInfo(id));
//                更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
//                if (Config.isShare()) {
//                    //更新共享字段
//                    ShareClubMemberMgr.getInstance().updateField(this, map);
//                }
//                UnionDynamicBO.insertCaseSportsRecord(this.playerID,player.getPid(), this.clubID, CommTime.nowSecond(), getType.value(), 0, String.valueOf(changeValue),String.valueOf(curCaseSportsRemainder), String.valueOf(curSportsRemainder), "");

                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(player.getPid(), clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 保存报名费收益到保险箱
     * 亲友圈-具体到人
     *
     * @param unionId    赛事Id
     * @param value      收益值
     * @param sourceType 来源类型
     * @param roomName   房间名称
     * @param roomKey    房间key
     * @param gameId     游戏Id
     * @param classificationRegionId     玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveUnionSportsPointProfitPromotionToCasePoint(long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ResOpType resOpType, long roomId, String dateTime, long execPid, long reasonPid, String dateTimeZhongZhi) {
        final double finalValue = CommMath.FormatDouble(value);
        if (0D == finalValue) {
            return;
        }
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                //中至赛事添加判断
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getCaseSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);

                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    // 设置比赛分
                    this.setCaseSportsPoint(curRemainder);
                    clubMember.getClubMemberBO().setCaseSportsPoint(curRemainder);
                    getBaseService().update("caseSportsPoint", curRemainder, id, new AsyncInfo(id));
                } else {
                    curRemainder = preValue;
                }
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId);
                // 推广员分成每日一表记录
                FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, execPid, roomName, "", String.valueOf(roomKey), reasonPid);
                FlowLogger.clubLevelRoomLogShareValue(dateTime, this.getPlayerID(), 0, 0, roomId, 0, this.getId(), 0, 0, 0, 0, 0, 0, clubID, unionId, finalValue, execPid);
                //中至模式数据记录
                if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                    FlowLogger.clubLevelRoomLogShareValueZhongZhi(dateTimeZhongZhi, this.getPlayerID(), 0, 0, roomId, 0, this.getId(), 0, 0, 0, 0, 0, 0, clubID, unionId, finalValue, execPid);
                }
                UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME_CASEPOINT.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), String.valueOf(roomKey));
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(playerID, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 保存报名费收益
     *
     * @param unionId    赛事Id
     * @param value      收益值
     * @param sourceType 来源类型
     * @param roomName   房间名称
     * @param roomKey    房间key
     * @param gameId     游戏Id
     * @param classificationRegionId     玩法地区分类统计ID，不参与身份、资产或路由
     */
    public void saveUnionSportsPointProfitToCasePoint(long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ResOpType resOpType, long roomId, boolean needRecord, Map<Long, Double> clubOwnerProfit) {
        final double finalValue = CommMath.FormatDouble(value);
        if (0D == finalValue) {
            return;
        }

        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + this.id, uuid);
            if (this.getBooleanLock().booleanLock(() -> {
                //中至赛事添加判断
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
                if (Objects.nonNull(union)) {
                    unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                }
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
                // 前置值
                final double preValue = CommMath.FormatDouble(clubMember.getCaseSportsPoint());
                // 当前值
                double curRemainder = CommMath.addDouble(preValue, finalValue);
                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    // 设置比赛分
                    this.setCaseSportsPoint(curRemainder);
                    clubMember.getClubMemberBO().setCaseSportsPoint(curRemainder);
                    getBaseService().update("caseSportsPoint", curRemainder, id, new AsyncInfo(id));
                } else {
                    curRemainder = preValue;
                }

                double finalCurRemainder = curRemainder;
                // 比赛分消耗记录
                FlowLogger.sportsPointChargeLog(this.playerID, this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId);
                // // 推广员分成每日一表记录 赢的话 只记录盟主那条
                if (finalValue > 0) {
                    FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), finalValue, curRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, 0, roomName, "", String.valueOf(roomKey), 0);
                } else {
                    // // 推广员分成每日一表记录 根据圈主记录map循环记录
                    if (needRecord && MapUtils.isNotEmpty(clubOwnerProfit)) {
                        clubOwnerProfit.entrySet().forEach(k -> {
                                    if (k.getValue() != 0) {
                                        // 推广员分成每日一表记录
                                        FlowLogger.roomPromotionPointLog(this.playerID, CommTime.getNowTimeStringYMD(), this.clubID, unionId, ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), k.getValue(), finalCurRemainder, preValue, resOpType.ordinal(), gameId, classificationRegionId, roomId, 0, roomName, "", String.valueOf(roomKey), k.getKey());
                                    }
                                }
                        );
                    }
                }
                // 赛事比赛分收益记录
                FlowLogger.unionSportsPointProfitLog(unionId, this.clubID, finalValue, sourceType, roomName, roomKey, roomId);
                UnionDynamicBO.insertRoomSportsPoint(playerID, this.clubID, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME_CASEPOINT.value(), unionId, String.valueOf(finalValue), String.valueOf(curRemainder), String.valueOf(roomKey));
                // 前置值
                double scorePointPreValue = CommMath.FormatDouble(clubMember.getClubMemberBO().getCaseSportsPoint());
                // 当前值
                double scorePointCurRemainder = CommMath.addDouble(scorePointPreValue, finalValue);
                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    this.setScorePoint(scorePointCurRemainder);
                    clubMember.getClubMemberBO().setScorePoint(scorePointCurRemainder);
                    getBaseService().update("scorePoint", this.getScorePoint(), id, new AsyncInfo(id));
                }
                //更新共享亲友圈成员信息
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                return true;
            })) {
                // 通知指定玩家更新比赛分
                DispatcherComponent.getInstance().publish(new UnionNotify2PlayerSportsPointEvent(playerID, clubID, SUnion_SportsPoint.make(clubID, playerID, getSportsPoint(), this.getUnionState())));
            }
        } finally {
            DistributedRedisLock.release("sportsPoint" + this.id, uuid);
        }
    }

    /**
     * 获取上级id
     * 0的话 取圈主的id
     *
     * @return
     */
    public Long getUpLevelId() {
        long upLeveLId = upLevelId;
        if (upLeveLId == 0L) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getClubID());
            if (Objects.isNull(club)) {
                return 0L;
            }
            ClubMember createrClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(this.getClubID(), club.getOwnerPlayerId());
            if (Objects.isNull(createrClubMember)) {
                return 0L;
            }
            upLeveLId = createrClubMember.getId();
        }
        return upLeveLId;
    }

    /**
     * getRealUpLevelId
     * 获取真实的上级关系
     *
     * @return
     */
    public Long getRealUpLevelId() {
        return upLevelId;
    }
}
