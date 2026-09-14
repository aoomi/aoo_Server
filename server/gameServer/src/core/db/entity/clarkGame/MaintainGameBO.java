package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "maintainGame")
@Data
@NoArgsConstructor
public class MaintainGameBO extends BaseEntity<MaintainGameBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "gameTypeId", comment = "游戏ID")
    private int gameTypeId;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始维护时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束维护时间")
    private int endTime;
    @DataBaseField(type = "varchar(255)", fieldname = "title", comment = "标题")
    private String title;
    @DataBaseField(type = "varchar(255)", fieldname = "content", comment = "内容")
    private String content;
    @DataBaseField(type = "varchar(255)", fieldname = "mainTitle", comment = "主标题")
    private String mainTitle;
    @DataBaseField(type = "int(2)", fieldname = "status", comment = "0没有维护,1维护中")
    private int status;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE `maintainGame` (\n" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',\n" +
                "  `startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始维护时间',\n" +
                "  `endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束维护时间',\n" +
                "  `gameTypeId` int(11) NOT NULL DEFAULT '0' COMMENT '游戏ID',\n" +
                "  `title` varchar(255) DEFAULT NULL COMMENT '标题',\n" +
                "  `content` varchar(255) DEFAULT NULL COMMENT '内容',\n" +
                "  `mainTitle` varchar(255) DEFAULT NULL COMMENT '主标题',\n" +
                "  `status` int(2) NOT NULL DEFAULT '0' COMMENT '0没有维护,1维护中',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  KEY `INDEX_GAME_ID` (`gameTypeId`) USING BTREE COMMENT '游戏ID索引'\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1) + " DEFAULT CHARSET=utf8 COMMENT='游戏维护';";
        return sql;
    }
}
