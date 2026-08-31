package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 玩家切换城市日志
 */
@TableName(value = "PlayerChangeCityLog")
@Data
public class PlayerChangeCityLogFlow extends BaseClarkLogEntity<PlayerChangeCityLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid = 0; // 用户ID
    @DataBaseField(type = "int(11)", fieldname = "pre_city", comment = "之前定位的城市")
    private int pre_city = 0; //最近登录时间
    @DataBaseField(type = "int(11)", fieldname = "cur_city", comment = "现在定位的城市")
    private int cur_city = 0; //最近登出时间


    public PlayerChangeCityLogFlow() {
    }

    public PlayerChangeCityLogFlow(long pid, int pre_city, int cur_city) {
        this.pid = pid;
        this.pre_city = pre_city;
        this.cur_city = cur_city;


    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerChangeCityLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `pre_city`, `cur_city`)"
                + "values(?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerChangeCityLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`pre_city` int(11) NOT NULL DEFAULT '0' COMMENT '之前定位的城市',"
                + "`cur_city` int(11) NOT NULL DEFAULT '0' COMMENT '现在定位的城市',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`),"
                + "KEY `date_time_pid_cur_city` (`date_time`,`pid`,`cur_city`)"
                + ") COMMENT='玩家切换城市日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[6];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = pre_city;
        params[5] = cur_city;
        return params;
    }

    /**
     * 进程Id
     *
     * @return
     */
    @Override
    public int threadId() {
        return DispatcherComponentLogEnum.OTHER_BD_LOG.id();
    }

    /**
     * 环大小
     *
     * @return
     */
    @Override
    public int bufferSize() {
        return DispatcherComponentLogEnum.OTHER_BD_LOG.bufferSize();
    }

}
