package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "dictionariesMap")
@Data
@NoArgsConstructor
public class DictionariesMapBO extends BaseEntity<DictionariesMapBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "varchar(255)", fieldname = "key", comment = "key键值")
    private String key = "";
    @DataBaseField(type = "varchar(255)", fieldname = "value", comment = "key键值对应的value")
    private String value = "";
    @DataBaseField(type = "text", fieldname = "des", comment = "key的描述")
    private String des = "";

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `dictionariesMap` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`key` varchar(255) NOT NULL DEFAULT '' COMMENT 'key键值',"
                + "`value` varchar(255) NOT NULL DEFAULT '' COMMENT 'key键值对应的value',"
                + "`des` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'key的描述' ,"
                + "PRIMARY KEY (`id`),"
                + "KEY `key` (`key`)"
                + ") COMMENT='数据字典'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

}
