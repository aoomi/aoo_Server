package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户的扩展货币
 *
 * @author Huaxing
 */
@TableName(value = "playerCurrency")
@Data
@NoArgsConstructor
public class PlayerCurrencyBO extends BaseEntity<PlayerCurrencyBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid;
    @DataBaseField(type = "int(5)", fieldname = "prizeType", comment = "类型")
    private int prizeType = 0;
    @DataBaseField(type = "int(11)", fieldname = "prizeValue", comment = "值")
    private int prizeValue = 0;


    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, new AsyncInfo(id));
    }

    public void savePrizeType(int prizeType) {
        if (prizeType == this.prizeType) {
            return;
        }
        this.prizeType = prizeType;
        getBaseService().update("prizeType", prizeType, new AsyncInfo(id));
    }

    public void savePrizeValue(int prizeValue) {
        if (prizeValue == this.prizeValue) {
            return;
        }
        this.prizeValue = prizeValue;
        getBaseService().update("prizeValue", prizeValue, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerCurrency` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`prizeType` int(5) NOT NULL DEFAULT '0' COMMENT '类型',"
                + "`prizeValue` int(11) NOT NULL DEFAULT '0' COMMENT '值',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='用户货币'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
