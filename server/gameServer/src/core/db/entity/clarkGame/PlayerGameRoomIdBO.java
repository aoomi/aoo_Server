package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerGameRoomIdBOService;
import core.ioc.ContainerMgr;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏房间玩家
 *
 * @author Huaxing
 */
@Data
@NoArgsConstructor
@TableName(value = "PlayerGameRoomId")
public class PlayerGameRoomIdBO extends BaseEntity<PlayerGameRoomIdBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家PID")
    private long pid; // 玩家PID
    @DataBaseField(type = "int(10)", fieldname = "dateTime", comment = "日志时间(yyyymmdd)")
    private int dateTime; //日志时间(yyyymmdd)
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId = 0; //俱乐部ID
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间Id")
    private long roomID = 0; //房间ID

    public PlayerGameRoomIdBO(long pid, int dateTime, long clubId, long roomID) {
        this.pid = pid;
        this.dateTime = dateTime;
        this.clubId = clubId;
        this.roomID = roomID;
    }

    /**
     * 保存
     *
     * @param pid
     * @param endTime
     * @param clubId
     * @param roomID
     */
    public static void saveIgnore(long pid, int endTime, long clubId, long roomID) {
        new PlayerGameRoomIdBO(pid, CommTime.getSecToYMD(endTime), clubId, roomID).saveIgnore();
    }

    public static void delete(long pid, long clubId, long roomID) {
        ((PlayerGameRoomIdBOService) ContainerMgr.get().getComponent(PlayerGameRoomIdBOService.class)).delete(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("clubId", clubId), Restrictions.eq("roomID", roomID)));
    }

    public void saveIgnore() {
        this.getBaseService().saveIgnore(this);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerGameRoomId` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家PID',"
                + "`dateTime` int(10) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间Id',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `pid_clubId_roomID` (`pid`,`clubId`,`roomID`) USING BTREE,"
                + "KEY `dateTime` (`dateTime`)"
                + ") COMMENT='玩家房间战绩Id记录' DEFAULT CHARSET=utf8";
        return sql;
    }

}
