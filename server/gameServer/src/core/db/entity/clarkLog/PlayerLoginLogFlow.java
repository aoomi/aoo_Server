package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 玩家登录日志
 */
@TableName(value = "PlayerLoginLog")
@Data
public class PlayerLoginLogFlow extends BaseClarkLogEntity<PlayerLoginLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid = 0; // 用户ID
    @DataBaseField(type = "int(11)", fieldname = "last_login", comment = "最近登录时间")
    private int last_login = 0; //最近登录时间
    @DataBaseField(type = "int(11)", fieldname = "last_logout", comment = "最近登出时间")
    private int last_logout = 0; //最近登出时间
    @DataBaseField(type = "varchar(255)", fieldname = "game_list", comment = "游戏列表")
    private String game_list = "";//游戏列表
    @DataBaseField(type = "varchar(50)", fieldname = "ip", comment = "IP地址")
    private String ip = ""; //IP地址
    @DataBaseField(type = "varchar(50)", fieldname = "latitude", comment = "纬度")
    private String latitude = ""; //纬度
    @DataBaseField(type = "varchar(50)", fieldname = "longitude", comment = "经度")
    private String longitude = ""; //经度
    @DataBaseField(type = "int(11)", fieldname = "address", comment = "地址")
    private String address = ""; //地址

    public PlayerLoginLogFlow() {
    }

    public PlayerLoginLogFlow(long pid, int last_login, int last_logout, String game_list, String ip, String latitude, String longitude, String address) {
        this.pid = pid;
        this.last_login = last_login;
        this.last_logout = last_logout;
        this.game_list = StringUtils.isEmpty(game_list) ? "" : game_list;
        this.ip = StringUtils.isEmpty(ip) ? "" : ip;
        this.latitude = StringUtils.isEmpty(latitude) ? "" : latitude;
        this.longitude = StringUtils.isEmpty(longitude) ? "" : longitude;
        this.address = StringUtils.isEmpty(address) ? "" : address;

    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerLoginLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `last_login`, `last_logout`, `game_list`, `ip`, `latitude`, `longitude`, `address`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerLoginLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`last_login` int(11) NOT NULL DEFAULT '0' COMMENT '最近登录时间',"
                + "`last_logout` int(11) NOT NULL DEFAULT '0' COMMENT '最近登出时间',"
                + "`game_list` varchar(255) NOT NULL DEFAULT '' COMMENT '游戏列表',"
                + "`ip` varchar(50) NOT NULL DEFAULT '' COMMENT 'IP地址',"
                + "`latitude` varchar(50) NOT NULL DEFAULT '' COMMENT '纬度',"
                + "`longitude` varchar(50) NOT NULL DEFAULT '' COMMENT '经度',"
                + "`address` varchar(255) NOT NULL DEFAULT '' COMMENT '地址',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='玩家登录日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[11];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = last_login;
        params[5] = last_logout;
        params[6] = game_list;
        params[7] = ip;
        params[8] = latitude;
        params[9] = longitude;
        params[10] = address;
        return params;
    }
}
