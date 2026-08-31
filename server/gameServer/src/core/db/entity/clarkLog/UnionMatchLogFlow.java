package core.db.entity.clarkLog;


import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@TableName(value = "UnionMatchLog")
@Data
public class UnionMatchLogFlow extends BaseClarkLogEntity<UnionMatchLogFlow> {
    @DataBaseField(type = "int(5)", fieldname = "rankingId", comment = "排名Id")
    private int rankingId;
    @DataBaseField(type = "varchar(100)", fieldname = "name", comment = "玩家昵称")
    private String name;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户Pid")
    private long pid;
    @DataBaseField(type = "int(6)", fieldname = "clubSign", comment = "亲友圈Id")
    private int clubSign = 0;
    @DataBaseField(type = "varchar(25)", fieldname = "clubName", comment = "所属亲友圈")
    private String clubName;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPoint", comment = "比赛分")
    private double sportsPoint = 0D;
    @DataBaseField(type = "int(11)", fieldname = "roundId", comment = "回合Id")
    private int roundId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;

    public UnionMatchLogFlow() {
    }

    public UnionMatchLogFlow(int rankingId, String name, long pid, int clubSign, String clubName, double sportsPoint, int roundId, long unionId, long clubId) {
        this.rankingId = rankingId;
        this.name = StringUtils.isEmpty(name) ? "" : name;
        this.pid = pid;
        this.clubSign = clubSign;
        this.clubName = StringUtils.isEmpty(clubName) ? "" : clubName;
        this.sportsPoint = sportsPoint;
        this.roundId = roundId;
        this.unionId = unionId;
        this.clubId = clubId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO UnionMatchLog"
                + "(`serverId`, `timestamp`, `dateTime`, `rankingId`, `name`,`pid`, `clubSign`, `clubName`, `sportsPoint`, `roundId`, `unionId`, `clubId`)"
                + "values(?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?)";
    }


    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `UnionMatchLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`rankingId` int(5) NOT NULL DEFAULT '0' COMMENT '排名Id',"
                + "`name` varchar(100) NOT NULL DEFAULT '' COMMENT '玩家昵称',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubSign` int(6) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`clubName` varchar(25) NOT NULL DEFAULT '' COMMENT '所属亲友圈',"
                + "`sportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '比赛分',"
                + "`roundId` int(11) NOT NULL DEFAULT '0'  COMMENT '回合Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid_uid_cid_rid` (`pid`,`unionId`,`clubId`,`roundId`) USING BTREE"
                + ") COMMENT='赛事排名日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[12];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = rankingId;
        params[4] = name;
        params[5] = pid;
        params[6] = clubSign;
        params[7] = clubName;
        params[8] = sportsPoint;
        params[9] = roundId;
        params[10] = unionId;
        params[11] = clubId;
        return params;
    }

    @Override
    public String toString() {
        return "UnionMatchLogFlow{" +
                "rankingId=" + rankingId +
                ", name='" + name + '\'' +
                ", pid=" + pid +
                ", clubSign=" + clubSign +
                ", clubName='" + clubName + '\'' +
                ", sportsPoint=" + sportsPoint +
                ", roundId=" + roundId +
                '}';
    }
}
