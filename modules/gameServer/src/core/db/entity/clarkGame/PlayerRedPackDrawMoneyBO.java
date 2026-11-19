package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家红包提款金额
 *
 * @author Huaxing
 */

@TableName(value = "playerRedPackDrawMoney")
@Data
@NoArgsConstructor
public class PlayerRedPackDrawMoneyBO extends BaseEntity<PlayerRedPackDrawMoneyBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "getTime", comment = "获得时间")
    private int getTime;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "红包金额(单位：分)")
    private int value;
    @DataBaseField(type = "int(11)", fieldname = "before", comment = "提现前红包金额(单位：分)")
    private int before;
    @DataBaseField(type = "int(11)", fieldname = "finalValue", comment = "提现后红包金额(单位：分)")
    private int finalValue;


    public PlayerRedPackDrawMoneyBO(long pid, int getTime, int value, int before, int finalValue) {
        this.pid = pid;
        this.getTime = getTime;
        this.value = value;
        this.before = before;
        this.finalValue = finalValue;
    }

    public void insert_sync() {
        this.getBaseService().save(this);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerRedPackDrawMoney` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`getTime` int(11) NOT NULL DEFAULT '0' COMMENT '获得时间',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '红包(单位：分)',"
                + "`before` int(11) NOT NULL DEFAULT '0' COMMENT '提现前红包(单位：分)',"
                + "`finalValue` int(11) NOT NULL DEFAULT '0' COMMENT '提现后红包(单位：分)',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家红包提款金额'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }


}
