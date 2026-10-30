package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家好友帮拆红包
 *
 * @author Administrator
 */
@TableName(value = "playerFriendsRedPack")
@Data
@NoArgsConstructor
public class PlayerFriendsRedPackBO extends BaseEntity<PlayerFriendsRedPackBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "int(5)", fieldname = "pondType", comment = "红包类型")
    private int pondType;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "值")
    private int value;

    public void saveValue(int value) {
        if (this.value == value) {
            return;
        }
        this.value = value;
        this.getBaseService().update("value", value, id);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerFriendsRedPack` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`pondType` int(5) NOT NULL DEFAULT '0' COMMENT '红包类型',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '值'," + "PRIMARY KEY (`id`)," + "KEY `PID` (`pid`)"
                + ") COMMENT='玩家好友帮拆红包'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
