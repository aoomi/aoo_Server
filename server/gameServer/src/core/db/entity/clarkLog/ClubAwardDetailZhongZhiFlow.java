package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**中至
 *颁奖 记录用的数据
 * 详细数据 房间名 大赢家 消耗钻石记录
 * @author Administrator
 */
@TableName(value = "ClubAwardDetailZhongZhi")
@Data
@NoArgsConstructor
public class ClubAwardDetailZhongZhiFlow extends BaseClarkLogEntity<ClubAwardDetailZhongZhiFlow> {
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "int(11)", fieldname = "winner", comment = "大赢家")
    private int winner;
    @DataBaseField(type = "int(11)", fieldname = "consume", comment = "消耗值")
    private int consume;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "联赛Id")
    private long unionId;
    @DataBaseField(type = "int(11)", fieldname = "awardNum", comment = "颁奖次数")
    private int awardNum;
    @DataBaseField(type = "varchar(50)", fieldname = "configName", comment = "房间配置名称")
    private String configName = "";
    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "玩家id")
    private long memberId;
    public ClubAwardDetailZhongZhiFlow(String date_time, int winner, int consume, long clubId, long unionId, int awardNum, String configName,long memberId) {
        this.date_time = date_time;
        this.winner = winner;
        this.consume = consume;
        this.clubId = clubId;
        this.unionId = unionId;
        this.awardNum = awardNum;
        this.configName = configName;
        this.memberId = memberId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubAwardDetailZhongZhi"
                + "(`server_id`, `timestamp`, `date_time`,  `winner`, `consume`, `clubId`,`unionId`,`awardNum`,`configName`,`memberId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,?)";
    }



    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubAwardDetailZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`winner` int(11) NOT NULL DEFAULT '0' COMMENT '大赢家',"
                + "`consume` int(11) NOT NULL DEFAULT '0' COMMENT '消耗值',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '联盟Id',"
                + "`awardNum` int(11) NOT NULL DEFAULT '0' COMMENT '颁奖次数',"
                + "`configName` varchar(50) NOT NULL DEFAULT '' COMMENT '房间配置名称',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家id',"
                + "PRIMARY KEY (`id`),"
                + "KEY `clubId_awardNum` (`clubId`,`awardNum`) USING BTREE,"
                + "KEY `date_time` (`date_time`)"
                + ") COMMENT='颁奖记录(详细数据 房间名 大赢家 消耗钻石记录)' DEFAULT CHARSET=utf8";
        return sql;
    }


    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[10];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = winner;
        params[4] = consume;
        params[5] = clubId;
        params[6] = unionId;
        params[7] = awardNum;
        params[8] = configName;
        params[9] = memberId;
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
