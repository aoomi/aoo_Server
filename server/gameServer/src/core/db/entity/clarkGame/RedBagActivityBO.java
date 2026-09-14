package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "RedBagActivity")
@Data
@NoArgsConstructor
public class RedBagActivityBO extends BaseEntity<RedBagActivityBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "crowd", comment = "活动人群 1:所有玩家   2 指定代理 3除指定代理外的玩家")
    private int crowd; //活动人群 1:所有玩家   2 指定代理
    @DataBaseField(type = "varchar(255)", fieldname = "crowd_daili", comment = "以参与活动代理id 多个ID用逗号隔开")
    private String crowd_daili; //可以参与活动代理id
    @DataBaseField(type = "varchar(255)", fieldname = "game_type", comment = "适用的游戏类型 多个用逗号隔开 所有游戏用-1标识")
    private String game_type; //适用的游戏id
    @DataBaseField(type = "int(11)", fieldname = "create_time", comment = "活动创建时间")
    private int create_time; //活动创建时间
    @DataBaseField(type = "int(11)", fieldname = "begin_time", comment = "活动开始时间")
    private int begin_time; //活动开始时间
    @DataBaseField(type = "int(11)", fieldname = "end_time", comment = "活动结束时间")
    private int end_time; //活动结束时间
    @DataBaseField(type = "int(11)", fieldname = "updatetime", comment = "活动更新时间")
    private int updatetime; //活动更新时间
    @DataBaseField(type = "int(11)", fieldname = "max_money", comment = "金额上线（以分为单位）")
    private int max_money; //金额上线（以分为单位）
    @DataBaseField(type = "int(11)", fieldname = "every_money", comment = "每次的金钱不超过总额的多少  百分比 如40 是总额的百分之四十")
    private int every_money; //百分比

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `RedBagActivity` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`crowd` int(11) NOT NULL DEFAULT '0' COMMENT '活动人群 1:所有玩家   2 指定代理   3 指定代理外',"
                + "`crowd_daili` varchar(255) NOT NULL DEFAULT '' COMMENT '可以参与活动代理id',"
                + "`game_type` varchar(255) NOT NULL DEFAULT '' '适用的游戏id 所有游戏用-1标识',"
                + "`create_time` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`begin_time` int(11) NOT NULL DEFAULT '0' COMMENT '活动开始时间',"
                + "`end_time` int(11) NOT NULL DEFAULT '0' COMMENT '活动结束时间',"
                + "`updatetime` int(11) DEFAULT NULL DEFAULT '0' COMMENT '更新时间',"
                + "`max_money` int(11) NOT NULL DEFAULT '0' COMMENT '金额上线（以分为单位）',"
                + "`every_money` int(11) NOT NULL DEFAULT '20' COMMENT '每次的金钱不超过总额的多少  百分比 如40 是总额的百分之四十',"
                + "PRIMARY KEY (`id`),"
                + "KEY `id` (`id`)"
                + ") COMMENT='红包活动'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }

}
