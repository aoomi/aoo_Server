package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联赛动态记录。
 *
 * @author Administrator
 */
@TableName(value = "unionDynamic")
@Data
@NoArgsConstructor
public class UnionDynamicBO extends BaseEntity<UnionDynamicBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "所属玩家亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "execPid", comment = "执行操作玩家ID")
    private long execPid;
    @DataBaseField(type = "int(11)", fieldname = "execTime", comment = "执行时间")
    private int execTime;
    @DataBaseField(type = "int(3)", fieldname = "execType", comment = "执行类型")
    private int execType;
    @DataBaseField(type = "bigint(20)", fieldname = "execClubId", comment = "执行操作玩家亲友圈Id")
    private long execClubId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "varchar(50)", fieldname = "value", comment = "值")
    private String value = "";
    @DataBaseField(type = "int(11)", fieldname = "dateTime", comment = "时间")
    private int dateTime;
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型（0联盟1亲友圈）")
    private int type;
    @DataBaseField(type = "varchar(50)", fieldname = "curValue", comment = "当前值")
    private String curValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "preValue", comment = "前值")
    private String preValue = "";
    @DataBaseField(type = "varchar(8)", fieldname = "roomKey", comment = "房间key")
    private String roomKey = "";
    @DataBaseField(type = "varchar(150)", fieldname = "msg", comment = "备用消息")
    private String msg = "";
    @DataBaseField(type = "varchar(50)", fieldname = "pidCurValue", comment = "所属玩家当前值")
    private String pidCurValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "pidPreValue", comment = "所属玩家前值")
    private String pidPreValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "pidValue", comment = "所属玩家变化值")
    private String pidValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "execPidCurValue", comment = "执行玩家当前值")
    private String execPidCurValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "execPidPreValue", comment = "执行玩家前值")
    private String execPidPreValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "execPidValue", comment = "执行玩家变化值")
    private String execPidValue = "";
    private String name = "";// 所属玩家名称
    private String execName = "";// 执行操作玩家PID
    private String clubName = "";
    private int clubSign;
    private String execClubName = "";
    private int execClubSign;


    public UnionDynamicBO(long pid, long clubId, int execTime, int execType, int createTime, long unionId, String value, int dateTime, int type, String curValue, String roomKey) {
        this.pid = pid;
        this.clubId = clubId;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.roomKey = roomKey;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, long execClubId, int execTime, int execType, int createTime, long unionId, String value, int dateTime, int type, String curValue, String roomKey) {
        this.pid = pid;
        this.clubId = clubId;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.roomKey = roomKey;
        this.execPid = execPid;
        this.execClubId = execClubId;
    }

    public UnionDynamicBO(long pid, long execPid, long clubId, int execTime, int execType, int createTime, long unionId, String value, int dateTime, String preValue, int type, String curValue, String roomKey, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue) {
        this.pid = pid;
        this.clubId = clubId;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.roomKey = roomKey;
        this.preValue = preValue;
        this.execPid = execPid;
        this.pidCurValue = pidCurValue;
        this.pidPreValue = pidPreValue;
        this.pidValue = pidValue;
        this.execPidCurValue = execPidCurValue;
        this.execPidPreValue = execPidPreValue;
        this.execPidValue = execPidValue;
    }

    public UnionDynamicBO(long pid, long clubId, int execTime, int execType, int createTime, long unionId, String value, int dateTime, int type, String curValue, String roomKey, String msg) {
        this.pid = pid;
        this.clubId = clubId;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.roomKey = roomKey;
        this.msg = msg;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, int createTime, long unionId, String value, int dateTime, int type, String curValue, String roomKey, String msg) {
        this.pid = pid;
        this.execPid = execPid;
        this.clubId = clubId;
        this.execClubId = clubId;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.roomKey = roomKey;
        this.msg = msg;
    }

    public final static void insertRoomSportsPoint(long pid, long clubId, int execTime, int execType, long unionId, String value, String curValue, String roomKey) {
        new UnionDynamicBO(pid, clubId, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, roomKey).insert();
    }

    public final static void insertRoomSportsPoint(long pid, long clubId, long execPid, long execClubId, int execTime, int execType, long unionId, String value, String curValue, String roomKey) {
        new UnionDynamicBO(pid, clubId, execPid, execClubId, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, roomKey).insert();
    }

    public final static void insertRoomSportsPointZhongZhi(long pid, long clubId, int execTime, int execType, long unionId, String value, String curValue, String roomKey) {
        new UnionDynamicBO(pid, clubId, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, roomKey).insert();
    }

    /**
     * 插入保险箱动态消息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param value
     * @param curValue
     * @param roomKey
     */
    public final static void insertCaseSportsRecord(long pid, long execPid, long clubId, int execTime, int execType, long unionId, String value, String curValue, String preValue, String roomKey, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue) {
        new UnionDynamicBO(pid, execPid, clubId, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), preValue, 1, curValue, roomKey, pidCurValue, pidPreValue, pidValue, execPidCurValue, execPidPreValue, execPidValue).insert();
    }

    /**
     * 插入预警值修改信息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param preValue
     * @param curValue
     */
    public final static void insertSportsPointLog(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String preValue, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 1, curValue, preValue).insert();
    }

    public final static void insertPersonalSportsPointLog(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String preValue, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, preValue).insert();
    }

    /**
     * 插入亲友圈预警值修改信息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param preValue
     * @param curValue
     */
    public final static void insertSportsPointLogClub(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String preValue, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, curValue, preValue).insert();
    }

    /**
     * 插入亲友圈预警值修改信息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param preValue
     * @param curValue
     */
    public final static void insertUnionBanGamePlayer(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String preValue, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, curValue, preValue).insert();
    }

    /**
     * 带消息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param value
     * @param curValue
     * @param roomKey
     * @param msg
     */
    public final static void insertRoomSportsPoint(long pid, long clubId, int execTime, int execType, long unionId, String value, String curValue, String roomKey, String msg) {
        new UnionDynamicBO(pid, clubId, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, roomKey, msg).insert();
    }

    /**
     * 带消息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param value
     * @param curValue
     * @param roomKey
     * @param msg
     */
    public final static void insertRoomSportsPoint(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String value, String curValue, String roomKey, String msg) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 3, curValue, roomKey, msg).insert();
    }

    /**
     * 增加推广员归属变更消息
     *
     * @param pid
     * @param clubId
     * @param execTime
     * @param execType
     * @param unionId
     * @param roomKey
     * @param msg
     */
    public final static void insertPromotionBelongChange(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String roomKey, String msg) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 1, "", roomKey, msg).insert();
    }

    /**
     * 增加赛事配置动态
     * <p>
     * 	创建赛事：玩家@玩家名称[ID:@id]创建了赛事；
     * 	解散赛事：玩家@玩家名称[ID:@id]解散了赛事；
     * 	赛事启用：玩家@玩家名称[ID:@id]启用了赛事；
     * 	赛事停用：玩家@玩家名称[ID:@id]停用了赛事；
     * 	魔法表情使用：玩家@玩家名称[ID:@id]允许了赛事房间使用魔法表情；
     * 	魔法表情使用：玩家@玩家名称[ID:@id]允许了赛事房间使用魔法表情；
     * 	开启/关闭加入审核：
     * 	玩家@玩家名称[ID:@id]关闭了加入审核；
     * 	玩家@玩家名称[ID:@id]开启了加入审核；
     * 	开启/加入退出审核：
     * 	玩家@玩家名称[ID:@id]开启了退出审核；
     * 	玩家@玩家名称[ID:@id]关闭了退出审核；
     * 	竞技点清零设置：
     * 	玩家@玩家名称[ID:@id]设置了竞技点清零为每日清零；
     * 	玩家@玩家名称[ID:@id]设置了竞技点清零为每月清零；
     * 	玩家@玩家名称[ID:@id]设置了竞技点清零为每周清零；
     * 	玩家@玩家名称[ID:@id]设置了竞技点清零为不清零；
     * 	联盟名称修改：
     * 	玩家@玩家名称[ID:@id]修改了联盟名称为@新联盟名称；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     * @param value    值
     */
    public final static void insertUnionConfig(long pid, long clubId, long unionId, int execTime, int execType, String value) {
        new UnionDynamicBO(pid, clubId, 0L, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();

    }

    /**
     * 裁判力度修改：玩家@玩家名称[ID:@id] 修改了联赛的裁判力度为@值，修改前为@值；
     * 赛事淘汰值修改：玩家@玩家名称[ID:@id] 修改了联赛的淘汰值为@值，修改前为@值；
     *
     * @param pid
     * @param clubId
     * @param unionId
     * @param execTime
     * @param execType
     * @param value
     * @param curValue
     * @param preValue
     */
    public final static void insertUnionConfig(long pid, long clubId, long unionId, int execTime, int execType, String value, String curValue, String preValue) {
        new UnionDynamicBO(pid, clubId, 0L, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, curValue, preValue).insert();

    }


    /**
     * 	修改积分比例：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]修改了积分比例为12%；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionScorePercent(long pid, long clubId, long unionId, long execPid, int execTime, int execType, String value, String preValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "", preValue).insert();
    }

    /**
     * 	修改积分比例：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]修改了积分比例为12%；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionScorePercentShare(long pid, long clubId, long unionId, long execClubId, long execPid, int execTime, int execType, String value, String preValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, execClubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, preValue, preValue).insert();
    }

    /**
     * 	修改积分比例：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]修改了积分比例为12%；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionScorePercentShare(long pid, long clubId, long unionId, long execPid, int execTime, int execType, String value, String preValue, String msg) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, preValue, preValue, msg).insert();
    }

    /**
     * 增加房间玩法动态
     * 	修改房间玩法：管理员@玩家名称[ID:@id]修改了赛事房间玩法；
     * 	修改房间玩法包含：创建亲友圈房间、修改房间、解散房间；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionGameConfig(long pid, long clubId, long unionId, int execTime, int execType) {
        new UnionDynamicBO(pid, clubId, 0L, execTime, execType, 0L, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();
    }

    /**
     * 增加房间玩法动态
     * 	修改房间玩法：管理员@玩家名称[ID:@id]修改了赛事房间玩法；
     * 	修改房间玩法包含：创建亲友圈房间、修改房间、解散房间；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionGameConfig(long pid, long clubId, long unionId, int execTime, int execType, String value) {
        new UnionDynamicBO(pid, clubId, 0L, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();
    }


    /**
     * 	联盟总竞技点增加/减少：
     * 	联盟总竞技点增加了@数量；
     * 	联盟总竞技点减少了@数量；
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     * @param curValue 当前值
     */
    public final static void insertUnionBackstageSportsPoint(long pid, long clubId, long unionId, long execPid, int execTime, int execType, String value, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();

    }


    /**
     * 	加入赛事：亲友圈@亲友圈名称[ID:@id]被@玩家名称[id：@id]审核加入了赛事；
     * 	关闭审核时：亲友圈@亲友圈名称[ID:@id]加入了赛事
     * 	加入赛事：亲友圈@亲友圈名称[ID:@id]被@玩家名称[id：@id]邀请加入了赛事；
     * 	踢出赛事：亲友圈@亲友圈名称[ID:@id]被@玩家名称[id：@id]踢出了赛事；
     * 	退出赛事：亲友圈@亲友圈名称[ID:@id]退出了赛事；
     * 	需审核时：亲友圈@亲友圈名称[ID:@id]被@玩家名称[id：@id]审核退出了赛事
     * <p>
     * 	设置管理：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]设为了赛事管理；
     * 	取消管理：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]取消了赛事管理；     *
     *
     * @param pid      玩家Pid
     * @param unionId  联赛Id
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertUnionClub(long pid, long clubId, long unionId, long execPid, int execTime, int execType) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), unionId, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();

    }

    public final static void insertUnionClub(long pid, long clubId, long unionId, long execPid, int execTime, int execType, String value) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, "").insert();

    }

    /**
     * 竞技点变动
     * 	亲友圈创建者被赛事管理或盟主增加竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]增加了竞技点值@值；
     * 	亲友圈创建者被赛事管理或盟主增加/减少竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]减少了竞技点值@值；
     *
     * @param pid        玩家Pid
     * @param unionId    联赛Id
     * @param clubId     被操作者亲友圈Id
     * @param execPid    操作者Pid
     * @param execClubId 操作者亲友圈Id
     * @param execTime   操作时间
     * @param execType   操作类型
     * @param value      操作值
     * @param type       类型
     * @param curValue   当前值
     */
    public final static void insertSportsPoint(long pid, long unionId, long clubId, long execPid, long execClubId, int execTime, int execType, String value, int type, String curValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, execClubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), type, curValue).insert();
    }

    /**
     * 竞技点变动
     * 	亲友圈创建者被赛事管理或盟主增加竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]增加了竞技点值@值；
     * 	亲友圈创建者被赛事管理或盟主增加/减少竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]减少了竞技点值@值；
     *
     * @param pid        玩家Pid
     * @param unionId    联赛Id
     * @param clubId     被操作者亲友圈Id
     * @param execPid    操作者Pid
     * @param execClubId 操作者亲友圈Id
     * @param execTime   操作时间
     * @param execType   操作类型
     * @param value      操作值
     * @param type       类型
     * @param curValue   当前值
     */
    public final static void insertSportsPoint(long pid, long unionId, long clubId, long execPid, long execClubId, int execTime, int execType, String value, int type, String curValue, String preValue, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, execClubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), type, curValue, preValue, pidCurValue, pidPreValue, pidValue, execPidCurValue, execPidPreValue, execPidValue).insert();
    }

    /**
     * 竞技点变动
     * 	亲友圈创建者被赛事管理或盟主增加竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]增加了竞技点值@值；
     * 	亲友圈创建者被赛事管理或盟主增加/减少竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]减少了竞技点值@值；
     *
     * @param pid        玩家Pid
     * @param unionId    联赛Id
     * @param clubId     被操作者亲友圈Id
     * @param execPid    操作者Pid
     * @param execClubId 操作者亲友圈Id
     * @param execTime   操作时间
     * @param execType   操作类型
     * @param value      操作值
     * @param type       类型
     * @param curValue   当前值
     */
    public final static void insertSportsPoint(long pid, long unionId, long clubId, long execPid, long execClubId, int execTime, int execType, String value, int type, String curValue, String preValue, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue, String msg, String roomKey) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, execClubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), type, curValue, preValue, pidCurValue, pidPreValue, pidValue, execPidCurValue, execPidPreValue, execPidValue, msg, roomKey).insert();
    }

    /**
     * 竞技点变动
     * 	亲友圈创建者被赛事管理或盟主增加竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]增加了竞技点值@值；
     * 	亲友圈创建者被赛事管理或盟主增加/减少竞技点：玩家@玩家名称[ID:@id]被@玩家名称[id：@id]减少了竞技点值@值；
     *
     * @param pid        玩家Pid
     * @param unionId    联赛Id
     * @param clubId     被操作者亲友圈Id
     * @param execPid    操作者Pid
     * @param execClubId 操作者亲友圈Id
     * @param execTime   操作时间
     * @param execType   操作类型
     * @param value      操作值
     * @param type       类型
     * @param curValue   当前值
     */
    public final static void insertSportsPoint(long pid, long unionId, long clubId, long execPid, long execClubId, int execTime, int execType, String value, int type, String curValue, String preValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, execClubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), type, curValue, preValue).insert();
    }

//    public ClubDynamicBO(long pid, long clubID, long execPid, int execTime, int execType) {
//        this.pid = pid;
//        this.clubID = clubID;
//        this.execPid = execPid;
//        this.execTime = execTime;
//        this.execType = execType;
//        this.createTime = CommTime.nowSecond();
//        this.dateTime = Integer.parseInt(CommTime.getNowTimeStringYMD());
//    }
//
//
//    public ClubDynamicBO(long pid, long clubID, int execTime, int execType) {
//        this.pid = pid;
//        this.clubID = clubID;
//        this.execPid = 0L;
//        this.execTime = execTime;
//        this.execType = execType;
//        this.createTime = CommTime.nowSecond();
//        this.dateTime = Integer.parseInt(CommTime.getNowTimeStringYMD());
//    }


    /**
     * 增加亲友圈房间玩法动态
     *
     * @param pid      玩家Pid
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertClubGameConfig(long pid, long clubId, int execTime, int execType) {
        new UnionDynamicBO(pid, clubId, 0L, execTime, execType, 0L, CommTime.nowSecond(), 0L, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 1, "").insert();
    }

    /**
     * 增加亲友圈动态
     *
     * @param pid      玩家Pid
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertClubDynamic(long pid, long clubId, long execPid, int execTime, int execType) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, 0L, CommTime.nowSecond(), 0L, "", Integer.parseInt(CommTime.getNowTimeStringYMD()), 1, "").insert();
    }

    /**
     * 增加亲友圈动态
     *
     * @param pid      玩家Pid
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertClubDynamic(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String roomKey, String msg) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, roomKey, msg).insert();
    }

    /**
     * 增加亲友圈动态
     *
     * @param pid      玩家Pid
     * @param execTime 执行时间
     * @param execType 执行类型
     */
    public final static void insertClubDynamic(long pid, long clubId, long execPid, int execTime, int execType, long unionId, String roomKey, String msg, String pidPreValue, String pidCurValue) {
        new UnionDynamicBO(pid, clubId, execPid, execTime, execType, clubId, CommTime.nowSecond(), unionId, Integer.parseInt(CommTime.getNowTimeStringYMD()), 0, roomKey, msg, pidPreValue, pidCurValue).insert();
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, String value, int dateTime, int type, String curValue) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, String value, int dateTime, int type, String curValue, String preValue, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.preValue = preValue;
        this.pidCurValue = pidCurValue;
        this.pidPreValue = pidPreValue;
        this.pidValue = pidValue;
        this.execPidCurValue = execPidCurValue;
        this.execPidPreValue = execPidPreValue;
        this.execPidValue = execPidValue;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, String value, int dateTime, int type, String curValue, String preValue, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue, String msg, String roomKey) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.preValue = preValue;
        this.pidCurValue = pidCurValue;
        this.pidPreValue = pidPreValue;
        this.pidValue = pidValue;
        this.execPidCurValue = execPidCurValue;
        this.execPidPreValue = execPidPreValue;
        this.execPidValue = execPidValue;
        this.msg = msg;
        this.roomKey = roomKey;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, String value, int dateTime, int type, String curValue, String preValue) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.preValue = preValue;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, int dateTime, int type, String roomKey, String msg) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.dateTime = dateTime;
        this.type = type;
        this.roomKey = roomKey;
        this.msg = msg;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, int dateTime, int type, String roomKey, String msg, String pidPreValue, String pidCurValue) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.dateTime = dateTime;
        this.type = type;
        this.roomKey = roomKey;
        this.pidPreValue = pidPreValue;
        this.pidCurValue = pidCurValue;
        this.msg = msg;
    }

    public UnionDynamicBO(long pid, long clubId, long execPid, int execTime, int execType, long execClubId, int createTime, long unionId, String value, int dateTime, int type, String curValue, String preValue, String msg) {
        this.pid = pid;
        this.clubId = clubId;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.execClubId = execClubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.type = type;
        this.curValue = curValue;
        this.preValue = preValue;
        this.msg = msg;
    }

    /**
     * 异步保存
     */
    public void insert() {
        long id = this.getBaseService().save(this, new AsyncInfo(this.getPid()));
        if (id < 0) {
            CommLogD.error("UnionDynamicBO save error:" + this.toString());
        }
    }

    @Override
    public String toString() {
        return "UnionDynamicBO{" +
                "id=" + id +
                ", pid=" + pid +
                ", clubId=" + clubId +
                ", execPid=" + execPid +
                ", execTime=" + execTime +
                ", execType=" + execType +
                ", execClubId=" + execClubId +
                ", createTime=" + createTime +
                ", unionId=" + unionId +
                ", value='" + value + '\'' +
                ", dateTime=" + dateTime +
                ", type=" + type +
                ", curValue='" + curValue + '\'' +
                ", preValue='" + preValue + '\'' +
                ", roomKey='" + roomKey + '\'' +
                ", msg='" + msg + '\'' +
                ", pidCurValue='" + pidCurValue + '\'' +
                ", pidPreValue='" + pidPreValue + '\'' +
                ", pidValue='" + pidValue + '\'' +
                ", execPidCurValue='" + execPidCurValue + '\'' +
                ", execPidPreValue='" + execPidPreValue + '\'' +
                ", execPidValue='" + execPidValue + '\'' +
                ", name='" + name + '\'' +
                ", execName='" + execName + '\'' +
                ", clubName='" + clubName + '\'' +
                ", clubSign=" + clubSign +
                ", execClubName='" + execClubName + '\'' +
                ", execClubSign=" + execClubSign +
                '}';
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionDynamic` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家亲友圈Id',"
                + "`execPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行操作玩家ID',"
                + "`execTime` int(11) NOT NULL DEFAULT '0' COMMENT '执行时间',"
                + "`execType` int(3) NOT NULL DEFAULT '0' COMMENT '执行类型',"
                + "`execClubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行操作玩家亲友圈Id',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`value` varchar(50) NOT NULL DEFAULT '' COMMENT '值',"
                + "`dateTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`type` int(2) NOT NULL DEFAULT '0' COMMENT '类型（0联盟1亲友圈）',"
                + "`curValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '当前值',"
                + "`preValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`roomKey` varchar(8) NOT NULL DEFAULT '0' COMMENT '房间key',"
                + "`msg` varchar(150) NOT NULL DEFAULT '' COMMENT '备用消息',"
                + "`pidCurValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '所属玩家当前值',"
                + "`pidPreValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '所属玩家变化值',"
                + "`pidValue` varchar(50) NOT NULL DEFAULT '' COMMENT '所属玩家变化值',"
                + "`execPidCurValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '执行玩家当前值',"
                + "`execPidPreValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '执行玩家前值',"
                + "`execPidValue` varchar(50) NOT NULL DEFAULT '' COMMENT '执行玩家变化值',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId_dateTime_execTime` (`unionId`,`dateTime`,`execTime`) USING BTREE,"
                + "KEY `clubId_dateTime_execTime` (`clubId`,`dateTime`,`execTime`),"
                + "KEY `pid_dateTime_execTime` (`pid`,`dateTime`,`execTime`) USING BTREE"
                + ") COMMENT='联赛动态'  DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }


}
