package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "ClubmemberStatusLog")
@Data
public class ClubmemberStatusLogFlow extends BaseClarkLogEntity<ClubmemberStatusLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "exePid", comment = "操作者")
    private long exePid;
    @DataBaseField(type = "bigint(20)", fieldname = "exeClubId", comment = "操作者亲友圈Id")
    private long exeClubId;
    @DataBaseField(type = "int(3)", fieldname = "execType", comment = "执行类型")
    private int execType;

    public ClubmemberStatusLogFlow() {
    }

    public ClubmemberStatusLogFlow(long pid, long clubId, long exePid, long exeClubId, int execType) {
        this.pid = pid;
        this.clubId = clubId;
        this.exePid = exePid;
        this.exeClubId = exeClubId;
        this.execType = execType;
    }


    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubmemberStatusLog"
                + "(`serverId`, `timestamp`, `dateTime`,`pid`, `clubId`, `exePid`, `exeClubId`, `execType`)"
                + "values(?,?,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubmemberStatusLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`exePid` bigint(20) NOT NULL DEFAULT '0' COMMENT '操作者',"
                + "`exeClubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '操作者亲友圈Id',"
                + "`execType` int(3) NOT NULL DEFAULT '0' COMMENT '执行类型',"
                + "PRIMARY KEY (`id`),"
                + "KEY `clubID_pid` (`clubId`,`pid`) USING BTREE,"
                + "KEY `ExecClubID_pid` (`exeClubId`,`exePid`) USING BTREE"
                + ") COMMENT='成员身份变化记录' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[8];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = clubId;
        params[5] = exePid;
        params[6] = exeClubId;
        params[7] = execType;
        return params;
    }
}
