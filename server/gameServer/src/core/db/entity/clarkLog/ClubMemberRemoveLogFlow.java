package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 亲友圈成员移除
 */
@TableName(value = "ClubMemberRemoveLog")
@Data
public class ClubMemberRemoveLogFlow extends BaseClarkLogEntity<ClubMemberRemoveLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "成员Id")
    private long memberId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "reason", comment = "产生原因类型")
    private int reason = 0; // 产生原因类型
    @DataBaseField(type = "int(11)", fieldname = "status", comment = "当前状态")
    private int status = 0; // 当前剩余
    @DataBaseField(type = "int(11)", fieldname = "isminister", comment = "职务 0普通会员 1管理 2创建者")
    private int isminister = 0; // 职务 0普通会员 1管理 2创建者
    @DataBaseField(type = "bigint(20)", fieldname = "exePid", comment = "操作者PID")
    private long exePid;//操作者PID
    @DataBaseField(type = "int(11)", fieldname = "level", comment = "等级(0:默认普通成员)")
    private int level;
    @DataBaseField(type = "bigint(20)", fieldname = "upLevelId", comment = "上个等级id")
    private long upLevelId;//
    @DataBaseField(type = "int(11)", fieldname = "creattime", comment = "申请时间")
    private int creattime;// 申请时间
    @DataBaseField(type = "int(11)", fieldname = "updatetime", comment = "处理时间")
    private int updatetime;// 处理时间
    @DataBaseField(type = "int(11)", fieldname = "deletetime", comment = "踢出时间")
    private int deletetime;// 踢出时间

    public ClubMemberRemoveLogFlow() {
    }

    public ClubMemberRemoveLogFlow(long memberId, long pid, long clubId, int reason, int status, int isminister, long exePid, int level, long upLevelId, int creattime, int updatetime, int deletetime) {
        this.memberId = memberId;
        this.pid = pid;
        this.clubId = clubId;
        this.reason = reason;
        this.status = status;
        this.isminister = isminister;
        this.exePid = exePid;
        this.level = level;
        this.upLevelId = upLevelId;
        this.creattime = creattime;
        this.updatetime = updatetime;
        this.deletetime = deletetime;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubMemberRemoveLog"
                + "(`server_id`, `timestamp`, `date_time`, `memberId`, `pid`, `clubId`, `reason`, `status`, `isminister`, `exePid`, `level`, `upLevelId`, `creattime`, `updatetime`, `deletetime`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubMemberRemoveLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '成员Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`reason` int(11) NOT NULL DEFAULT '0' COMMENT '产生原因类型',"
                + "`status` int(2) NOT NULL DEFAULT '0' COMMENT '当前状态',"
                + "`isminister` int(11) NOT NULL DEFAULT '0' COMMENT '职务 0普通会员 1管理 2创建者',"
                + "`exePid` bigint(20) NOT NULL DEFAULT '0' COMMENT '操作者PID',"
                + "`level` int(11) NOT NULL DEFAULT '0' COMMENT '等级(0:默认普通成员)',"
                + "`upLevelId` bigint(20) NOT NULL DEFAULT '0' COMMENT '上个等级id',"
                + "`creattime` int(11) NOT NULL DEFAULT '0' COMMENT '申请时间',"
                + "`updatetime` int(11) NOT NULL DEFAULT '0' COMMENT '处理时间',"
                + "`deletetime` int(11) NOT NULL DEFAULT '0' COMMENT '踢出时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='亲友圈成员移除日志表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[15];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = memberId;
        params[4] = pid;
        params[5] = clubId;
        params[6] = reason;
        params[7] = status;
        params[8] = isminister;
        params[9] = exePid;
        params[10] = level;
        params[11] = upLevelId;
        params[12] = creattime;
        params[13] = updatetime;
        params[14] = deletetime;
        return params;
    }
}
