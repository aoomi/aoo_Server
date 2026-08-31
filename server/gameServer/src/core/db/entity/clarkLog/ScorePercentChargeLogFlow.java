package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 积分修改记录
 */
@TableName(value = "ScorePercentChargeLog")
@Data
public class ScorePercentChargeLogFlow extends BaseClarkLogEntity<ScorePercentChargeLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户Pid")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "curRemainder", comment = "当前剩余")
    private int curRemainder = 0; // 当前剩余
    @DataBaseField(type = "int(11)", fieldname = "preValue", comment = "前值")
    private int preValue = 0; // 前值
    @DataBaseField(type = "bigint(20)", fieldname = "exePid", comment = "操作者pid")
    private long exePid;

    public ScorePercentChargeLogFlow() {
    }

    public ScorePercentChargeLogFlow(long pid, long unionId, long clubId, int curRemainder, int preValue, long exePid) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.curRemainder = curRemainder;
        this.preValue = preValue;
        this.exePid = exePid;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ScorePercentChargeLog"
                + "(`serverId`,`timestamp`,`dateTime`,`pid`,`unionId`,`clubId`,`curRemainder`,`preValue`,`exePid`)"
                + "values(?, ?, ?, ?, ?, ?,?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ScorePercentChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`curRemainder` int(11) NOT NULL DEFAULT '0' COMMENT '当前剩余',"
                + "`preValue` int(11) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`exePid` bigint(20) NOT NULL DEFAULT '0' COMMENT '操作者pid',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='积分修改记录日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[9];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = unionId;
        params[5] = clubId;
        params[6] = curRemainder;
        params[7] = preValue;
        params[8] = exePid;
        return params;
    }
}
