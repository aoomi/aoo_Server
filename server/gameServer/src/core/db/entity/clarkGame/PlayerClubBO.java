package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "playerClub")
@Data
@NoArgsConstructor
public class PlayerClubBO extends BaseEntity<PlayerClubBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;
    @DataBaseField(type = "int(5)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;
    @DataBaseField(type = "int(11)", fieldname = "clubRoomCard", comment = "圈卡")
    private int clubRoomCard;

    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }

    public void saveAgentsID(long agentsID) {
        if (agentsID == this.agentsID) {
            return;
        }
        this.agentsID = agentsID;
        getBaseService().update("agentsID", agentsID, id, new AsyncInfo(id));
    }

    public void saveLevel(int level) {
        if (this.level == level) {
            return;
        }
        this.level = level;
        getBaseService().update("level", level, id, new AsyncInfo(id));
    }

    public void saveClubRoomCard(int clubRoomCard) {
        if (this.clubRoomCard == clubRoomCard) {
            return;
        }
        this.clubRoomCard = clubRoomCard;
        getBaseService().update("clubRoomCard", clubRoomCard, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerClub` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部代理ID',"
                + "`level` int(5) NOT NULL DEFAULT '0' COMMENT '俱乐部代理等级',"
                + "`clubRoomCard` int(11) NOT NULL DEFAULT '0' COMMENT '圈卡',"
                + "PRIMARY KEY (`id`),"
                + "KEY `PID` (`pid`)"
                + ") COMMENT='玩家圈卡'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
