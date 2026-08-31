package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "ClubPromotionDayActiveChargeLog")
@Data
@Deprecated
public class ClubPromotionDayActiveChargeLogFlow extends BaseClarkLogEntity<ClubPromotionDayActiveChargeLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "int(11)", fieldname = "reason", comment = "产生原因类型")
    private int reason = 0; // 产生原因类型
    @DataBaseField(type = "double(11,2)", fieldname = "num", comment = "数量")
    private double num = 0D; // 数量
    @DataBaseField(type = "double(11,2)", fieldname = "curRemainder", comment = "当前剩余")
    private double curRemainder = 0D; // 当前剩余
    @DataBaseField(type = "double(11,2)", fieldname = "preValue", comment = "前值")
    private double preValue = 0D; // 前值
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型(1:获得,2:消耗)")
    private int type = 0; // 类型(1:获得,2:消耗)
    @DataBaseField(type = "bigint(20)", fieldname = "partnerPid", comment = "推广员Pid")
    private long partnerPid;//

    public ClubPromotionDayActiveChargeLogFlow() {
    }


    public ClubPromotionDayActiveChargeLogFlow(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, long partnerPid) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.reason = reason;
        this.num = num;
        this.curRemainder = curRemainder;
        this.preValue = preValue;
        this.type = type;
        this.partnerPid = partnerPid;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubPromotionDayActiveChargeLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `clubId`, `unionId`, `reason`, `num`, `curRemainder`, `preValue`, `type`, `partnerPid`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubPromotionDayActiveChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈ID',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`reason` int(11) NOT NULL DEFAULT '0' COMMENT '产生原因类型',"
                + "`num` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '数量',"
                + "`curRemainder` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '当前剩余',"
                + "`preValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(2:消耗,3回退 9赛事转出到玩家 10玩家房卡转入赛事 )',"
                + "`partnerPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '推广员Pid',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='亲友圈推广员每天活跃值消费日志' DEFAULT CHARSET=utf8";
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
        params[6] = reason;
        params[7] = num;
        params[8] = curRemainder;
        params[9] = preValue;
        params[10] = type;
        params[11] = partnerPid;
        return params;
    }
}
