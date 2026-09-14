package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@TableName(value = "gameSet")
public class GameSetBO extends BaseEntity<GameSetBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "int(11)", fieldname = "setID", comment = "局数ID")
    private int setID;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "text", fieldname = "dataJsonRes", comment = "结果数据JSON")
    private String dataJsonRes = "";
    @DataBaseField(type = "int(11)", fieldname = "playBackCode", comment = "回放编码")
    private int playBackCode = 0;
    @DataBaseField(type = "int(11)", fieldname = "tabId", comment = "标识Id(房间内的自增唯一标识)")
    private int tabId;

    public void saveRoomID(long roomID) {
        if (roomID == this.roomID) {
            return;
        }
        this.roomID = roomID;
        getBaseService().update("roomID", roomID, id, new AsyncInfo(id));
    }

    public void saveSetID(int setID) {
        if (setID == this.setID) {
            return;
        }
        this.setID = setID;
        getBaseService().update("setID", setID, id, new AsyncInfo(id));
    }

    public void saveEndTime(int endTime) {
        if (endTime == this.endTime) {
            return;
        }
        this.endTime = endTime;
        getBaseService().update("endTime", endTime, id, new AsyncInfo(id));
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

    public void savePlayBackCode(int playBackCode) {
        if (this.playBackCode == playBackCode) {
            return;
        }
        this.playBackCode = playBackCode;
        getBaseService().update("playBackCode", playBackCode, id, new AsyncInfo(id));
    }

    public void setDataJsonRes(String dataJsonRes) {
        if (StringUtils.isEmpty(dataJsonRes)) {
            return;
        }
        if (dataJsonRes.equals(this.dataJsonRes)) {
            return;
        }
        this.dataJsonRes = dataJsonRes;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `gameSet` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`setID` int(11) NOT NULL DEFAULT '0' COMMENT '局数ID',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`dataJsonRes`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '结果数据JSON' ,"
                + "`playBackCode` int(11) NOT NULL DEFAULT '0' COMMENT '回放编码',"
                + "`tabId` int(11) NOT NULL DEFAULT '0' COMMENT '标识Id(房间内的自增唯一标识)',"
                + "PRIMARY KEY (`id`),"
                + "KEY `roomID` (`roomID`)"
                + ") COMMENT='一局游戏'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
