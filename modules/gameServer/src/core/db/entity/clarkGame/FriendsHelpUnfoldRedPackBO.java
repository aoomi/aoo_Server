package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友帮拆红包
 *
 * @author Huaxing
 */
@TableName(value = "friendsHelpUnfoldRedPack")
@Data
@NoArgsConstructor
public class FriendsHelpUnfoldRedPackBO extends BaseEntity<FriendsHelpUnfoldRedPackBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "taskType", comment = "任务类型")
    private int taskType;
    @DataBaseField(type = "varchar(255)", fieldname = "title", comment = "标题")
    private String title;
    @DataBaseField(type = "varchar(255)", fieldname = "content", comment = "内容")
    private String content;
    @DataBaseField(type = "int(11)", fieldname = "targetValue", comment = "目标值")
    private int targetValue;
    @DataBaseField(type = "int(11)", fieldname = "targetType", comment = "目标类型")
    private int targetType;
    @DataBaseField(type = "bigint(20)", fieldname = "preTaskId", comment = "前置任务")
    private long preTaskId;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "红包(单位：分)")
    private int value;
    @DataBaseField(type = "int(2)", fieldname = "pondType", comment = "红包池类型")
    private int pondType;


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `friendsHelpUnfoldRedPack` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`taskType` int(11) NOT NULL DEFAULT '0' COMMENT '任务类型',"
                + "`title` varchar(255) NOT NULL DEFAULT '' COMMENT '标题',"
                + "`content` varchar(255) NOT NULL DEFAULT '' COMMENT '内容',"
                + "`targetValue` int(11) NOT NULL DEFAULT '0' COMMENT '目标值',"
                + "`targetType` int(11) NOT NULL DEFAULT '0' COMMENT '目标类型',"
                + "`preTaskId` bigint(20) NOT NULL DEFAULT '0' COMMENT '前置任务',"
                + "`value` int(11) NOT NULL DEFAULT '0' COMMENT '红包(单位：分)',"
                + "`pondType` int(2) NOT NULL DEFAULT '0' COMMENT '红包池类型',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='好友帮拆红包'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }

    @Override
    public String toString() {
        return "FriendsHelpUnfoldRedPackBO [id=" + id + ", taskType=" + taskType + ", title=" + title + ", content="
                + content + ", targetValue=" + targetValue + ", targetType=" + targetType + ", preTaskId=" + preTaskId
                + ", value=" + value + ", pondType=" + pondType + "]";
    }


}
