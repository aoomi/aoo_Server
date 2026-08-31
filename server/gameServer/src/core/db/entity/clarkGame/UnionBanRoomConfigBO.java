package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 * 赛事禁止房间配置数据表
 *
 * @author Huaxing
 */
@TableName(value = "unionBanRoomConfig")
@Data
@NoArgsConstructor
public class UnionBanRoomConfigBO extends BaseEntity<UnionBanRoomConfigBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "配置Id")
    private long configId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public UnionBanRoomConfigBO(long unionId, long clubId, long pid, long configId, int createTime) {
        this.unionId = unionId;
        this.clubId = clubId;
        this.pid = pid;
        this.configId = configId;
        this.createTime = createTime;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionBanRoomConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '配置Id',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionId_clubId_pid_configId` (`unionId`,`clubId`,`pid`,`configId`)"
                + ") COMMENT='赛事禁止房间配置数据表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public String getInsertSql() {
        return "INSERT ignore INTO unionBanRoomConfig"
                + "(`unionId`,`clubId`,`pid`,`configId`,`createTime`)"
                + "values(?, ?, ?, ?, ?)";
    }

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public Object[] addToBatch() {
        Object[] params = new Object[5];
        params[0] = unionId;
        params[1] = clubId;
        params[2] = pid;
        params[3] = configId;
        params[4] = createTime;
        return params;
    }


}
