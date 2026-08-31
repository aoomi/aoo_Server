package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 比赛分异常记录
 */
@TableName(value = "SportsPointErrorLog")
@Data
public class SportsPointErrorLogFlow extends BaseClarkLogEntity<SportsPointErrorLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "double(11,2)", fieldname = "correctValue", comment = "正确值")
    private double correctValue = 0; // 正确值
    @DataBaseField(type = "double(11,2)", fieldname = "errorValue", comment = "错误值")
    private double errorValue = 0; // 错误值

    public SportsPointErrorLogFlow() {
    }

    public SportsPointErrorLogFlow(long unionId, double correctValue, double errorValue) {
        this.unionId = unionId;
        this.correctValue = correctValue;
        this.errorValue = errorValue;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO SportsPointErrorLog"
                + "(`serverId`, `timestamp`, `dateTime`, `unionId`, `correctValue`, `errorValue`)"
                + "values(?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `SportsPointErrorLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`correctValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '正确值',"
                + "`errorValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '错误值',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='比赛分异常记录' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[6];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = unionId;
        params[4] = correctValue;
        params[5] = errorValue;
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
