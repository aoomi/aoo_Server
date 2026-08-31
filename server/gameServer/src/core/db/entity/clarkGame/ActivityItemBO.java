package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "activityItem")
@Data
@NoArgsConstructor
public class ActivityItemBO extends BaseEntity<ActivityItemBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(3)", fieldname = "type", comment = "活动类型(1:积分排行)")
    private int type;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;


    public void saveUpdateTime(int updateTime) {
        if (this.updateTime == updateTime) {
            return;
        }
        this.updateTime = updateTime;
        getBaseService().update("updateTime", updateTime, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `activityItem` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`type` int(3) NOT NULL DEFAULT '0' COMMENT '活动类型',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='活动项表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
