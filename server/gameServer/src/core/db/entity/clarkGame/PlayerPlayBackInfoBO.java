package core.db.entity.clarkGame;


import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;

/**
 * 玩家游戏记录回放
 *
 * @author Huaxing
 */
@TableName(value = "PlayerPlayBack")
@Data
public class PlayerPlayBackInfoBO extends BaseEntity<PlayerPlayBackInfoBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "text", fieldname = "playerList", comment = "用户列表JSON")
    private String playerList = "";
    @DataBaseField(type = "int(4)", fieldname = "gameType", comment = "游戏类型")
    private int gameType = -1;

    public void clean() {
        this.playerList = null;
    }


    public void saveRoomID(long roomID) {
        if (roomID == this.roomID) {
            return;
        }
        this.roomID = roomID;
        getBaseService().update("roomID", roomID, id, new AsyncInfo(id));
    }

    public void saveGameType(int gameType) {
        if (this.gameType == gameType) {
            return;
        }
        this.gameType = gameType;
        getBaseService().update("gameType", gameType, id, new AsyncInfo(id));
    }
}
