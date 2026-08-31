package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.service.clarkGame.RoomConfigCalcActiveBOService;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "roomConfigCalcActive")
@Data
@NoArgsConstructor
public class RoomConfigCalcActiveBO extends BaseEntity<RoomConfigCalcActiveBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "玩家Pid")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "configId", comment = "房间配置Id")
    private long configId;
    @DataBaseField(type = "int(5)", fieldname = "scorePercent", comment = "积分比例")
    private int scorePercent;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "double(11,2)", fieldname = "scoreDividedInto", comment = "分数分成值")
    private double scoreDividedInto;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Id")
    private long pid;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `roomConfigCalcActive` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`configId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间配置Id',"
                + "`scorePercent` int(5) NOT NULL DEFAULT '0' COMMENT '积分比例',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0'  COMMENT '更新时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`scoreDividedInto` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '分数分成值',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Id',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `pid_unionId_configId_clubId` (`pid`,`unionId`,`configId`,`clubId`) USING BTREE"
                + ") COMMENT='推广员积分比例修改配置表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    /**
     * 增加或者保存
     */
    public void saveIgnoreOrUpDate() {
        ((RoomConfigCalcActiveBOService) this.getBaseService()).saveIgnoreOrUpDate(this);
    }

}
