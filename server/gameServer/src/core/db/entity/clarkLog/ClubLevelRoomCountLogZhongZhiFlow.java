package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "ClubLevelRoomCountLogZhongZhi")
@Data
@NoArgsConstructor
public class ClubLevelRoomCountLogZhongZhiFlow extends BaseClarkLogEntity<ClubLevelRoomCountLogZhongZhiFlow> {
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "int(11)", fieldname = "setCount", comment = "局数")
    private int setCount;
    @DataBaseField(type = "int(11)", fieldname = "winner", comment = "大赢家")
    private int winner;
    @DataBaseField(type = "int(11)", fieldname = "consume", comment = "消耗值")
    private int consume;
    @DataBaseField(type = "int(11)", fieldname = "roomSize", comment = "开桌数")
    private long roomSize;
    @DataBaseField(type = "bigint(20)", fieldname = "upLevelId", comment = "上级推广员id")
    private long upLevelId;
    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "归属id")
    private long memberId;
    @DataBaseField(type = "bigint(20)", fieldname = "sumPoint", comment = "总得分")
    private long sumPoint;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPoint", comment = "比赛分")
    private double sportsPoint = 0D;
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
    @DataBaseField(type = "double(11,2)", fieldname = "promotionShareValue", comment = "推广员战绩分成")
    private double promotionShareValue;
    @DataBaseField(type = "int(11)", fieldname = "sizePlayer", comment = "人数")
    private int sizePlayer;
    @DataBaseField(type = "varchar(20)", fieldname = "shareValue", comment = "代理分成值")
    private String shareValue = "";
    @DataBaseField(type = "double(11,2)", fieldname = "scorePoint", comment = "活跃度")
    private double scorePoint = 0D;
    @DataBaseField(type = "double(11,2)", fieldname = "personalSportsPoint", comment = "个人比赛分")
    private double personalSportsPoint = 0D;

    public ClubLevelRoomCountLogZhongZhiFlow(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
        this.date_time = date_time;
        this.setCount = setCount;
        this.winner = winner;
        this.consume = consume;
        this.roomSize = roomSize;
        this.upLevelId = upLevelId;
        this.memberId = memberId;
        this.sumPoint = sumPoint;
        this.sportsPoint = sportsPoint;
        this.sportsPointConsume = sportsPointConsume;
        this.roomSportsPointConsume = roomSportsPointConsume;
        this.roomAvgSportsPointConsume = roomAvgSportsPointConsume;
        this.clubId = clubId;
        this.unionId = unionId;
        this.promotionShareValue = promotionShareValue;
        this.sizePlayer = sizePlayer;
        this.shareValue = shareValue;
        this.scorePoint = scorePoint;
        this.personalSportsPoint = personalSportsPoint;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubLevelRoomCountLogZhongZhi"
                + "(`server_id`, `timestamp`, `date_time`, `setCount`, `winner`, `consume`, `roomSize`, `upLevelId`, `memberId`, `sumPoint`, `sportsPoint`, `sportsPointConsume`, `roomSportsPointConsume`, `roomAvgSportsPointConsume`, `clubId`,`unionId`,`promotionShareValue`,`sizePlayer`,`shareValue`,`scorePoint`,`personalSportsPoint`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,? ,? ,? ,?,?,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubLevelRoomCountLogZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`setCount` int(11) NOT NULL DEFAULT '0' COMMENT '局数',"
                + "`winner` int(11) NOT NULL DEFAULT '0' COMMENT '大赢家',"
                + "`consume` int(11) NOT NULL DEFAULT '0' COMMENT '消耗值',"
                + "`roomSize` int(11) NOT NULL DEFAULT '0' COMMENT '开桌数',"
                + "`upLevelId` bigint(20) NOT NULL DEFAULT '0' COMMENT '上级推广员id',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '归属id',"
                + "`sumPoint` bigint(20) NOT NULL DEFAULT '0' COMMENT '总得分',"
                + "`sportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '比赛分',"
                + "`sportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '消耗比赛分',"
                + "`roomSportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房费比赛分',"
                + "`roomAvgSportsPointConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房费比赛分均值',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '联盟Id',"
                + "`promotionShareValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '推广员战绩分成',"
                + "`sizePlayer` int(11) NOT NULL DEFAULT '0' COMMENT '人数',"
                + "`shareValue` varchar(20) NOT NULL DEFAULT '' COMMENT '代理分成值',"
                + "`scorePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '活跃度',"
                + "`personalSportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '个人比赛分',"
                + "PRIMARY KEY (`id`),"
                + "KEY `date_time, memberId` (`date_time`,`memberId`)"
                + ") COMMENT='玩家推广员及下属消耗中至特殊表' DEFAULT CHARSET=utf8";
        return sql;
    }


    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[21];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = setCount;
        params[4] = winner;
        params[5] = consume;
        params[6] = roomSize;
        params[7] = upLevelId;
        params[8] = memberId;
        params[9] = sumPoint;
        params[10] = sportsPoint;
        params[11] = sportsPointConsume;
        params[12] = roomSportsPointConsume;
        params[13] = roomAvgSportsPointConsume;
        params[14] = clubId;
        params[15] = unionId;
        params[16] = promotionShareValue;
        params[17] = sizePlayer;
        params[18] = shareValue;
        params[19] = scorePoint;
        params[20] = personalSportsPoint;
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


}
