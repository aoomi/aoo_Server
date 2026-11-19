package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "PlayerOnLineChargeLog")
@Data
public class PlayerOnLineChargeLogFlow extends BaseClarkLogEntity<PlayerOnLineChargeLogFlow> {

    @DataBaseField(type = "int(11)", fieldname = "onlineCount", comment = "在线人数")
    private int onlineCount = 0; // 在线人数

    public PlayerOnLineChargeLogFlow() {
    }

    public PlayerOnLineChargeLogFlow(int onlineCount) {
        this.onlineCount = onlineCount;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerOnLineChargeLog"
                + "(`server_id`, `timestamp`, `date_time`, `onlineCount`)"
                + "values(?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerOnLineChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`onlineCount` int(11) NOT NULL DEFAULT '0' COMMENT '在线人数',"


                + "PRIMARY KEY (`id`)"
                + ") COMMENT='Player Online 在线人数' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[4];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = onlineCount;
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
