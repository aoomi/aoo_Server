package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "CasePointChargeLog")
@Data
public class CasePointChargeLogFlow extends BaseClarkLogEntity<CasePointChargeLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "double(11,2)", fieldname = "num", comment = "数量")
    private double num = 0; // 数量
    @DataBaseField(type = "double(11,2)", fieldname = "curRemainder", comment = "竞技点当前剩余")
    private double curRemainder = 0; // 当前剩余
    @DataBaseField(type = "double(11,2)", fieldname = "preValue", comment = "竞技点前值")
    private double preValue = 0; // 前值
    @DataBaseField(type = "double(11,2)", fieldname = "curRemainder", comment = "保险箱当前剩余")
    private double caseSportCurRemainder = 0; // 当前剩余
    @DataBaseField(type = "double(11,2)", fieldname = "preValue", comment = "保险箱前值")
    private double caseSportPreValue = 0; // 前值
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型(1:获得,2:消耗)")
    private int type = 0; // 类型(1:获得,2:消耗)

    public CasePointChargeLogFlow() {
    }

    public CasePointChargeLogFlow(long pid, long clubId, long unionId, double num, double curRemainder, double preValue, double caseSportCurRemainder, double caseSportPreValue, int type) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.num = num;
        this.curRemainder = curRemainder;
        this.preValue = preValue;
        this.type = type;
        this.caseSportCurRemainder = caseSportCurRemainder;
        this.caseSportPreValue = caseSportPreValue;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO CasePointChargeLog"
                + "(`serverId`, `timestamp`, `dateTime`, `pid`, `clubId`,`unionId`,  `num`, `curRemainder`, `preValue`,  `caseSportCurRemainder`, `caseSportPreValue`,`type`)"
                + "values(?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `CasePointChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`num` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '数量',"
                + "`curRemainder` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '竞技点当前剩余',"
                + "`preValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '竞技点前值',"
                + "`caseSportCurRemainder` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '保险箱当前剩余',"
                + "`caseSportPreValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '保险箱前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(1:获得,2:消耗)',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId_dateTime_pid` (`unionId`,`dateTime`,`pid`) USING BTREE"
                + ") COMMENT='竞技点消费日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[12];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = clubId;
        params[5] = unionId;
        params[6] = num;
        params[7] = curRemainder;
        params[8] = preValue;
        params[9] = caseSportCurRemainder;
        params[10] = caseSportPreValue;
        params[11] = type;
        return params;
    }
}
