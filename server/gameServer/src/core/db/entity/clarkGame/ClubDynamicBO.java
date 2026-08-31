package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 亲友圈动态记录。
 *
 * @author Administrator
 */
@TableName(value = "clubDynamic")
@Data
@NoArgsConstructor
public class ClubDynamicBO extends BaseEntity<ClubDynamicBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "bigint(20)", fieldname = "execPid", comment = "执行操作玩家ID")
    private long execPid;
    @DataBaseField(type = "int(11)", fieldname = "execTime", comment = "执行时间")
    private int execTime;
    @DataBaseField(type = "int(3)", fieldname = "execType", comment = "执行类型")
    private int execType;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "dateTime", comment = "时间")
    private int dateTime;
    private String name;// 所属玩家名称
    private String execName;// 执行操作玩家PID


    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getPid()));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubDynamic` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`execPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行操作玩家ID',"
                + "`execTime` int(11) NOT NULL DEFAULT '0' COMMENT '执行时间',"
                + "`execType` int(3) NOT NULL DEFAULT '0' COMMENT '执行类型',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`dateTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `clubID_dateTime_execTime` (`clubID`,`dateTime`,`execTime`) USING BTREE"
                + ") COMMENT='亲友圈动态'  DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }


}
