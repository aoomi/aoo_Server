package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.google.common.collect.Maps;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 大赛事房间配置数据表
 *
 * @author Huaxing
 */
@TableName(value = "bigUnionRoomConfig")
@Data
@NoArgsConstructor
public class UnionRoomConfigBO extends BaseEntity<UnionRoomConfigBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "text", fieldname = "gameConfig", comment = "游戏配置")
    private String gameConfig = "";
    @DataBaseField(type = "int(2)", fieldname = "status", comment = "状态(-1空,0正常,1禁用,2结算)")
    private int status;
    @DataBaseField(type = "int(5)", fieldname = "gameId", comment = "游戏Id")
    private int gameId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "sortId", comment = "排序Id")
    private int sortId;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;


    public void saveStatus(int status) {
        if (this.status == status) {
            return;
        }
        this.status = status;
        getBaseService().update("status", status, id, new AsyncInfo(id));
    }

    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    /**
     * 保存游戏配置
     *
     * @param gameConfig 配置
     */
    public void savaGameConfig(String gameConfig, int gameId, int updateTime) {
        Map<String, Object> updateMap = Maps.newHashMap();
        if (StringUtils.isEmpty(gameConfig) || gameConfig.equals(this.gameConfig)) {
            return;
        }
        this.gameConfig = gameConfig;
        this.gameId = gameId;
        updateMap.put("gameConfig", gameConfig);
        updateMap.put("gameId", gameId);
        updateMap.put("updateTime", updateTime);
        getBaseService().update(updateMap, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `bigUnionRoomConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`gameConfig` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '游戏配置',"
                + "`status` int(2) NOT NULL DEFAULT '-1' COMMENT '状态(-1空,0正常,1禁用,2结算)',"
                + "`gameId` int(5) NOT NULL DEFAULT '-1' COMMENT '游戏Id',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`sortId` int(11) NOT NULL DEFAULT '0' COMMENT '排序Id',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId` (`unionId`)"
                + ") COMMENT='大赛事房间配置数据表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    public boolean savaConfig() {
        HashMap<String, Object> map = new HashMap<>(3);
        map.put("status", this.getStatus());
        map.put("gameConfig", this.getGameConfig());
        map.put("gameId", getGameId());
        map.put("updateTime", getUpdateTime());
        return getBaseService().update(map, this.getId()) > 0;
    }


}
