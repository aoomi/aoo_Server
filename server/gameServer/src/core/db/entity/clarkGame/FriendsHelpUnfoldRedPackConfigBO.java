package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友帮拆红包配置
 *
 * @author Administrator
 */
@TableName(value = "friendsHelpUnfoldRedPackConfig")
@Data
@NoArgsConstructor
public class FriendsHelpUnfoldRedPackConfigBO extends BaseEntity<FriendsHelpUnfoldRedPackConfigBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "int(2)", fieldname = "limitType", comment = "限制类型（1：每日，2：每周，3：每月）")
    private int limitType;
    @DataBaseField(type = "int(11)", fieldname = "limitTime", comment = "限制时间")
    private int limitTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `friendsHelpUnfoldRedPackConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`limitType` int(2) NOT NULL DEFAULT '0' COMMENT '限制类型（1：每日，2：每周，3：每月）',"
                + "`limitTime` int(11) NOT NULL DEFAULT '0' COMMENT '限制时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='好友帮拆红包配置' DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }

    @Override
    public String toString() {
        return "FriendsHelpUnfoldRedPackConfigBO [id=" + id + ", startTime=" + startTime + ", endTime=" + endTime
                + ", limitType=" + limitType + ", limitTime=" + limitTime + "]";
    }


}

