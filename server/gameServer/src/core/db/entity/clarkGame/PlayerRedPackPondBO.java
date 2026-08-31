package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家红包池
 *
 * @author Administrator
 */

@TableName(value = "playerRedPackPond")
@Data
@NoArgsConstructor
public class PlayerRedPackPondBO extends BaseEntity<PlayerRedPackPondBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "toPid", comment = "显示目标玩家ID")
    private long toPid;
    @DataBaseField(type = "int(11)", fieldname = "getTime", comment = "获得时间")
    private int getTime;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "红包金额(单位：分)")
    private int value;
    @DataBaseField(type = "varchar(255)", fieldname = "rewards", comment = "奖励方式")
    private String rewards;
    @DataBaseField(type = "int(2)", fieldname = "pondType", comment = "红包池类型")
    private int pondType;


    public PlayerRedPackPondBO(long pid, long toPid, int getTime, int value, String rewards, int pondType) {
        this.id = 0L;
        this.pid = pid;
        this.toPid = toPid;
        this.getTime = getTime;
        this.value = value;
        this.rewards = rewards;
        this.pondType = pondType;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerRedPackPond` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`toPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '显示目标玩家ID',"
                + "`getTime` int(11) NOT NULL DEFAULT '0' COMMENT '获得时间',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '红包金额(单位：分)',"
                + "`rewards` varchar(255) NOT NULL DEFAULT '' COMMENT '奖励方式',"
                + "`pondType` int(2) NOT NULL DEFAULT '0' COMMENT '红包池类型',"
                + "KEY `pid` (`pid`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家红包池' DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
