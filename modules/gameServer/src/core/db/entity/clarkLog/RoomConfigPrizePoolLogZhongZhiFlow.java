package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖金池与玩法经营
 *
 * @author Administrator
 */
@TableName(value = "roomConfigPrizePoolLogZhongZhi")
@Data
@NoArgsConstructor
public class RoomConfigPrizePoolLogZhongZhiFlow extends BaseClarkLogEntity<RoomConfigPrizePoolLogZhongZhiFlow> {
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "配置Id")
    private long configId = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间Id")
    private long roomId = 0;
    @DataBaseField(type = "int(11)", fieldname = "setCount", comment = "对局数(实际)")
    private int setCount = 0;
    @DataBaseField(type = "int(11)", fieldname = "roomSize", comment = "开房数", defaultValue = "1")
    private int roomSize = 1;
    @DataBaseField(type = "int(11)", fieldname = "consumeValue", comment = "消耗值")
    private int consumeValue = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId = 0;
    @DataBaseField(type = "varchar(150)", fieldname = "roomName", comment = "房间名称")
    private String roomName;
    @DataBaseField(type = "text", fieldname = "dataJsonCfg", comment = "配置数据JSON")
    private String dataJsonCfg = "";
    @DataBaseField(type = "double(11,2)", fieldname = "prizePool", comment = "奖金池")
    private double prizePool;
    @DataBaseField(type = "int(11)", fieldname = "gameId", comment = "游戏Id")
    private int gameId = 0;
    @DataBaseField(type = "double(11,2)", fieldname = "sportPointIncome", comment = "本次推广员分成盟主所得比赛分")
    private double sportPointIncome;
    @DataBaseField(type = "double(11,2)", fieldname = "roomSportsPointConsume", comment = "房费消耗")
    private double roomSportsPointConsume;

    public RoomConfigPrizePoolLogZhongZhiFlow(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
        this.date_time = date_time;
        this.configId = configId;
        this.roomId = roomId;
        this.setCount = setCount;
        this.roomSize = roomSize;
        this.consumeValue = value;
        this.unionId = unionId;
        this.roomName = roomName;
        this.dataJsonCfg = dataJsonCfg;
        this.prizePool = prizePool;
        this.gameId = gameId;
        this.sportPointIncome = sportPointIncome;
        this.roomSportsPointConsume = roomSportsPointConsume;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO roomConfigPrizePoolLogZhongZhi"
                + "(`server_id`, `timestamp`, `date_time`, `configId`, `roomId`, `setCount`, `roomSize`, `consumeValue`, `unionId`, `roomName`, `dataJsonCfg`,`prizePool`,`gameId`,`sportPointIncome`,`roomSportsPointConsume`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `roomConfigPrizePoolLogZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '配置Id',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "`setCount` int(11) NOT NULL DEFAULT '0' COMMENT '对局数(实际)',"
                + "`roomSize` int(11) NOT NULL DEFAULT '1' COMMENT '开房数',"
                + "`consumeValue` int(11) NOT NULL DEFAULT '0' COMMENT '消耗值',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`roomName` varchar(150) NOT NULL DEFAULT '' COMMENT '房间名称',"
                + "`dataJsonCfg`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '奖金池' ,"
                + "`prizePool` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房间竞技点消耗总消耗',"
                + "`gameId` int(11) NOT NULL DEFAULT '0' COMMENT '游戏Id',"
                + "`sportPointIncome` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '本次推广员分成盟主所得比赛分',"
                + "`roomSportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房费消耗',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId_date_time_roomName` (`unionId`,`date_time`,`roomName`),"
                + " KEY `timestamp` (`timestamp`),"
                + " KEY `configId_roomId` (`configId`,`roomId`)"
                + ") COMMENT='奖金池与玩法经营' DEFAULT CHARSET=utf8";
        return sql;
    }


    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[15];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = configId;
        params[4] = roomId;
        params[5] = setCount;
        params[6] = roomSize;
        params[7] = consumeValue;
        params[8] = unionId;
        params[9] = roomName;
        params[10] = dataJsonCfg;
        params[11] = prizePool;
        params[12] = gameId;
        params[13] = sportPointIncome;
        params[14] = roomSportsPointConsume;
        return params;
    }
}
