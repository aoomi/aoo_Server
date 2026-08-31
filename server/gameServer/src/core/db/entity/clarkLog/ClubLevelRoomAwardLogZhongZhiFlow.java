package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中至 颁奖功能数据表
 * @author Administrator
 */
@TableName(value = "ClubLevelRoomAwardLogZhongZhi")
@Data
@NoArgsConstructor
public class ClubLevelRoomAwardLogZhongZhiFlow extends BaseClarkLogEntity<ClubLevelRoomAwardLogZhongZhiFlow> {
    @DataBaseField(type = "int(11)", fieldname = "server_id", comment = "服务器ID")
    private int server_id;
    @DataBaseField(type = "int(11)", fieldname = "timestamp", comment = "日志时间(时间戳)")
    private int timestamp;
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "int(2)", fieldname = "winner", comment = "大赢家")
    private int winner;
    @DataBaseField(type = "double(11,2)", fieldname = "consume", comment = "消耗值")
    private double consume;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间Id")
    private long roomId;
    @DataBaseField(type = "bigint(20)", fieldname = "upLevelId", comment = "上级推广员id")
    private long upLevelId;
    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "归属id")
    private long memberId;
    @DataBaseField(type = "int(11)", fieldname = "point", comment = "得分")
    private int point;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPoint", comment = "比赛分")
    private double sportsPoint = 0D;
    @DataBaseField(type = "int(11)", fieldname = "setCount", comment = "局数")
    private int setCount;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPointConsume", comment = "消耗比赛分")
    private double sportsPointConsume = 0D;
    @DataBaseField(type = "double(11,2)", fieldname = "roomSportsPointConsume", comment = "房费比赛分")
    private double roomSportsPointConsume = 0D;
    @DataBaseField(type = "double(11,2)", fieldname = "roomAvgSportsPointConsume", comment = "房费比赛分均值")
    private double roomAvgSportsPointConsume = 0D;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "联赛Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "transferId", comment = "从哪个成员Id转移过来的数据")
    private long transferId;
    @DataBaseField(type = "double(11,2)", fieldname = "promotionShareValue", comment = "推广员战绩分成")
    private double promotionShareValue;
    @DataBaseField(type = "bigint(20)", fieldname = "playGamePid", comment = "游戏玩家Pid(推广员分成需要用到)")
    private long playGamePid;
    @DataBaseField(type = "varchar(50)", fieldname = "configName", comment = "房间配置名称")
    private String configName = "";
    public ClubLevelRoomAwardLogZhongZhiFlow(String date_time, long pid, int winner, double consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid, String configName ) {
        this.server_id = Constant.serverIid;
        this.timestamp = CommTime.nowSecond();
        this.date_time = date_time;
        this.pid = pid;
        this.winner = winner;
        this.consume = consume;
        this.roomId = roomId;
        this.upLevelId = upLevelId;
        this.memberId = memberId;
        this.point = point;
        this.sportsPoint = sportsPoint;
        this.setCount = setCount;
        this.sportsPointConsume = sportsPointConsume;
        this.roomSportsPointConsume = roomSportsPointConsume;
        this.roomAvgSportsPointConsume = roomAvgSportsPointConsume;
        this.clubId = clubId;
        this.unionId = unionId;
        this.promotionShareValue = promotionShareValue;
        this.playGamePid = playGamePid;
        this.configName = configName;

    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubLevelRoomAwardLogZhongZhi"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `winner`, `consume`, `roomId`, `upLevelId`, `memberId`, `point`, `sportsPoint`, `setCount`, `sportsPointConsume`, `roomSportsPointConsume`, `roomAvgSportsPointConsume`, `clubId`,`unionID`, `transferId`, `promotionShareValue`,`playGamePid`,`configName`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,? ,? ,? ,? ,?,?,?,?,?,?,?)";
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().insert(this.getInsertSql(), new AsyncInfo(this.getPid()), this.addToBatch());
    }



    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubLevelRoomAwardLogZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`winner` int(2) NOT NULL DEFAULT '0' COMMENT '大赢家',"
                + "`consume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '消耗值',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "`upLevelId` bigint(20) NOT NULL DEFAULT '0' COMMENT '上级推广员id',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '归属id',"
                + "`point` int(11) NOT NULL DEFAULT '0' COMMENT '得分',"
                + "`sportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '比赛分',"
                + "`setCount` int(11) NOT NULL DEFAULT '0' COMMENT '局数',"
                + "`sportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '消耗比赛分',"
                + "`roomSportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房费比赛分',"
                + "`roomAvgSportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房费比赛分均值',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '联盟Id',"
                + "`transferId` bigint(20) NOT NULL DEFAULT '0' COMMENT '从哪个成员Id转移过来的数据',"
                + "`promotionShareValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '推广员战绩分成',"
                + "`playGamePid` bigint(20) NOT NULL DEFAULT '0' COMMENT '游戏玩家Pid(推广员分成需要用到)',"
                + "`configName` varchar(50) NOT NULL DEFAULT '' COMMENT '房间配置名称',"
                + "PRIMARY KEY (`id`),"
                + "KEY `memberId_timestamp` (`memberId`,`timestamp`) USING BTREE,"
                + "KEY `unionId` (`unionId`) USING BTREE,"
                + "KEY `timestamp_pid` (`timestamp`,`pid`) USING BTREE,"
                + "KEY `clubId_memberId` (`clubId`,`memberId`)"
                + ") COMMENT='玩家推广员及下属消耗中至(颁奖记录)' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[21];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = pid;
        params[4] = winner;
        params[5] = consume;
        params[6] = roomId;
        params[7] = upLevelId;
        params[8] = memberId;
        params[9] = point;
        params[10] = sportsPoint;
        params[11] = setCount;
        params[12] = sportsPointConsume;
        params[13] = roomSportsPointConsume;
        params[14] = roomAvgSportsPointConsume;
        params[15] = clubId;
        params[16] = unionId;
        params[17] = transferId;
        params[18] = promotionShareValue;
        params[19] = playGamePid;
        params[20] = configName;
        return params;
    }

    /**
     * 进程Id
     *
     * @return
     */
    @Override
    public int threadId() {
        return DispatcherComponentLogEnum.CLUB_LEVEL_ROOM.id();
    }

    /**
     * 环大小
     *
     * @return
     */
    @Override
    public int bufferSize() {
        return DispatcherComponentLogEnum.CLUB_LEVEL_ROOM.bufferSize();
    }

    @Override
    public String toString() {
        return "ClubLevelRoomAwardLogZhongZhi{" +
                "server_id=" + server_id +
                ", timestamp=" + timestamp +
                ", date_time='" + date_time + '\'' +
                ", pid=" + pid +
                ", winner=" + winner +
                ", consume=" + consume +
                ", roomId=" + roomId +
                ", upLevelId=" + upLevelId +
                ", memberId=" + memberId +
                ", point=" + point +
                ", sportsPoint=" + sportsPoint +
                ", setCount=" + setCount +
                ", sportsPointConsume=" + sportsPointConsume +
                ", roomSportsPointConsume=" + roomSportsPointConsume +
                ", roomAvgSportsPointConsume=" + roomAvgSportsPointConsume +
                ", clubId=" + clubId +
                ", unionId=" + unionId +
                ", transferId=" + transferId +
                ", promotionShareValue=" + promotionShareValue +
                ", playGamePid=" + playGamePid +
                ", configName='" + configName + '\'' +
                '}';
    }
}
