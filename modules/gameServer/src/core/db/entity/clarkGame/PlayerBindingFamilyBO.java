package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;


@TableName(value = "playerBindingFamily")
@Data
@NoArgsConstructor
public class PlayerBindingFamilyBO extends BaseEntity<PlayerBindingFamilyBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家账号ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "状态 (1工会、会长、一级代理、2副会长、3普通玩家)")
    private int type;
    @DataBaseField(type = "varchar(50)", fieldname = "value", comment = "传入值")
    private String value;
    @DataBaseField(type = "int(11)", fieldname = "time", comment = "时间")
    private int time;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerBindingFamily` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家账号ID',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '状态 (1工会、会长、一级代理、2副会长、3普通玩家)',"
                + "`value` varchar(50) NOT NULL DEFAULT '' COMMENT '传入值',"
                + "`time` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='公会绑定'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }

}
