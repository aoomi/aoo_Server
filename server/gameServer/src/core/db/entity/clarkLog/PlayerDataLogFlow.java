package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 玩家数据日志
 * 该表可以统计，注册用户、试玩、日活跃、试玩比例、老用户登录人数、老玩家比例、次日留存、7日留存、30日留存
 *
 * @author Administrator
 */
@TableName(value = "PlayerDataLog")
@Data
public class PlayerDataLogFlow extends BaseClarkLogEntity<PlayerDataLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "bigint(20)", fieldname = "accountId", comment = "账号id")
    private long accountId = 0; // 账号id
    @DataBaseField(type = "bigint(20)", fieldname = "reg_time", comment = "玩家注册时间")
    private long reg_time = 0; //玩家注册时间
    @DataBaseField(type = "int(11)", fieldname = "trial_mark", comment = "试玩标记（1、试玩）")
    private int trial_mark = 0; //是否试玩
    @DataBaseField(type = "int(2)", fieldname = "sign", comment = "标记（0:当天首次）")
    private int sign = 0; //标记（0:当天首次）
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;

    public PlayerDataLogFlow() {
    }

    public PlayerDataLogFlow(long pid, long accountId, long reg_time, int trial_mark, int sign, int classificationRegionId) {
        this.pid = pid;
        this.accountId = accountId;
        this.reg_time = reg_time;
        this.trial_mark = trial_mark;
        this.sign = sign;
        this.classificationRegionId = classificationRegionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerDataLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `accountId`, `reg_time`, `trial_mark`, `sign`, `classificationRegionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerDataLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`accountId` bigint(20) NOT NULL DEFAULT '0' COMMENT '账号id',"
                + "`reg_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家注册时间',"
                + "`trial_mark` int(11) NOT NULL DEFAULT '0' COMMENT '试玩标记',"
                + "`sign` int(2) NOT NULL DEFAULT '0' COMMENT '标记（0:当天首次）',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "PRIMARY KEY (`id`),"
                + "KEY `DT_TM` (`date_time`(20),`trial_mark`),"
                + "KEY `RT` (`reg_time`)"
                + ") COMMENT='玩家数据日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[9];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = accountId;
        params[5] = reg_time;
        params[6] = trial_mark;
        params[7] = sign;
        params[8] = classificationRegionId;
        return params;
    }
}
