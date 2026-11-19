package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 玩家房间日志
 * 统计开房次数、游戏总局数、平均局数
 *
 * @author Administrator
 */
@TableName(value = "PlayerRoomLog")
@Data
@NoArgsConstructor
public class PlayerRoomLogFlow extends BaseClarkLogEntity<PlayerRoomLogFlow> {
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "bigint(20)", fieldname = "ownner_id", comment = "房主ID")
    private long ownner_id = 0; // 房主ID
    @DataBaseField(type = "bigint(20)", fieldname = "room_id", comment = "房间ID")
    private long room_id = 0; //玩家注册时间
    @DataBaseField(type = "int(11)", fieldname = "set_count", comment = "局数")
    private int set_count = 0; //当局局数
    @DataBaseField(type = "text", fieldname = "player_list", comment = "用户列表JSON")
    private String player_list;//玩家列表
    @DataBaseField(type = "int(11)", fieldname = "sum_count", comment = "总局数")
    private int sum_count = 0; //总局数
    @DataBaseField(type = "bigint(20)", fieldname = "club_id", comment = "俱乐部ID")
    private long club_id = 0; //俱乐部ID
    @DataBaseField(type = "varchar(20)", fieldname = "room_key", comment = "房间号")
    private String room_key = ""; //房间号
    @DataBaseField(type = "int(11)", fieldname = "room_card", comment = "房卡")
    private int room_card = 0; //房卡
    @DataBaseField(type = "int(11)", fieldname = "createRoomTime", comment = "创建时间")
    private int createRoomTime;
    @DataBaseField(type = "int(11)", fieldname = "clubCostType", comment = "消耗方式(2:金币(房卡),3:圈卡)")
    private int clubCostType;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型（0,1,2....4.）")
    private int gameType = -1;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId = 0; //赛事ID

    public PlayerRoomLogFlow(String date_time, long ownner_id, long room_id, int set_count, String player_list, int sum_count, long club_id, String room_key, int room_card, int createRoomTime, int clubCostType, int gameType, int classificationRegionId, long unionId) {
        this.date_time = date_time;
        this.ownner_id = ownner_id;
        this.room_id = room_id;
        this.set_count = set_count;
        this.player_list = StringUtils.isEmpty(player_list) ? "" : player_list;
        this.sum_count = sum_count;
        this.club_id = club_id;
        this.room_key = StringUtils.isEmpty(room_key) ? "" : room_key;
        this.room_card = room_card;
        this.createRoomTime = createRoomTime;
        this.clubCostType = clubCostType;
        this.gameType = gameType;
        this.classificationRegionId = classificationRegionId;
        this.unionId = unionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PlayerRoomLog"
                + "(`server_id`, `timestamp`, `date_time`, `ownner_id`, `room_id`, `set_count`, `player_list`, `sum_count`, `club_id`, `room_key`, `room_card`,`createRoomTime`,`clubCostType`,`gameType`,`classificationRegionId`,`unionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerRoomLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`ownner_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '房主ID',"
                + "`room_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`set_count` int(11) NOT NULL DEFAULT '0' COMMENT '局数',"
                + "`player_list` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '玩家列表JSON' ,"
                + "`sum_count` int(11) NOT NULL DEFAULT '0' COMMENT '总局数',"
                + "`club_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`room_key` varchar(20) NOT NULL DEFAULT '' COMMENT '房间号',"
                + "`room_card` int(11) NOT NULL DEFAULT '0' COMMENT '房卡',"
                + "`createRoomTime` int(11) NOT NULL DEFAULT '0' COMMENT '房间创建时间',"
                + "`clubCostType` int(11) NOT NULL DEFAULT '0' COMMENT '消耗方式(0:金币(房卡),1:圈卡)',"
                + "`gameType` int(11) NOT NULL DEFAULT '-1' COMMENT '游戏类型（0,1,2....4.）',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "PRIMARY KEY (`id`),"
                + "KEY `DT` (`date_time`(20)),"
                + "KEY `club_id` (`club_id`),"
                + "KEY `room_key` (`room_key`)"
                + ") COMMENT='玩家房间日志' DEFAULT CHARSET=utf8";
        return sql;
    }


    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[16];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = ownner_id;
        params[4] = room_id;
        params[5] = set_count;
        params[6] = player_list;
        params[7] = sum_count;
        params[8] = club_id;
        params[9] = room_key;
        params[10] = room_card;
        params[11] = createRoomTime;
        params[12] = clubCostType;
        params[13] = gameType;
        params[14] = classificationRegionId;
        params[15] = unionId;
        return params;
    }
}
