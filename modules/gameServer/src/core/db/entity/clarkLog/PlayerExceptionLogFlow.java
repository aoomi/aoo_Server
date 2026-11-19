package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 玩家异常行为日志表
 */
@TableName(value = "PlayerExceptionLog")
@Data
public class PlayerExceptionLogFlow extends BaseClarkLogEntity<PlayerExceptionLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid = 0; // 用户ID
    @DataBaseField(type = "int(2)", fieldname = "exceptionType", comment = "异常类型")
    private int exceptionType = 0;
    @DataBaseField(type = "varchar(255)", fieldname = "content", comment = "异常内容")
    private String content;

    public PlayerExceptionLogFlow() {
    }

    public PlayerExceptionLogFlow(long pid, int exceptionType, String content) {
        this.pid = pid;
        this.exceptionType = exceptionType;
        this.content = content;


    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerExceptionLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `exceptionType`, `content`)"
                + "values(?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerExceptionLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`exceptionType` int(2) NOT NULL DEFAULT '0' COMMENT '异常类型',"
                + "`content` varchar(255) NOT NULL DEFAULT '' COMMENT '异常内容',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='玩家异常行为日志表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[6];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = exceptionType;
        params[5] = content;
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
