package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "email")
@Data
@NoArgsConstructor
public class EMailBO extends BaseEntity<EMailBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "createTime", comment = "邮件创建时间")
    private long createTime;
    @DataBaseField(type = "varchar(255)", fieldname = "title", comment = "邮件标题")
    private String title = "";
    @DataBaseField(type = "text", fieldname = "msgInfo", comment = "邮件正文")
    private String msgInfo = "";
    @DataBaseField(type = "varchar(255)", fieldname = "sender", comment = "邮件发送者")
    private String sender = "";
    @DataBaseField(type = "int(8)", fieldname = "status", comment = "邮件状态  0x00 默认状态   0x01已读  0x02 以获取奖励  0x04 已删除")
    private int status;
    @DataBaseField(type = "int(4)", fieldname = "isHaveAnyAttachment", comment = "是否有附件 1有  0没有")
    private int isHaveAnyAttachment;
    @DataBaseField(type = "text", fieldname = "rewardString", comment = "邮件奖励json字符串  [{'prizeType':6,'count':5}]  prizeType: 1-金币  2-(水晶)兑换券 6-房卡 count:数量")
    private String rewardString = "";
    @DataBaseField(type = "bigint(20)", fieldname = "playerID", comment = "邮件属于谁")
    private long playerID;
    @DataBaseField(type = "bigint(20)", fieldname = "deleteTime", comment = "邮件删除时间")
    private long deleteTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `email` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`createTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '邮件创建时间',"
                + "`title` varchar(255) NOT NULL DEFAULT ''  COMMENT '邮件标题',"
                + "`msgInfo` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '邮件正文',"
                + "`sender` varchar(255) NOT NULL DEFAULT '' COMMENT '邮件发送者',"
                + "`status` int(8) NOT NULL DEFAULT '0' COMMENT '邮件状态  0x00 默认状态   0x01已读  0x02 以获取奖励  0x04 已删除',"
                + "`isHaveAnyAttachment` int(4) NOT NULL DEFAULT '0' COMMENT '是否有附件 1有  0没有',"
                + "`rewardString` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '邮件奖励json字符串prizeType: 1-金币  2-(水晶)兑换券 6-房卡 count:数量',"
                + "`playerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '邮件属于谁',"
                + "`deleteTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '邮件删除时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='邮件表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
