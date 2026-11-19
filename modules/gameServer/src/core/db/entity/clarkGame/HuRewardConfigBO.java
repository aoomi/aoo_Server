package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 胡牌类型奖励配置
 *
 * @author Huaxing
 */
@TableName(value = "huRewardConfig")
@Data
@NoArgsConstructor
public class HuRewardConfigBO extends BaseEntity<HuRewardConfigBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "int(11)", fieldname = "beginTime", comment = "活动开启时间")
    private int beginTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "活动结束时间")
    private int endTime;
    @DataBaseField(type = "text", fieldname = "prize", comment = "奖品信息")
    private String prize;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `huRewardConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`gameType` int(11) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`beginTime` int(11) NOT NULL DEFAULT '0' COMMENT '活动开启时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '活动结束时间',"
                + "`prize` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '奖品信息',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `gameType` (`gameType`)"
                + ") COMMENT='胡牌类型奖励配置'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
