package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import com.google.common.collect.Maps;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家抽奖信息
 *
 * @author Administrator
 */
@TableName(value = "playerLuckDrawInfo")
@Data
@NoArgsConstructor
public class PlayerLuckDrawInfoBO extends BaseEntity<PlayerLuckDrawInfoBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "配置Id（0:不清除的配置）")
    private long configId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "count", comment = "计数")
    private int count;
    @DataBaseField(type = "int(11)", fieldname = "conditionValue", comment = "抽奖条件值")
    private int conditionValue;
    @DataBaseField(type = "int(3)", fieldname = "type", comment = "消耗类型：0：免费，1:消耗累计，2：局数累计，3：大赢家次数")
    private int type;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;

    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }

    public void saveCount(int count) {
        if (this.count == count) {
            return;
        }
        this.count = count;
        this.updateTime = CommTime.nowSecond();
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        map.put("count", count);
        map.put("updateTime", updateTime);
        getBaseService().update(map, id, new AsyncInfo(id));
    }

    public void consumeCount(int count) {
        if (this.count == count) {
            return;
        }
        this.count = count;
        getBaseService().update("count", count, id, new AsyncInfo(id));
    }


    public void saveConditionValue(int conditionValue) {
        if (this.conditionValue == conditionValue) {
            return;
        }
        this.conditionValue = conditionValue;
        getBaseService().update("conditionValue", conditionValue, id, new AsyncInfo(id));
    }

    public void gainCount(int count, int conditionValue) {
        if (this.getCount() == count && this.getConditionValue() == conditionValue) {
            return;
        }
        HashMap<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        if (this.getCount() != count) {
            this.setCount(count);
            map.put("count", count);
        }
        if (this.getConditionValue() != conditionValue) {
            this.setConditionValue(conditionValue);
            map.put("conditionValue", conditionValue);
        }
        getBaseService().update(map, id, new AsyncInfo(id));
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerLuckDrawInfo` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '配置Id（0:不清除的配置）',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`count` int(11) NOT NULL DEFAULT '0' COMMENT '计数',"
                + "`conditionValue` int(11) NOT NULL DEFAULT '0' COMMENT '抽奖条件值',"
                + "`type` int(3) NOT NULL DEFAULT '0' COMMENT '消耗类型：0：免费，1:消耗累计，2：局数累计，3：大赢家次数',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "KEY `pid` (`pid`),"
                + "KEY `configId` (`configId`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家抽奖信息' DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    @Override
    public String toString() {
        return "PlayerLuckDrawInfoBO{" +
                "id=" + id +
                ", configId=" + configId +
                ", pid=" + pid +
                ", count=" + count +
                ", conditionValue=" + conditionValue +
                ", type=" + type +
                ", updateTime=" + updateTime +
                ", createTime=" + createTime +
                '}';
    }
}
