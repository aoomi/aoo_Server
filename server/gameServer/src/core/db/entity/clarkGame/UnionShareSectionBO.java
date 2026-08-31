package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联盟区间分成表
 */
@TableName(value = "UnionShareSection")
@Data
@NoArgsConstructor
public class UnionShareSectionBO extends BaseEntity<UnionShareSectionBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "double(11,2)", fieldname = "beginValue", comment = "起始值")
    private double beginValue;
    /**
     * 结束区间为闭合状态
     */
    @DataBaseField(type = "double(11,2)", fieldname = "endValue", comment = "结束值")
    private double endValue;
    @DataBaseField(type = "int(2)", fieldname = "endFlag", comment = "结束标志")
    private int endFlag;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `UnionShareSection` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0'  COMMENT '更新时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`beginValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '起始值',"
                + "`endValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '结束值',"
                + "`endFlag` bigint(2) NOT NULL DEFAULT '0' COMMENT '结束标志',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionId_id` (`unionId`,`id`) USING BTREE"
                + ") COMMENT='联盟区间分成表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


}
