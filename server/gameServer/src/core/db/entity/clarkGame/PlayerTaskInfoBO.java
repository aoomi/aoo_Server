package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家任务信息
 *
 * @author Administrator
 */
@TableName(value = "playerTaskInfo")
@Data
@NoArgsConstructor
public class PlayerTaskInfoBO extends BaseEntity<PlayerTaskInfoBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "taskId", comment = "任务Id")
    private long taskId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "count", comment = "计数")
    private int count;
    @DataBaseField(type = "int(3)", fieldname = "state", comment = "状态(0:未完成,1:可领取,2:完成并领取)")
    private int state;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;

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
        getBaseService().update("count", this.count, id, new AsyncInfo(id));
    }

    public void saveState(int state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        getBaseService().update("state", this.state, id, new AsyncInfo(id));
    }

    public boolean setCount(int count) {
        if (this.count == count) {
            return false;
        }
        this.count = count;
        return true;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerTaskInfo` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`taskId` bigint(20) NOT NULL DEFAULT '0' COMMENT '任务ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`count` int(11) NOT NULL DEFAULT '0' COMMENT '计数',"
                + "`state` int(3) NOT NULL DEFAULT '0' COMMENT '状态(0:未完成,1:完成)',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',"
                + "KEY `pid` (`pid`),"
                + "KEY `taskId` (`taskId`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家任务信息' DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
