package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "RoomPromotionPointLog", dbDay = TableName.DbDayEnum.EVERY_DAY)
@Data
public class RoomPromotionPointLogFlow extends BaseClarkLogEntity<RoomPromotionPointLogFlow> {
    @DataBaseField(type = "int(11)", fieldname = "timestamp", comment = "日志时间戳")
    private int timestamp;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "分成玩家pid(得到分成的玩家)")
    private long pid = 0;
    @DataBaseField(type = "varchar(20)", fieldname = "dateTime", comment = "日志时间(yyyymmdd)")
    private String dateTime;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "int(11)", fieldname = "reason", comment = "产生原因类型")
    private int reason = 0; // 产生原因类型
    @DataBaseField(type = "double(11,2)", fieldname = "num", comment = "数量")
    private double num = 0; // 数量
    @DataBaseField(type = "double(11,2)", fieldname = "curRemainder", comment = "当前剩余")
    private double curRemainder = 0; // 当前剩余
    @DataBaseField(type = "double(11,2)", fieldname = "preValue", comment = "前值")
    private double preValue = 0; // 前值
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型(1:获得,2:消耗)")
    private int type = 0; // 类型(1:获得,2:消耗)
    @DataBaseField(type = "int(11)", fieldname = "gameId", comment = "游戏Id")
    private int gameId;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间Id")
    private long roomId;
    @DataBaseField(type = "bigint(20)", fieldname = "execPid", comment = "参与游戏的玩家(盟主的话该字段不计)")
    private long execPid = 0;
    @DataBaseField(type = "varchar(150)", fieldname = "roomName", comment = "房间名称")
    private String roomName = "";
    @DataBaseField(type = "varchar(8)", fieldname = "roomKey", comment = "房间key")
    private String roomKey = "";
    @DataBaseField(type = "varchar(150)", fieldname = "msg", comment = "备用消息")
    private String msg = "";
    @DataBaseField(type = "bigint(20)", fieldname = "reasonPid", comment = "因为哪个玩家才产生的这个记录(分成上级关系,圈主找不到盟主的话-1)")
    private long reasonPid = 0;

    public RoomPromotionPointLogFlow() {
    }

    public RoomPromotionPointLogFlow(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, String roomKey, long reasonPid) {
        this.pid = pid;
        this.dateTime = dateTime;
        this.clubId = clubId;
        this.unionId = unionId;
        this.reason = reason;
        this.num = num;
        this.curRemainder = curRemainder;
        this.preValue = preValue;
        this.type = type;
        this.gameId = gameId;
        this.classificationRegionId = classificationRegionId;
        this.roomId = roomId;
        this.execPid = execPid;
        this.roomName = roomName;
        this.msg = msg;
        this.roomKey = roomKey;
        this.reasonPid = reasonPid;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO RoomPromotionPointLog" + this.getDateTime()
                + "(`serverId`, `timestamp`, `dateTime`, `pid`, `clubId`,`unionId`, `reason`, `num`, `curRemainder`, `preValue`, `type`,`gameId`,`classificationRegionId`, `roomId`, `execPid`, `roomName`,`msg`,`roomKey`,`reasonPid`)"
                + "values(?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?,?,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        return getCreateTableSQL(CommTime.getNowTimeYMD());
    }

    public static String getCreateTableSQL(String dateTime) {
        String sql = "CREATE TABLE IF NOT EXISTS `RoomPromotionPointLog" + dateTime + "` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '分成玩家pid(得到分成的玩家)',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`reason` int(11) NOT NULL DEFAULT '0' COMMENT '产生原因类型',"
                + "`num` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '数量',"
                + "`curRemainder` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '当前剩余',"
                + "`preValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(1:获得,2:消耗)',"
                + "`gameId` int(11) NOT NULL DEFAULT '-1' COMMENT '游戏Id',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "`execPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '参与游戏的玩家(盟主的话该字段不计)',"
                + "`reasonPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '因为哪个玩家才产生的这个记录(分成上级关系,圈主找不到盟主的话-1)',"
                + "`roomName` varchar(150) NOT NULL DEFAULT '' COMMENT '房间名称',"
                + "`roomKey` varchar(8) NOT NULL DEFAULT '0' COMMENT '房间key',"
                + "`msg` varchar(150) NOT NULL DEFAULT '' COMMENT '备用消息',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid_roomId_execPid` (`pid`,`roomId`,`execPid`) USING BTREE,"
                + "KEY `dateTime_roomId` (`dateTime`,`roomId`) USING BTREE"
                + ") COMMENT='房费分成详细表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[19];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = clubId;
        params[5] = unionId;
        params[6] = reason;
        params[7] = num;
        params[8] = curRemainder;
        params[9] = preValue;
        params[10] = type;
        params[11] = gameId;
        params[12] = classificationRegionId;
        params[13] = roomId;
        params[14] = execPid;
        params[15] = roomName;
        params[16] = msg;
        params[17] = roomKey;
        params[18] = reasonPid;
        return params;
    }
}
