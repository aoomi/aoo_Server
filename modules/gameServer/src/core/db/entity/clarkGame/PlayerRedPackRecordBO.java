package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家红包记录
 *
 * @author Administrator
 */

@TableName(value = "playerRedPackRecord")
@Data
@NoArgsConstructor
public class PlayerRedPackRecordBO extends BaseEntity<PlayerRedPackRecordBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "toPid", comment = "显示目标玩家ID")
    private long toPid;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "红包金额(单位：分)")
    private int value;
    @DataBaseField(type = "int(2)", fieldname = "isHu", comment = "是否胡牌(0:没有,1:胡牌)")
    private int isHu;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerRedPackRecord` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`toPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '显示目标玩家ID',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '红包金额(单位：分)',"
                + "`isHu` int(2) NOT NULL DEFAULT '0' COMMENT '是否胡牌(0:没有,1:胡牌)',"
                + "KEY `pid` (`pid`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家红包记录' DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
