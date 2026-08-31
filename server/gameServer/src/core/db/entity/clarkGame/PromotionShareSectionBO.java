package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推广员区间分成表
 */
@TableName(value = "PromotionShareSection")
@Data
@NoArgsConstructor
public class PromotionShareSectionBO extends BaseEntity<PromotionShareSectionBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionSectionId", comment = "联盟区间Id")
    private long unionSectionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    /**
     * 默认取区间值的结束值 这样的话 就是全部分配给自己
     */
    @DataBaseField(type = "double(11,2)", fieldname = "shareToSelfValue", comment = "分配给自己的值")
    private double shareToSelfValue;
    @DataBaseField(type = "double(11,2)", fieldname = "beginValue", comment = "起始值")
    private double beginValue;
    /**
     * 结束区间为闭合状态
     */
    @DataBaseField(type = "double(11,2)", fieldname = "endValue", comment = "结束值")
    private double endValue;
    @DataBaseField(type = "int(2)", fieldname = "endFlag", comment = "结束标志")
    private int endFlag;
    @DataBaseField(type = "double(11,2)", fieldname = "allowShareToValue", comment = "可分配的值")
    private double allowShareToValue;


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `PromotionShareSection` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionSectionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '联盟分成区间Id',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0'  COMMENT '更新时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`shareToSelfValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分配给自己的值',"
                + "`allowShareToValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分配给自己的值',"
                + "`beginValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '起始值',"
                + "`endValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '结束值',"
                + "`endFlag` bigint(2) NOT NULL DEFAULT '0' COMMENT '结束标志',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `clubId_pid_unionSectionId_id` (`clubId`,`pid`,`unionSectionId`,`id`) USING BTREE"
                + ") COMMENT='推广员区间分成表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


}
