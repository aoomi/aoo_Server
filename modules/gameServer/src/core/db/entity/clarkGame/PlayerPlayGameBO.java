package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家玩游戏次数列表
 *
 * @author Huaxing
 */
@TableName(value = "playerPlayGame")
@Data
@NoArgsConstructor
public class PlayerPlayGameBO extends BaseEntity<PlayerPlayGameBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "*所属玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "int(11)", fieldname = "count", comment = "次数")
    private int count;

    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, new AsyncInfo(id));
    }

    public void saveGameType(int gameType) {
        if (this.gameType == gameType) {
            return;
        }
        this.gameType = gameType;
        getBaseService().update("gameType", gameType, new AsyncInfo(id));
    }

    public void saveCount(int count) {
        if (this.count == count) {
            return;
        }
        this.count = count;
        getBaseService().update("count", count, new AsyncInfo(id));
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerPlayGame` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`gameType` int(11) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`count` int(11) NOT NULL DEFAULT '0' COMMENT '次数',"
                + "PRIMARY KEY (`id`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='玩家玩游戏次数列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

}
