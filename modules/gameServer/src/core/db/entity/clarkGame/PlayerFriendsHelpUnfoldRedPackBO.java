package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家任务信息
 *
 * @author Administrator
 */

@TableName(value = "playerFriendsHelpUnfoldRedPack")
@Data
@NoArgsConstructor
public class PlayerFriendsHelpUnfoldRedPackBO extends BaseEntity<PlayerFriendsHelpUnfoldRedPackBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "taskId", comment = "任务Id")
    private long taskId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "taskType", comment = "任务类型")
    private int taskType;
    @DataBaseField(type = "int(3)", fieldname = "state", comment = "状态(0:未完成,1:可领取,2:完成并领取)")
    private int state;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "红包金额(单位：分)")
    private int value;
    @DataBaseField(type = "int(11)", fieldname = "targetType", comment = "任务目标类型")
    private int targetType;


    public void saveState(int state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        this.getBaseService().update("state", state, id);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerFriendsHelpUnfoldRedPack` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`taskId` bigint(20) NOT NULL DEFAULT '0' COMMENT '任务ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`taskType` int(11) NOT NULL DEFAULT '0' COMMENT '任务类型',"
                + "`state` int(3) NOT NULL DEFAULT '0' COMMENT '状态(0:未完成,1:完成)',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '红包金额(单位：分)',"
                + "`targetType` int(11) NOT NULL DEFAULT '0' COMMENT '任务目标类型',"
                + "KEY `pid` (`pid`),"
                + "KEY `taskId` (`taskId`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家任务信息' DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }

    @Override
    public String toString() {
        return "PlayerFriendsHelpUnfoldRedPackBO [id=" + id + ", taskId=" + taskId + ", pid=" + pid + ", taskType="
                + taskType + ", state=" + state + ", value=" + value + ", targetType=" + targetType + "]";
    }


}
