package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.server.ServerConfig;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 玩家圈卡真实消耗日志
 *
 * @author Administrator
 */
@TableName(value = "ClubCardWinnerRebateLog")
@Data
public class ClubCardWinnerRebateLogFlow extends BaseClarkLogEntity<ClubCardWinnerRebateLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;
    @DataBaseField(type = "int(5)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "varchar(255)", fieldname = "winnerPid", comment = "大赢家列表")
    private String winnerPid;
    @DataBaseField(type = "int(11)", fieldname = "clubLevel", comment = "代理分成")
    private int clubLevel = 0;
    @DataBaseField(type = "int(11)", fieldname = "agentsCard", comment = "代理设置房卡")
    private int agentsCard = 0;
    @DataBaseField(type = "int(11)", fieldname = "officialCard", comment = "官方设置房卡")
    private int officialCard = 0;
    @DataBaseField(type = "int(11)", fieldname = "actualCard", comment = "实际消耗房卡")
    private int actualCard = 0;
    @DataBaseField(type = "int(11)", fieldname = "money", comment = "计算后的返利金额")
    private int money;
    @DataBaseField(type = "int(11)", fieldname = "paymentRoomCardType", comment = "付费方式")
    private int paymentRoomCardType;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "int(11)", fieldname = "playerNum", comment = "人数")
    private int playerNum;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;

    public ClubCardWinnerRebateLogFlow() {
    }

    public ClubCardWinnerRebateLogFlow(long roomID, long agentsID, int level, long clubID, String winnerPid, int clubLevel, int agentsCard, int officialCard, int actualCard, int money, int paymentRoomCardType, int gameType, int playerNum, int classificationRegionId) {
        this.roomID = roomID;
        this.agentsID = agentsID;
        this.level = level;
        this.clubID = clubID;
        this.winnerPid = StringUtils.isEmpty(winnerPid) ? "" : winnerPid;
        this.clubLevel = clubLevel;
        this.agentsCard = agentsCard;
        this.officialCard = officialCard;
        this.actualCard = actualCard;
        this.money = money;
        this.paymentRoomCardType = paymentRoomCardType;
        this.gameType = gameType;
        this.playerNum = playerNum;
        this.classificationRegionId = classificationRegionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubCardWinnerRebateLog"
                + "(`server_id`, `timestamp`, `date_time`, `roomID`, `agentsID`, `level`, `clubID`, `winnerPid`,`clubLevel`,`agentsCard`,`officialCard`,`actualCard`,`money`,`paymentRoomCardType`,`gameType`,`playerNum`,`classificationRegionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubCardWinnerRebateLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部代理ID',"
                + "`level` int(5) NOT NULL DEFAULT '0' COMMENT '俱乐部代理等级',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`winnerPid` varchar(255) NOT NULL DEFAULT '0' COMMENT '大赢家列表',"
                + "`clubLevel` int(11) NOT NULL DEFAULT '0' COMMENT '代理分成',"
                + "`agentsCard` int(11) NOT NULL DEFAULT '0' COMMENT '代理设置房卡',"
                + "`officialCard` int(11) NOT NULL DEFAULT '0' COMMENT '官方设置房卡',"
                + "`actualCard` int(11) NOT NULL DEFAULT '0' COMMENT '实际消耗房卡',"
                + "`money` int(11) NOT NULL DEFAULT '0' COMMENT '计算后的返利金额',"
                + "`paymentRoomCardType` int(11) NOT NULL DEFAULT '0' COMMENT '付费方式',"
                + "`gameType` int(11) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`playerNum` int(11) NOT NULL DEFAULT '0' COMMENT '人数',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "KEY `D` (`date_time`(20)) USING BTREE,"
                + "KEY `A` (`agentsID`) USING BTREE,"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='圈卡大赢家返利' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[17];
        params[0] = ServerConfig.ServerID();
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = roomID;
        params[4] = agentsID;
        params[5] = level;
        params[6] = clubID;
        params[7] = winnerPid;
        params[8] = clubLevel;
        params[9] = agentsCard;
        params[10] = officialCard;
        params[11] = actualCard;
        params[12] = money;
        params[13] = paymentRoomCardType;
        params[14] = gameType;
        params[15] = playerNum;
        params[16] = classificationRegionId;
        return params;
    }
}
