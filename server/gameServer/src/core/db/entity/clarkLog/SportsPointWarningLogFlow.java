package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "SportsPointWarningLogFlow")
@Data
public class SportsPointWarningLogFlow extends BaseClarkLogEntity<SportsPointWarningLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0;//批量移动到哪个人下
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "upPid", comment = "谁下级的竞技点不足")
    private long upPid;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPointWarning", comment = "预警值")
    private double sportsPointWarning;
    @DataBaseField(type = "double(11,2)", fieldname = "calcSportsPointWarning", comment = "计算出来的值")
    private double calcSportsPointWarning;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间Id")
    private long roomId;

    public SportsPointWarningLogFlow() {
    }

    public SportsPointWarningLogFlow(long pid, long clubId, long unionId, long upPid, double sportsPointWarning, double calcSportsPointWarning, long roomId) {
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.upPid = upPid;
        this.sportsPointWarning = sportsPointWarning;
        this.calcSportsPointWarning = calcSportsPointWarning;
        this.roomId = roomId;

    }


    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public long getUnionId() {
        return unionId;
    }

    public void setUnionId(long unionId) {
        this.unionId = unionId;
    }

    public long getUpPid() {
        return upPid;
    }

    public void setUpPid(long upPid) {
        this.upPid = upPid;
    }

    public double getSportsPointWarning() {
        return sportsPointWarning;
    }

    public void setSportsPointWarning(double sportsPointWarning) {
        this.sportsPointWarning = sportsPointWarning;
    }

    public double getCalcSportsPointWarning() {
        return calcSportsPointWarning;
    }

    public void setCalcSportsPointWarning(double calcSportsPointWarning) {
        this.calcSportsPointWarning = calcSportsPointWarning;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO SportsPointWarningLogFlow"
                + "(`serverId`, `timestamp`, `dateTime`,`pid`, `clubId`, `unionId`, `upPid`, `sportsPointWarning`,`calcSportsPointWarning`, `roomId`)"
                + "values(?,?,?,?,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `SportsPointWarningLogFlow` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`upPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '谁下级的竞技点不足',"
                + "`sportsPointWarning` double(11,2) NOT NULL DEFAULT 0  COMMENT '预警值',"
                + "`calcSportsPointWarning` double(11,2) NOT NULL DEFAULT 0  COMMENT '计算出来的值',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='预警值触发信息表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[10];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = clubId;
        params[5] = unionId;
        params[6] = upPid;
        params[7] = sportsPointWarning;
        params[8] = calcSportsPointWarning;
        params[9] = roomId;
        return params;
    }
}
