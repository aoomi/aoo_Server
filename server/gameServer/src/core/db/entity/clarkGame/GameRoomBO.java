package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;

/*
 * 游戏房间记录
 * */
@Data
@NoArgsConstructor
@TableName(value = "gameRoom")
public class GameRoomBO extends BaseEntity<GameRoomBO> implements Serializable {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "ownner", comment = "房主")
    private long ownner;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "int(11)", fieldname = "setCount", comment = "局数")
    private int setCount;
    @DataBaseField(type = "int(11)", fieldname = "roomType", comment = "房间类型")
    private int roomType;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType = -1;
    @DataBaseField(type = "int(4)", fieldname = "playerNum", comment = "参与人数")
    private int playerNum;
    @DataBaseField(type = "text", fieldname = "dataJsonCfg", comment = "配置数据JSON")
    private String dataJsonCfg = "";
    @DataBaseField(type = "text", fieldname = "dataJsonRes", comment = "结果数据JSON")
    private String dataJsonRes = "";
    @DataBaseField(type = "varchar(25)", fieldname = "roomKey", comment = "房间号")
    private String roomKey = "";
    @DataBaseField(type = "int(4)", fieldname = "type", comment = "房间种类(1、麻将，2、扑克)")
    private int type;
    @DataBaseField(type = "text", fieldname = "playerList", comment = "用户列表JSON")
    private String playerList = "";
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "int(11)", fieldname = "createType", comment = "房间创建类型(1：正常,2：代开)")
    private int createType = 1;
    @DataBaseField(type = "int(10)", fieldname = "dateTime", comment = "日志时间(yyyymmdd)")
    private int dateTime;
    @DataBaseField(type = "int(11)", fieldname = "consumeValue", comment = "消耗值")
    private int consumeValue = 0;
    @DataBaseField(type = "int(2)", fieldname = "valueType", comment = "2:房卡,3:圈卡")
    private int valueType = 0;
    @DataBaseField(type = "int(2)", fieldname = "roomState", comment = "1:游戏中,2:游戏结束")
    private int roomState = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "double(11,2)", fieldname = "roomSportsConsume", comment = "房间竞技点消耗")
    private double roomSportsConsume;
    @DataBaseField(type = "int(2)", fieldname = "roomSportsType", comment = "竞技点房费类型：0大赢家，1每人，2百分比")
    private int roomSportsType;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "房间配置Id")
    private long configId;
    @DataBaseField(type = "varchar(50)", fieldname = "configName", comment = "房间配置名称")
    private String configName = "";

    /**
     * 竞技点倍数
     */
    private Double sportsDouble;

    public void clear() {
        this.dataJsonCfg = null;
        this.dataJsonRes = null;
        this.playerList = null;
        this.roomKey = null;
    }


    public void saveOwnner(long ownner) {
        if (ownner == this.ownner) {
            return;
        }
        this.ownner = ownner;
        getBaseService().update("ownner", ownner, id, new AsyncInfo(id));
    }

    public void saveCreateTime(int createTime) {
        if (createTime == this.createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    public void saveEndTime(int endTime) {
        if (endTime == this.endTime) {
            return;
        }
        this.endTime = endTime;
        getBaseService().update("endTime", endTime, id, new AsyncInfo(id));
    }

    public void saveSetCount(int setCount) {
        if (setCount == this.setCount) {
            return;
        }
        this.setCount = setCount;
        getBaseService().update("setCount", setCount, id, new AsyncInfo(id));
    }

    public void saveRoomType(int roomType) {
        if (roomType == this.roomType) {
            return;
        }
        this.roomType = roomType;
        getBaseService().update("roomType", roomType, id, new AsyncInfo(id));
    }

    public void saveGameType(int gameType) {
        if (gameType == this.gameType) {
            return;
        }
        this.gameType = gameType;
        getBaseService().update("gameType", gameType, id, new AsyncInfo(id));
    }

    public void savePlayerNum(int playerNum) {
        if (playerNum == this.playerNum) {
            return;
        }
        this.playerNum = playerNum;
        getBaseService().update("playerNum", playerNum, id, new AsyncInfo(id));
    }

    public void saveDataJsonCfg(String dataJsonCfg) {
        if (StringUtils.isEmpty(dataJsonCfg)) {
            return;
        }
        if (dataJsonCfg.equals(this.dataJsonCfg)) {
            return;
        }
        this.dataJsonCfg = dataJsonCfg;
        getBaseService().update("dataJsonCfg", dataJsonCfg, id, new AsyncInfo(id));
    }

    public void saveDataJsonRes(String dataJsonRes) {
        if (StringUtils.isEmpty(dataJsonRes)) {
            return;
        }
        if (dataJsonRes.equals(this.dataJsonRes)) {
            return;
        }
        this.dataJsonRes = dataJsonRes;
        getBaseService().update("dataJsonRes", dataJsonRes, id, new AsyncInfo(id));
    }

    public void saveRoomKey(String roomKey) {
        if (StringUtils.isEmpty(roomKey)) {
            return;
        }
        if (roomKey.equals(this.roomKey)) {
            return;
        }
        this.roomKey = roomKey;
        getBaseService().update("roomKey", roomKey, id, new AsyncInfo(id));
    }

    public void savePlayerList(String playerList) {
        if (StringUtils.isEmpty(playerList)) {
            return;
        }
        if (playerList.equals(this.playerList)) {
            return;
        }
        this.playerList = playerList;
        getBaseService().update("playerList", playerList, id, new AsyncInfo(id));
    }

    public void saveCreateType(int createType) {
        if (this.createType == createType) {
            return;
        }
        this.createType = createType;
        getBaseService().update("createType", createType, id, new AsyncInfo(id));
    }


    public HashMap<String, Object> getUpdatePlayerListConsumeValue() {
        HashMap<String, Object> keyValue = new HashMap<String, Object>();
        keyValue.put("playerList", playerList);
        keyValue.put("consumeValue", consumeValue);
        keyValue.put("valueType", valueType);
        keyValue.put("roomState", roomState);
        keyValue.put("endTime", endTime);
        return keyValue;
    }


    public HashMap<String, Object> getUpdateKeyValue() {
        HashMap<String, Object> keyValue = new HashMap<String, Object>();
        keyValue.put("endTime", endTime);
        keyValue.put("playerNum", playerNum);
        keyValue.put("dataJsonRes", dataJsonRes);
        keyValue.put("playerList", playerList);
        keyValue.put("dateTime", dateTime);
        keyValue.put("clubID", clubID);
        keyValue.put("consumeValue", consumeValue);
        keyValue.put("valueType", valueType);
        keyValue.put("roomState", roomState);
        keyValue.put("roomSportsConsume", roomSportsConsume);
        return keyValue;
    }


    public HashMap<String, Object> getUpdateSet(int endTime, String playerList, int consumeValue) {
        HashMap<String, Object> keyValue = new HashMap<String, Object>();
        keyValue.put("endTime", endTime);
        this.endTime = endTime;
        // 有数据
        if (StringUtils.isNotEmpty(playerList)) {
            // 检查是否一样数据
            if (!playerList.equals(this.playerList)) {
                // 数据不一样更新
                keyValue.put("playerList", playerList);
                this.playerList = playerList;
            }
        }

        if (consumeValue != this.consumeValue) {
            // 消耗值有改变
            keyValue.put("consumeValue", consumeValue);
            this.consumeValue = consumeValue;
        }
        return keyValue;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `gameRoom` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`ownner` bigint(20) NOT NULL DEFAULT '0' COMMENT '房主',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`setCount` int(11) NOT NULL DEFAULT '0' COMMENT '局数',"
                + "`roomType` int(11) NOT NULL DEFAULT '0' COMMENT '房间类型',"
                + "`gameType` int(11) NOT NULL DEFAULT '-1' COMMENT '游戏类型',"
                + "`playerNum` int(4) NOT NULL DEFAULT '0' COMMENT '参与人数',"
                + "`dataJsonCfg`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '配置数据JSON' ,"
                + "`dataJsonRes`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '结果数据JSON' ,"
                + "`roomKey` varchar(25) NOT NULL DEFAULT '' COMMENT '房间号',"
                + "`type` int(4) NOT NULL DEFAULT '0' COMMENT '房间种类(1、麻将，2、扑克)',"
                + "`playerList`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '玩家列表JSON' ,"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`createType` int(11) NOT NULL DEFAULT '1' COMMENT '房间创建类型(1：正常,2：代开)',"
                + "`dateTime` int(10) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`consumeValue` int(11) NOT NULL DEFAULT '0' COMMENT '消耗值',"
                + "`valueType` int(2) NOT NULL DEFAULT '0' COMMENT '2:房卡,3:圈卡',"
                + "`roomState` int(2) NOT NULL DEFAULT '0' COMMENT '1:游戏中,2:游戏结束',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`roomSportsConsume` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '房间竞技点消耗总消耗',"
                + "`roomSportsType` int(2) NOT NULL DEFAULT '0' COMMENT '竞技点房费类型：0大赢家，1每人',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间配置Id',"
                + "`configName` varchar(50) NOT NULL DEFAULT '' COMMENT '房间配置名称',"
                + "PRIMARY KEY (`id`) ,"
                + "KEY `dateTime_roomState_endTime` (`dateTime`,`roomState`,`endTime`),"
                + "KEY `clubID_dateTime` (`clubID`,`dateTime`),"
                + "KEY `clubID_gameType_endTime` (`clubID`,`gameType`,`endTime`)"
                + ") COMMENT='游戏房间'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
