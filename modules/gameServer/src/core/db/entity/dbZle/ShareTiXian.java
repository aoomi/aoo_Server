package core.db.entity.dbZle;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "db_share_tixian")
@Data
@NoArgsConstructor
public class ShareTiXian extends BaseEntity<ShareTiXian> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(10)", fieldname = "player_id", comment = "")
    private int player_id; //截短的玩家id
    @DataBaseField(type = "bigint(64)", fieldname = "money", comment = "")
    private int money; //分
    @DataBaseField(type = "bigint(10)", fieldname = "status", comment = "")
    private int status; //0提现失败 1 提现成功

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE `db_share_tixian` (\n" +
                "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
                "  `player_id` int(10) DEFAULT 0,\n" +
                "  `money` int(64) DEFAULT 0,\n" +
                "  `status` int(10) DEFAULT 0,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=MyISAM AUTO_INCREMENT=503 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC";
        return sql;
    }
}
