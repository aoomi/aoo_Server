package core.db.entity.clarkGame;

import cenum.RebateEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家充值
 *
 * @author Huaxing
 */
@TableName(value = "rebate")
@Data
@NoArgsConstructor
public class RebateBO extends BaseEntity<RebateBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "accountID", comment = "玩家账号ID", indextype = DataBaseField.IndexType.Normal)
    private long accountID;
    @DataBaseField(type = "varchar(255)", fieldname = "sourceOfTime", comment = "返利时间", indextype = DataBaseField.IndexType.Normal)
    private String sourceOfTime = "";
    @DataBaseField(type = "int(11)", fieldname = "rebateType", comment = "来源0未知1推荐人2代理 标识accountID身份3任务4赏金")
    private int rebateType;
    @DataBaseField(type = "bigint(20)", fieldname = "sourceOfAccount", comment = "来充值自玩家的ID")
    private long sourceOfAccount;
    @DataBaseField(type = "int(11)", fieldname = "app_price", comment = "金额(单位分)")
    private int app_price;
    @DataBaseField(type = "varchar(50)", fieldname = "order_id", comment = "充值单号")
    private String order_id = "";
    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "工会ID", indextype = DataBaseField.IndexType.Normal)
    private long familyID = 10001L;
    @DataBaseField(type = "int(11)", fieldname = "flag", comment = "0:玩家1:代理 判断充值用户的推荐人是代理还是玩家", indextype = DataBaseField.IndexType.Normal)
    private int flag = RebateEnum.RebateFlag.REBATEFLAG_PLAYER.value();
    @DataBaseField(type = "bigint(20)", fieldname = "ActivityID", comment = "活动ID")
    private long ActivityID;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与资产或路由")
    private int classificationRegionId;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `rebate` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`accountID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家账号ID',"
                + "`sourceOfTime` varchar(255) NOT NULL DEFAULT '' COMMENT '返利时间',"
                + "`rebateType` int(11) NOT NULL DEFAULT '0' COMMENT '来源0未知1推荐人2代理 标识accountID身份3任务4赏金',"
                + "`sourceOfAccount` bigint(20) NOT NULL DEFAULT '0' COMMENT '来自充值玩家的ID',"
                + "`app_price`  int(11) NOT NULL DEFAULT '0.0' COMMENT '金额(单位分)',"
                + "`order_id` varchar(50) NOT NULL DEFAULT '' COMMENT '充值单号',"
                + "`familyID` bigint(20) NOT NULL DEFAULT '10001' COMMENT '工会ID',"
                + "`flag`  int(11) NOT NULL DEFAULT '0' COMMENT '0:玩家1:代理 判断充值用户的推荐人是代理还是玩家',"
                + "`ActivityID` varchar(50) NOT NULL DEFAULT '' COMMENT '活动ID',"
                + "`gameType` bigint(20) NOT NULL DEFAULT '10001' COMMENT '游戏类型',"
                + "`roomID`  int(11) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与资产或路由',"
                + "PRIMARY KEY (`id`),"
                + "KEY `accountID` (`accountID`),"
                + "KEY `ARAG` (`accountID`,`rebateType`,`ActivityID`,`gameType`),"
                + "KEY `familyID` (`familyID`),"
                + "KEY `sourceOfTime` (`sourceOfTime`)"
                + ") COMMENT='玩家返利'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    public void saveFamilyID(long familyID) {
        if (familyID == this.familyID) {
            return;
        }
        this.familyID = familyID;
        getBaseService().update("familyID", familyID, id, new AsyncInfo(id));
    }
}
