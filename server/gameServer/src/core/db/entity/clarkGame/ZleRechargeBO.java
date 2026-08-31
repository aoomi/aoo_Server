package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@TableName(value = "zleRecharge")
@Data
@NoArgsConstructor
public class ZleRechargeBO extends BaseEntity<ZleRechargeBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "keyId", comment = "会长或副会长ID")
    private long keyId;
    @DataBaseField(type = "bigint(20)", fieldname = "toId", comment = "目标玩家ID")
    private long toId;
    @DataBaseField(type = "int(11)", fieldname = "roomCard", comment = "房卡值")
    private int roomCard;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "buy_roomcard_num", comment = "买房卡值")
    private int buy_roomcard_num;
    @DataBaseField(type = "int(11)", fieldname = "give_roomcard_num", comment = "给房卡值")
    private int give_roomcard_num;
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型")
    private int type = 1;
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与资产或路由")
    private int classificationRegionId;
    @DataBaseField(type = "varchar(255)", fieldname = "beizhu", comment = "备注")
    private String beizhu = "";

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `zleRecharge` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`keyId` bigint(20) NOT NULL DEFAULT '0' COMMENT '会长或副会长ID',"
                + "`toId` bigint(20) NOT NULL DEFAULT '0' COMMENT '目标玩家ID',"
                + "`roomCard` int(11) NOT NULL DEFAULT '0' COMMENT '房卡值',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`buy_roomcard_num` int(11) NOT NULL DEFAULT '0' COMMENT '买房卡值',"
                + "`give_roomcard_num` int(11) NOT NULL DEFAULT '0' COMMENT '给房卡值',"
                + "`type` int(11) NOT NULL DEFAULT '1' COMMENT '类型',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与资产或路由',"
                + "`beizhu` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='掌乐充值'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1";
        return sql;
    }

    public void setBeizhu(String beizhu) {
        if (StringUtils.isEmpty(beizhu)) {
            return;
        }
        this.beizhu = beizhu;
    }
}
