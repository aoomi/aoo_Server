package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import com.google.common.collect.Maps;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

/** One account-wide room-card balance. Gameplay classifications never partition this row. */
@TableName(value = "playerWallet")
@Data
@NoArgsConstructor
public class PlayerWalletBO extends BaseEntity<PlayerWalletBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "primary key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "player id")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "room-card balance")
    private int value;
    @DataBaseField(type = "int(11)", fieldname = "time", comment = "update epoch seconds")
    private int time;

    public PlayerWalletBO(long pid) { this.pid = pid; }

    public void saveValue(int value) {
        if (this.value == value) return;
        HashMap<String, Object> updates = Maps.newHashMapWithExpectedSize(2);
        this.value = value;
        updates.put("value", value);
        this.time = CommTime.nowSecond();
        updates.put("time", time);
        getBaseService().update(updates, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        return "CREATE TABLE IF NOT EXISTS `playerWallet` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT,"
                + "`pid` bigint(20) NOT NULL,"
                + "`value` int(11) NOT NULL DEFAULT '0',"
                + "`time` int(11) NOT NULL DEFAULT '0',"
                + "PRIMARY KEY (`id`), UNIQUE KEY `uk_player_wallet_pid` (`pid`)"
                + ") COMMENT='account-wide room-card wallet' DEFAULT CHARSET=utf8mb4";
    }
}
