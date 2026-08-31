package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@TableName(value = "UnionSportsPointProfitLog")
@Data
public class UnionSportsPointProfitLogFlow extends BaseClarkLogEntity<UnionSportsPointProfitLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "double(11,2)", fieldname = "scorePoint", comment = "分数-收益总分数")
    private double scorePoint;
    @DataBaseField(type = "int(2)", fieldname = "sourceType", comment = "来源类型(1:房费)")
    private int sourceType;
    @DataBaseField(type = "varchar(15)", fieldname = "roomName", comment = "房间名称")
    private String roomName;
    @DataBaseField(type = "int(8)", fieldname = "roomKey", comment = "房间key")
    private int roomKey;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间Id")
    private long roomId;

    public UnionSportsPointProfitLogFlow() {
    }

    public UnionSportsPointProfitLogFlow(long unionId, long clubId, double scorePoint, int sourceType, String roomName, int roomKey, long roomId) {
        this.unionId = unionId;
        this.clubId = clubId;
        this.scorePoint = scorePoint;
        this.sourceType = sourceType;
        this.roomName = StringUtils.isEmpty(roomName) ? "" : roomName;
        this.roomKey = roomKey;
        this.roomId = roomId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO UnionSportsPointProfitLog"
                + "(`server_id`, `timestamp`, `date_time`, `unionId`, `clubId`, `scorePoint`, `sourceType`, `roomName`, `roomKey`, `roomId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `UnionSportsPointProfitLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`scorePoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分数-收益总分数',"
                + "`sourceType` int(2) NOT NULL DEFAULT '0' COMMENT '来源类型(1:房费)',"
                + "`roomName` varchar(15) NOT NULL DEFAULT '' COMMENT '房间名称',"
                + "`roomKey` int(8) NOT NULL DEFAULT '0' COMMENT '房间key',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='赛事竞技点收益' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[10];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = unionId;
        params[4] = clubId;
        params[5] = scorePoint;
        params[6] = sourceType;
        params[7] = roomName;
        params[8] = roomKey;
        params[9] = roomId;
        return params;
    }
}
