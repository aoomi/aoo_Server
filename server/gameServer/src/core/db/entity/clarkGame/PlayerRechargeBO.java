package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家充值
 *
 * @author Huaxing
 */
@TableName(value = "playerrecharge")
@Data
@NoArgsConstructor
public class PlayerRechargeBO extends BaseEntity<PlayerRechargeBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "accountID", comment = "玩家账号ID")
    private long accountID;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "工会ID")
    private long familyID;
    @DataBaseField(type = "int(11)", fieldname = "appPrice", comment = "金额")
    private int appPrice;
    @DataBaseField(type = "varchar(50)", fieldname = "orderId", comment = "充值单号")
    private String orderId;
    @DataBaseField(type = "int(11)", fieldname = "preValue", comment = "充值前")
    private int preValue;
    @DataBaseField(type = "int(11)", fieldname = "curValue", comment = "充值后")
    private int curValue;
    @DataBaseField(type = "bigint(20)", fieldname = "orderTime", comment = "订单时间")
    private long orderTime;
    @DataBaseField(type = "int(11)", fieldname = "rechargeNum", comment = "充值数量")
    private int rechargeNum;
    @DataBaseField(type = "int(11)", fieldname = "sourceType", comment = "来源类型  （0：微信APP，1：微信H5...）")
    private int sourceType;
    @DataBaseField(type = "varchar(50)", fieldname = "platformType", comment = "平台名称 (WZ)")
    private String platformType;
    @DataBaseField(type = "int(5)", fieldname = "rechargeType", comment = "充值类型(0:普通商城,1:亲友圈商城)")
    private int rechargeType;
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;
    @DataBaseField(type = "int(5)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与资产或路由")
    private int classificationRegionId;


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerrecharge` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`accountID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家账号ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '充值时间',"
                + "`familyID` bigint(20) NOT NULL DEFAULT '0' COMMENT '工会ID',"
                + "`appPrice` int(11) NOT NULL DEFAULT '0' COMMENT '金额',"
                + "`orderId` varchar(50) NOT NULL DEFAULT '' COMMENT '充值单号',"
                + "`preValue`  int(11) NOT NULL DEFAULT '0' COMMENT '充值前值',"
                + "`curValue` int(11) NOT NULL DEFAULT '0' COMMENT '充值完成值',"
                + "`orderTime`  bigint(20) NOT NULL DEFAULT '0' COMMENT '订单时间',"
                + "`rechargeNum`  int(11) NOT NULL DEFAULT '0' COMMENT '充值数量',"
                + "`sourceType`  int(11) NOT NULL DEFAULT '0' COMMENT '来源类型  （0：微信APP，1：微信H5...）',"
                + "`platformType` varchar(50) NOT NULL DEFAULT '' COMMENT '平台名称 (WZ)',"
                + "`rechargeType`  int(11) NOT NULL DEFAULT '0' COMMENT '充值类型(0:普通商城,1:亲友圈商城)',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部代理ID',"
                + "`level` int(5) NOT NULL DEFAULT '0' COMMENT '俱乐部代理等级',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与资产或路由',"
                + "KEY `agentsID` (`agentsID`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='玩家充值'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }

}
