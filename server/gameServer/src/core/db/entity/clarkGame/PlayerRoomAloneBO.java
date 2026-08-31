package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏房间玩家
 *
 * @author Huaxing
 */
@Data
@NoArgsConstructor
@TableName(value = "PlayerRoomAlone")
public class PlayerRoomAloneBO extends BaseEntity<PlayerRoomAloneBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家PID")
    private long pid; // 玩家PID
    @DataBaseField(type = "int(2)", fieldname = "prizeType", comment = "消耗类型")
    private int prizeType;    // 消耗类型
    @DataBaseField(type = "int(5)", fieldname = "value", comment = "消耗值")
    private int value;    // 消耗值
    @DataBaseField(type = "int(2)", fieldname = "paymentType", comment = "支付方式（0：房主付，1：平分支付，2：大赢家付）")
    private int paymentType; // 支付方式（0：房主付，1：平分支付，2：大赢家付）
    @DataBaseField(type = "int(5)", fieldname = "point", comment = "分数")
    private int point; // 分数
    @DataBaseField(type = "int(1)", fieldname = "winner", comment = "1：赢家")
    private int winner; // 1：赢家
    @DataBaseField(type = "int(3)", fieldname = "setCount", comment = "局数")
    private int setCount = 0; //当局局数
    @DataBaseField(type = "int(3)", fieldname = "sumCount", comment = "总局数")
    private int sumCount = 0; //总局数
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "亲友圈Id")
    private long clubID = 0; //俱乐部ID
    @DataBaseField(type = "varchar(10)", fieldname = "roomKey", comment = "房间号")
    private String roomKey = ""; //房间号
    @DataBaseField(type = "int(11)", fieldname = "createRoomTime", comment = "创建时间")
    private int createRoomTime;    //创建时间
    @DataBaseField(type = "int(2)", fieldname = "clubCostType", comment = "消耗方式(2:金币(房卡),3:圈卡)")
    private int clubCostType; //消耗方式
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID = 0; //房间ID
    @DataBaseField(type = "int(3)", fieldname = "gameType", comment = "游戏类型")
    private int gameType; //游戏类型
    @DataBaseField(type = "int(1)", fieldname = "classType", comment = "房间种类(1、麻将，2、扑克)")
    private int classType; //房间种类(1、麻将，2、扑克)
    @DataBaseField(type = "bigint(20)", fieldname = "partnerPid", comment = "伙伴PID")
    private long partnerPid = 0L; //房间ID
    @DataBaseField(type = "int(10)", fieldname = "dateTime", comment = "日志时间(yyyymmdd)")
    private int dateTime; //日志时间(yyyymmdd)
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPoint", comment = "输赢竞技点分数")
    private double sportsPoint;
    @DataBaseField(type = "int(11)", fieldname = "roomTypeValue", comment = "房间类型值")
    private int roomTypeValue;
    @DataBaseField(type = "double(11,2)", fieldname = "roomSportsConsume", comment = "房间竞技点消耗")
    private double roomSportsConsume;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "int(2)", fieldname = "roomSportsType", comment = "竞技点房费类型：0大赢家，1每人")
    private int roomSportsType;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "bigint(20)", fieldname = "upLevelId", comment = "上级推广员id")
    private long upLevelId;
    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "归属id")
    private long memberId;

    public HashMap<String, Object> getUpdateKeyValue() {
        HashMap<String, Object> keyValue = new HashMap<String, Object>();
        keyValue.put("value", value);
        keyValue.put("sportsPoint", sportsPoint);
        keyValue.put("roomSportsConsume", roomSportsConsume);
        keyValue.put("point", point);
        keyValue.put("winner", winner);
        keyValue.put("setCount", setCount);
        keyValue.put("dateTime", dateTime);
        keyValue.put("endTime", endTime);
        if (memberId > 0L) {
            keyValue.put("memberId", memberId);
        }
        if (upLevelId > 0L) {
            keyValue.put("upLevelId", upLevelId);
        }
        return keyValue;
    }

    public void savePoint(int point, int endTime) {
        if (this.point == point) {
            return;
        }
        Map<String, Object> map = com.google.common.collect.Maps.newHashMapWithExpectedSize(2);
        this.point = point;
        map.put("point", point);
        this.endTime = endTime;
        map.put("endTime", endTime);
        this.getBaseService().update(map, id, new AsyncInfo(id));
    }

    public void savePid(long pid) {
        if (this.pid == pid) {
            return;
        }
        this.pid = pid;
        this.getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerRoomAlone` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家PID',"
                + "`prizeType` int(2) NOT NULL DEFAULT '0' COMMENT '消耗类型',"
                + "`value` int(5) NOT NULL DEFAULT '0' COMMENT '消耗值',"
                + "`paymentType` int(2) NOT NULL DEFAULT '0' COMMENT '支付方式（0：房主付，1：平分支付，2：大赢家付）',"
                + "`point` int(5) NOT NULL DEFAULT '0' COMMENT '分数',"
                + "`winner` int(1) NOT NULL DEFAULT '0' COMMENT '1：赢家',"
                + "`setCount` int(3) NOT NULL DEFAULT '0' COMMENT '局数',"
                + "`sumCount` int(3) NOT NULL DEFAULT '0' COMMENT '总局数',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`roomKey` varchar(10) NOT NULL DEFAULT '' COMMENT '房间号',"
                + "`createRoomTime` int(11) NOT NULL DEFAULT '0' COMMENT '房间创建时间',"
                + "`clubCostType` int(11) NOT NULL DEFAULT '0' COMMENT '消耗方式(2:金币(房卡),3:圈卡)',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`gameType` int(3) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`classType` int(1) NOT NULL DEFAULT '0' COMMENT '房间种类(1、麻将，2、扑克)',"
                + "`partnerPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '伙伴PID',"
                + "`dateTime` int(10) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`sportsPoint` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '输赢竞技点分数',"
                + "`roomTypeValue` int(11) NOT NULL DEFAULT '0' COMMENT '房间类型值',"
                + "`roomSportsConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房间竞技点消耗',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`roomSportsType` int(2) NOT NULL DEFAULT '0' COMMENT '竞技点房费类型：0大赢家，1每人',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`upLevelId` bigint(20) NOT NULL DEFAULT '0' COMMENT '上级推广员id',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '归属id',"
                + "PRIMARY KEY (`id`),"
                + "KEY `clubID_dateTime_pid` (`clubID`,`dateTime`,`pid`),"
                + "KEY `clubID_roomID_pid` (`clubID`,`roomID`,`pid`),"
                + "KEY `pid_clubID_partnerPid` (`pid`,`clubID`,`partnerPid`),"
                + "KEY `clubID_dateTime_roomID` (`clubID`,`dateTime`,`roomID`),"
                + "KEY `roomID` (`roomID`)"
                + ") COMMENT='玩家房间单独记录' DEFAULT CHARSET=utf8";
        return sql;
    }

}
