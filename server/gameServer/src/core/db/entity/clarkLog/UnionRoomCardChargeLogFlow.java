package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "UnionRoomCardChargeLog")
@Data
public class UnionRoomCardChargeLogFlow extends BaseClarkLogEntity<UnionRoomCardChargeLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "int(11)", fieldname = "status", comment = "操作类型 0为默认状态,1为正常，2禁用3解散，4启用，5修改，6服务器重启，7后台 ,8游戏回退,9玩家转入转出,10赛事关闭,11记录消耗错误先回退后消耗")
    private int status = 0; // VIP等级
    @DataBaseField(type = "int(11)", fieldname = "reason", comment = "产生原因类型")
    private int reason = 0; // 产生原因类型
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
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "赛事代理ID")
    private long agentsID;//工会ID
    @DataBaseField(type = "int(11)", fieldname = "level", comment = "赛事代理等级")
    private int level;//代理等级
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;

    public UnionRoomCardChargeLogFlow() {
    }


    public UnionRoomCardChargeLogFlow(long pid, long unionId, int status, int reason, int num, int cur_remainder, int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {
        this.pid = pid;
        this.status = status;
        this.reason = reason;
        this.num = num;
        this.cur_remainder = cur_remainder;
        this.pre_value = pre_value;
        this.type = type;
        this.unionId = unionId;
        this.gameType = gameType;
        this.rechargeType = rechargeType;
        this.agentsID = agentsID;
        this.level = level;
        this.classificationRegionId = classificationRegionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO UnionRoomCardChargeLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `unionId`, `status`, `reason`, `num`, `cur_remainder`, `pre_value`, `type`,`gameType`,`rechargeType`,`agentsID`,`level`,`classificationRegionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `UnionRoomCardChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid 当status为9玩家转入转出,10赛事关闭 才有赋值 此值参考 ',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`status` int(11) NOT NULL DEFAULT '0' COMMENT '操作类型 0为默认状态,1为正常，2禁用3解散，4启用，5修改，6服务器重启，7后台 ,8游戏回退,9玩家转入转出,10赛事关闭,11记录消耗错误先回退后消耗',"
                + "`reason` int(11) NOT NULL DEFAULT '0' COMMENT '产生原因类型',"
                + "`num` int(11) NOT NULL DEFAULT '0' COMMENT '数量',"
                + "`cur_remainder` int(11) NOT NULL DEFAULT '0' COMMENT '当前剩余',"
                + "`pre_value` int(11) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(2:消耗,3回退 9赛事转出到玩家 10玩家房卡转入赛事 )',"
                + "`gameType` int(11) NOT NULL DEFAULT '-1' COMMENT '游戏类型',"
                + "`rechargeType` int(11) NOT NULL DEFAULT '0' COMMENT '类型(8:赛事)',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0'  COMMENT '赛事代理ID',"
                + "`level` int(11) NOT NULL DEFAULT '0'  COMMENT '赛事代理等级',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "PRIMARY KEY (`id`),"
                + "KEY `PRD` (`pid`,`rechargeType`,`date_time`(20)) USING BTREE,"
                + "KEY `DTR` (`date_time`(20),`type`,`rechargeType`) USING BTREE"
                + ") COMMENT='赛事 Room Card 房卡消费日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[16];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = unionId;
        params[5] = status;
        params[6] = reason;
        params[7] = num;
        params[8] = cur_remainder;
        params[9] = pre_value;
        params[10] = type;
        params[11] = gameType;
        params[12] = rechargeType;
        params[13] = agentsID;
        params[14] = level;
        params[15] = classificationRegionId;
        return params;
    }
}
