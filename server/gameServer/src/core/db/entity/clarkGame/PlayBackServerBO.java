package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回放码服务器关系记录。
 *
 * @author xushaojun
 */
@TableName(value = "playBackServer")
@Data
@NoArgsConstructor
public class PlayBackServerBO extends BaseEntity<PlayBackServerBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "playBackCode", comment = "回放码")
    private int playBackCode;
    @DataBaseField(type = "varchar(255)", fieldname = "gameServerIP", comment = "服务器IP")
    private String gameServerIP;
    @DataBaseField(type = "int(5)", fieldname = "gameServerPort", comment = "服务器端口")
    private int gameServerPort;
    @DataBaseField(type = "int(1)", fieldname = "weekDay", comment = "周几")
    private int weekDay;
    @DataBaseField(type = "int(8)", fieldname = "dateDay", comment = "日期")
    private int dateDay;

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE `playBackServer` (" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增'," +
                "  `playBackCode` int(11) NOT NULL DEFAULT '0' COMMENT '回放编码'," +
                "  `gameServerIP` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '服务器ip'," +
                "  `gameServerPort` int(5) NOT NULL DEFAULT '0' COMMENT '服务器端口'," +
                "  `weekDay` int(1) NOT NULL COMMENT '周几'," +
                "  `dateDay` int(8) NOT NULL COMMENT '日期'," +
                "  PRIMARY KEY (`id`)," +
                "  KEY `INDEX_PLAYBACKCODE` (`playBackCode`,`weekDay`) USING BTREE COMMENT '回放码索引'," +
                " KEY `INDEX_DATEDAY` (`dateDay`) USING BTREE COMMENT '日期索引'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_croatian_ci COMMENT='回放码服务器映射关系';";
        return sql;
    }


}
