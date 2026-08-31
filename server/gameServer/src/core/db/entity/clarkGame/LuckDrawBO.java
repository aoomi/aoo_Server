package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "luckDraw")
@Data
@NoArgsConstructor
public class LuckDrawBO extends BaseEntity<LuckDrawBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "varchar(50)", fieldname = "prizeName", comment = "奖励名称")
    private String prizeName;
    @DataBaseField(type = "int(11)", fieldname = "prizeType", comment = "奖励类型(1:乐豆,2:现金,3:物品,6:房卡)")
    private int prizeType;
    @DataBaseField(type = "int(11)", fieldname = "rewardNum", comment = "奖励数量")
    private int rewardNum;
    @DataBaseField(type = "int(11)", fieldname = "chance", comment = "机率")
    private int chance;
    @DataBaseField(type = "int(2)", fieldname = "dateType", comment = "选中")
    private int selected;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `luckDraw` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`prizeName` varchar(50) NOT NULL DEFAULT '' COMMENT '奖励名称',"
                + "`prizeType` int(11) NOT NULL DEFAULT '0' COMMENT '奖励类型(1:乐豆,2:现金,3:物品,6:房卡)',"
                + "`rewardNum` int(11) NOT NULL DEFAULT '0' COMMENT '奖励数量',"
                + "`chance` int(11) NOT NULL DEFAULT '0' COMMENT '机率',"
                + "`selected` int(2) NOT NULL DEFAULT '0' COMMENT '选中',"
                + "`url` varchar(255) NOT NULL DEFAULT '' COMMENT '图片地址',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='抽奖奖品列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);

        return sql;
    }
}
