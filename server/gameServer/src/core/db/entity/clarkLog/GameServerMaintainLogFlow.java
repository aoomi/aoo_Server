package core.db.entity.clarkLog;


import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "GameServerMaintainLog")
@Data
public class GameServerMaintainLogFlow extends BaseClarkLogEntity<GameServerMaintainLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "startServerTime", comment = "开启服务时间")
    private long startServerTime = 0; // 玩家pid
    @DataBaseField(type = "bigint(20)", fieldname = "stopServerTime", comment = "关闭服务时间")
    private long stopServerTime = 0; // 产生原因类型
    @DataBaseField(type = "int(11)", fieldname = "spacing", comment = "间隔时间(s)")
    private int spacing = 0;
    @DataBaseField(type = "int(11)", fieldname = "httpPost", comment = "http端口")
    private int httpPost;
    @DataBaseField(type = "int(11)", fieldname = "clientPort", comment = "客户端端口")
    private int clientPort;
    @DataBaseField(type = "varchar(50)", fieldname = "name", comment = "名称")
    private String name;
    @DataBaseField(type = "int(11)", fieldname = "pid", comment = "进程Pid")
    private int pid;

    public GameServerMaintainLogFlow() {
    }

    public GameServerMaintainLogFlow(long startServerTime, long stopServerTime, int spacing, int httpPost, int clientPort, String name, int pid) {
        this.startServerTime = startServerTime;
        this.stopServerTime = stopServerTime;
        this.spacing = spacing;
        this.httpPost = httpPost;
        this.clientPort = clientPort;
        this.name = name;
        this.pid = pid;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO GameServerMaintainLog"
                + "(`server_id`, `timestamp`, `date_time`, `startServerTime`, `stopServerTime`, `spacing`, `httpPost`, `clientPort`, `name`, `pid`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `GameServerMaintainLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`startServerTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '开启服务时间',"
                + "`stopServerTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '关闭服务时间',"
                + "`spacing` int(11) NOT NULL DEFAULT '0' COMMENT '间隔时间(s)',"
                + "`httpPost` int(11) NOT NULL DEFAULT '0' COMMENT 'http端口',"
                + "`clientPort` int(11) NOT NULL DEFAULT '0' COMMENT '客户端端口',"
                + "`name` varchar(50) NOT NULL DEFAULT '' COMMENT '名称',"
                + "`pid` int(11) NOT NULL DEFAULT '0' COMMENT '进程Pid',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='服务端维护日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[10];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = startServerTime;
        params[4] = stopServerTime;
        params[5] = spacing;
        params[6] = httpPost;
        params[7] = clientPort;
        params[8] = name;
        params[9] = pid;
        return params;
    }

    /**
     * 结束或批量
     *
     * @return
     */
    @Override
    public boolean endOfBatch() {
        return true;
    }

}
