package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联赛通知列表
 *
 * @author Administrator
 */
@TableName(value = "unionNotify")
@Data
@NoArgsConstructor
public class UnionNotifyBO extends BaseEntity<UnionNotifyBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "当前玩家Pid")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "所在亲友圈id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "联赛Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "execPid", comment = "操作者Pid")
    private long execPid;
    @DataBaseField(type = "int(3)", fieldname = "execType", comment = "执行类型")
    private int execType;
    @DataBaseField(type = "int(11)", fieldname = "execTime", comment = "执行时间")
    private int execTime;

    public UnionNotifyBO(long pid, long clubId, long unionId, int execType) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.execType = execType;
        this.execTime = CommTime.nowSecond();
    }

    public UnionNotifyBO(long pid, long clubId, long unionId, long execPid, int execType) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.execPid = execPid;
        this.execType = execType;
        this.execTime = CommTime.nowSecond();
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getClubId()));
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionNotify` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '当前玩家Pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '所在亲友圈id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '联赛Id',"
                + "`execPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '操作者Pid',"
                + "`execType` int(3) NOT NULL DEFAULT '0' COMMENT '执行类型',"
                + "`execTime` int(11) NOT NULL DEFAULT '0' COMMENT '执行时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `PID` (`pid`)"
                + ") COMMENT='联赛通知列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }


}
