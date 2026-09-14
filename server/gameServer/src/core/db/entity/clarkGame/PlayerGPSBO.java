package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@TableName(value = "playerGPS")
@Data
@NoArgsConstructor
public class PlayerGPSBO extends BaseEntity<PlayerGPSBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家id")
    private long pid;
    @DataBaseField(type = "varchar(50)", fieldname = "ip", comment = "IP地址")
    private String ip = ""; //IP地址
    @DataBaseField(type = "varchar(50)", fieldname = "latitude", comment = "纬度")
    private String latitude = ""; //纬度
    @DataBaseField(type = "varchar(50)", fieldname = "longitude", comment = "经度")
    private String longitude = ""; //经度
    @DataBaseField(type = "int(11)", fieldname = "playerTime", comment = "创建角色时间(秒)")
    private int playerTime;
    @DataBaseField(type = "int(11)", fieldname = "familyTime", comment = "创建代理时间(秒)")
    private int familyTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间(秒)")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间(秒)")
    private int updateTime;
    @DataBaseField(type = "int(2)", fieldname = "type", comment = "类型")
    private int type;


    public HashMap<String, Object> getUpdateKeyValue() {
        HashMap<String, Object> keyValue = new HashMap<String, Object>();
        keyValue.put("latitude", latitude);
        keyValue.put("longitude", longitude);
        keyValue.put("familyTime", familyTime);
        keyValue.put("updateTime", updateTime);
        return keyValue;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerGPS` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家id',"
                + "`ip` varchar(50) NOT NULL DEFAULT '' COMMENT 'IP地址',"
                + "`latitude` varchar(50) NOT NULL DEFAULT '' COMMENT '纬度',"
                + "`longitude` varchar(50) NOT NULL DEFAULT '' COMMENT '经度',"
                + "`playerTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建角色时间(秒)',"
                + "`familyTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建代理时间(秒)',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间(秒)',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间(秒)',"
                + "`type` int(2) NOT NULL DEFAULT '0' COMMENT '类型',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='玩家GPS'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
