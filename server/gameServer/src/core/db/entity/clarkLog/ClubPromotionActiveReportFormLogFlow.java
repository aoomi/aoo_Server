package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "ClubPromotionActiveReportFormLog")
@Data
@Deprecated
public class ClubPromotionActiveReportFormLogFlow extends BaseClarkLogEntity<ClubPromotionActiveReportFormLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "推广员Pid")
    private long pid;
    @DataBaseField(type = "double(11,2)", fieldname = "value", comment = "活跃值")
    private double value;
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;

    public ClubPromotionActiveReportFormLogFlow() {
    }

    public ClubPromotionActiveReportFormLogFlow(long clubId, long pid, double value, String date_time) {
        this.clubId = clubId;
        this.pid = pid;
        this.value = value;
        this.date_time = date_time;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubPromotionActiveReportFormLog"
                + "(`server_id`, `timestamp`, `date_time`, `clubId`, `pid`, `value`)"
                + "values(?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubPromotionActiveReportFormLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '推广员Pid',"
                + "`value` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '活跃值',"
                + "PRIMARY KEY (`id`),"
                + "KEY `clubId_partnerPid_date_time` (`clubId`,`pid`,`date_time`)"
                + ") COMMENT='亲友圈推广员活跃报表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[6];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = clubId;
        params[4] = pid;
        params[5] = value;
        return params;
    }
}
