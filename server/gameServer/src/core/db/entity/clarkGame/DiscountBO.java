package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@TableName(value = "discount")
@Data
@NoArgsConstructor
public class DiscountBO extends BaseEntity<DiscountBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(2)", fieldname = "crowdType", comment = "人群类型")
    private int crowdType;
    @DataBaseField(type = "varchar(500)", fieldname = "crowdList", comment = "人群列表")
    private String crowdList;
    private List<Long> crowdLists;
    @DataBaseField(type = "varchar(500)", fieldname = "gameList", comment = "游戏列表")
    private String gameList;
    private List<Integer> gameLists;
    @DataBaseField(type = "int(3)", fieldname = "value", comment = "活动值")
    private int value;
    @DataBaseField(type = "int(2)", fieldname = "dateType", comment = "日期类型")
    private int dateType;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始日期")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束日期")
    private int endTime;
    @DataBaseField(type = "int(2)", fieldname = "state", comment = "0:开启,1:关闭")
    private int state;

    public void setCrowdList(String crowdList) {
        this.crowdList = crowdList;
        this.setCrowdLists(StringUtils.isEmpty(crowdList) ? Collections.emptyList() : Arrays.stream(crowdList.split(",")).filter(k -> StringUtils.isNumeric(k)).map(k -> Long.parseLong(k.trim())).collect(Collectors.toList()));
    }

    public void setGameList(String gameList) {
        this.gameList = gameList;
        this.setGameLists(StringUtils.isEmpty(gameList) ? Collections.emptyList() : Arrays.stream(gameList.split(",")).filter(k -> StringUtils.isNumeric(k)).map(k -> Integer.parseInt(k.trim())).collect(Collectors.toList()));
    }

    /**
     * 检查指定代理是否存在
     *
     * @param id 代理ID
     * @return
     */
    public boolean crowdContains(long id) {
        if (id <= 0L) {
            return false;
        }
        return this.crowdLists.contains(id);
    }

    /**
     * 检查指定游戏是否存在
     *
     * @param gameType 指定游戏
     * @return
     */
    public boolean gameContains(int gameType) {
        return CollectionUtils.isEmpty(this.gameLists) ? true : this.gameLists.contains(gameType);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `discount` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`crowdType` int(2) NOT NULL DEFAULT '0' COMMENT '人群类型',"
                + "`crowdList` varchar(500) NOT NULL DEFAULT ''  COMMENT '人群列表',"
                + "`gameList` varchar(500) NOT NULL DEFAULT ''  COMMENT '游戏列表',"
                + "`value` int(3) NOT NULL DEFAULT '0' COMMENT '活动值',"
                + "`dateType` int(2) NOT NULL DEFAULT '0' COMMENT '日期类型',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始日期',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束日期',"
                + "`state` int(2) NOT NULL DEFAULT '0' COMMENT '0:开启,1:关闭'," + "PRIMARY KEY (`id`)"
                + ") COMMENT='打折（免费）游戏活动'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
