package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 玩家圈卡真实消耗日志
 *
 * @author Administrator
 */
@TableName(value = "PlayerClubCardLog")
@Data
@Deprecated
public class PlayerClubCardLogFlow extends BaseClarkLogEntity<PlayerClubCardLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "int(11)", fieldname = "num", comment = "数量")
    private int num = 0; // 数量
    @DataBaseField(type = "int(11)", fieldname = "cur_remainder", comment = "当前剩余")
    private int cur_remainder = 0; // 当前剩余
    @DataBaseField(type = "int(11)", fieldname = "pre_value", comment = "前值")
    private int pre_value = 0; // 前值
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型(1:获得,2:消耗)")
    private int type = 0; // 类型(1:获得,2:消耗)
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "int(11)", fieldname = "rechargeType", comment = "类型(1:直充,2:平台)")
    private int rechargeType = 0; // 类型(1:直充,2:平台)
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;
    @DataBaseField(type = "int(5)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "int(11)", fieldname = "reason", comment = "产生原因类型")
    private long reason;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;

    public PlayerClubCardLogFlow() {
    }

    public PlayerClubCardLogFlow(long pid, int num, int cur_remainder, int pre_value, int type, int gameType, int rechargeType, long roomID, long agentsID, int level, long clubID, int reason, int classificationRegionId) {
        this.pid = pid;
        this.num = num;
        this.cur_remainder = cur_remainder;
        this.pre_value = pre_value;
        this.type = type;
        this.gameType = gameType;
        this.rechargeType = rechargeType;
        this.roomID = roomID;
        this.agentsID = agentsID;
        this.level = level;
        this.clubID = clubID;
        this.reason = reason;
        this.classificationRegionId = classificationRegionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerClubCardLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `num`, `cur_remainder`, `pre_value`, `type`,`gameType`,`rechargeType`,`roomID`,`agentsID`,`level`,`clubID`,`reason`,`classificationRegionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerClubCardLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`num` int(11) NOT NULL DEFAULT '0' COMMENT '数量',"
                + "`cur_remainder` int(11) NOT NULL DEFAULT '0' COMMENT '当前剩余',"
                + "`pre_value` int(11) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(1:获得,2:消耗)',"
                + "`gameType` int(11) NOT NULL DEFAULT '-1' COMMENT '游戏类型',"
                + "`rechargeType` int(11) NOT NULL DEFAULT '0' COMMENT '类型(1:直充,2:平台)',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部代理ID',"
                + "`level` int(5) NOT NULL DEFAULT '0' COMMENT '俱乐部代理等级',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`reason` int(11) NOT NULL DEFAULT '0' COMMENT '产生原因类型',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='Club Room Card 圈卡消费日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[16];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = num;
        params[5] = cur_remainder;
        params[6] = pre_value;
        params[7] = type;
        params[8] = gameType;
        params[9] = rechargeType;
        params[10] = roomID;
        params[11] = agentsID;
        params[12] = level;
        params[13] = clubID;
        params[14] = reason;
        params[15] = classificationRegionId;
        return params;
    }
}
