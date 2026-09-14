package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 抽奖记录日志
 */
@TableName(value = "LuckDrawRecordLog")
@Data
@NoArgsConstructor
public class LuckDrawRecordLogFlow extends BaseClarkLogEntity<LuckDrawRecordLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid = 0; // 用户ID
    @DataBaseField(type = "varchar(50)", fieldname = "prizeName", comment = "奖励名称")
    private String prizeName;
    @DataBaseField(type = "int(11)", fieldname = "prizeType", comment = "奖励类型(1:乐豆,2:现金,3:物品,6:房卡)")
    private int prizeType;
    @DataBaseField(type = "int(11)", fieldname = "rewardNum", comment = "奖励数量")
    private int rewardNum;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "中奖时间")
    private int createTime;

    public LuckDrawRecordLogFlow(long pid, String prizeName, int prizeType, int rewardNum, int createTime) {
        this.pid = pid;
        this.prizeName = StringUtils.isEmpty(prizeName) ? "" : prizeName;
        this.prizeType = prizeType;
        this.rewardNum = rewardNum;
        this.createTime = createTime;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO LuckDrawRecordLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `prizeName`, `prizeType`, `rewardNum`, `createTime`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `LuckDrawRecordLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`prizeName` varchar(50) NOT NULL DEFAULT '' COMMENT '奖励名称',"
                + "`prizeType` int(11) NOT NULL DEFAULT '0' COMMENT '奖励类型(1:乐豆,2:现金,3:物品,6:房卡)',"
                + "`rewardNum` int(11) NOT NULL DEFAULT '0' COMMENT '奖励数量',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '中奖时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='抽奖记录日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[8];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = prizeName;
        params[5] = prizeType;
        params[6] = rewardNum;
        params[7] = createTime;
        return params;
    }
}
