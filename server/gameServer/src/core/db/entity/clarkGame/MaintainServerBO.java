package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "maintainServer")
@Data
@NoArgsConstructor
public class MaintainServerBO extends BaseEntity<MaintainServerBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "serverId", comment = "服务器ID")
    private int serverId;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始维护时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束维护时间")
    private int endTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `maintainServer` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始维护时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束维护时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='服务器动态配置'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
