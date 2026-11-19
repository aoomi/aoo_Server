package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家红包任务时间
 *
 * @author Administrator
 */

@TableName(value = "playerRedPackTaskTime")
@Data
@NoArgsConstructor
public class PlayerRedPackTaskTimeBO extends BaseEntity<PlayerRedPackTaskTimeBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerRedPackTaskTime` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "KEY `pid` (`pid`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家红包任务时间' DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
