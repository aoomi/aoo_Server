package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.service.clarkGame.PromotionLevelRoomConfigScorePercentBOService;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "promotionLevelRoomConfigScorePercent")
@Data
@NoArgsConstructor
public class PromotionLevelRoomConfigScorePercentBO extends BaseEntity<PromotionLevelRoomConfigScorePercentBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "房间配置Id")
    private long configId;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "double(11,2)", fieldname = "scorePercent", comment = "积分比例")
    private double scorePercent;
    @DataBaseField(type = "double(11,2)", fieldname = "scoreDividedInto", comment = "分数分成值")
    private double scoreDividedInto;
    @DataBaseField(type = "int(2)", fieldname = "type", comment = "类型（0：百分比，1：固定值）", defaultValue = "1")
    private int type;
    private double shareValue;
    private String configName;
    private int tagId;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `promotionLevelRoomConfigScorePercent` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间配置Id',"
                + "`scorePercent` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '积分比例',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0'  COMMENT '更新时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`scoreDividedInto` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分数分成值',"
                + "`type` int(2) NOT NULL DEFAULT '1' COMMENT '类型（0：百分比，1：固定值）',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `pid_unionId_configId_clubId` (`pid`,`unionId`,`configId`,`clubId`) USING BTREE"
                + ") COMMENT='推广员等级积分比例修改配置表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    /**
     * 增加或者保存
     */
    public void saveIgnoreOrUpDate(long execPid) {
        ((PromotionLevelRoomConfigScorePercentBOService) this.getBaseService()).saveIgnoreOrUpDate(this, execPid);
    }
}
