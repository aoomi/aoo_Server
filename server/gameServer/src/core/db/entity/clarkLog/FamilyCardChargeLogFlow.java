package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 代理圈卡交易记录
 *
 * @author Administrator
 */
@TableName(value = "FamilyCardChargeLog")
@Data
public class FamilyCardChargeLogFlow extends BaseClarkLogEntity<FamilyCardChargeLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "代理ID")
    private long familyID = 0; // 代理ID
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid = 0; // 玩家pid
    @DataBaseField(type = "int(11)", fieldname = "num", comment = "数量")
    private int num = 0; // 数量
    @DataBaseField(type = "int(11)", fieldname = "cur_remainder", comment = "当前剩余")
    private int cur_remainder = 0; // 当前剩余
    @DataBaseField(type = "int(11)", fieldname = "pre_value", comment = "前值")
    private int pre_value = 0; // 前值
    @DataBaseField(type = "int(11)", fieldname = "type", comment = "类型(1:获得,2:消耗)")
    private int type = 0; // 类型(1:获得,2:消耗)
    @DataBaseField(type = "int(11)", fieldname = "sourceType", comment = "来源类型(1:后台,2:玩家)")
    private int sourceType = 0; // 来源类型(1:后台,2:玩家)
    @DataBaseField(type = "int(11)", fieldname = "classificationRegionId", comment = "玩法地区分类统计ID，不参与身份、资产或路由")
    private int classificationRegionId;

    public FamilyCardChargeLogFlow() {
    }

    public FamilyCardChargeLogFlow(long familyID, long pid, int num, int cur_remainder, int pre_value, int type, int sourceType, int classificationRegionId) {
        this.familyID = familyID;
        this.pid = pid;
        this.num = num;
        this.cur_remainder = cur_remainder;
        this.pre_value = pre_value;
        this.type = type;
        this.sourceType = sourceType;
        this.classificationRegionId = classificationRegionId;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO FamilyCardChargeLog"
                + "(`server_id`, `timestamp`, `date_time`, `familyID`, `pid`, `num`, `cur_remainder`, `pre_value`, `type`, `sourceType`, `classificationRegionId`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `FamilyCardChargeLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`familyID` bigint(20) NOT NULL DEFAULT '0' COMMENT '代理ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`num` int(11) NOT NULL DEFAULT '0' COMMENT '数量',"
                + "`cur_remainder` int(11) NOT NULL DEFAULT '0' COMMENT '当前剩余',"
                + "`pre_value` int(11) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`type` int(11) NOT NULL DEFAULT '0' COMMENT '类型(1:获得,2:消耗)',"
                + "`sourceType` int(11) NOT NULL DEFAULT '0' COMMENT '来源类型(1:后台,2:玩家)',"
                + "`classificationRegionId` int(11) NOT NULL DEFAULT '0' COMMENT '玩法地区分类统计ID，不参与身份、资产或路由',"
                + "PRIMARY KEY (`id`),"
                + "KEY `T` (`type`),"
                + "KEY `S` (`sourceType`),"
                + "KEY `Ts` (`timestamp`)"
                + ") COMMENT='Family Card Charge 代理圈卡交易记录' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[11];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = familyID;
        params[4] = pid;
        params[5] = num;
        params[6] = cur_remainder;
        params[7] = pre_value;
        params[8] = type;
        params[9] = sourceType;
        params[10] = classificationRegionId;
        return params;
    }
}
