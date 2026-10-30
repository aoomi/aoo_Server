package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.CommLogD;
import com.google.common.collect.Maps;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 颁奖记录
 * @author Huaxing
 *
 */
@TableName(value = "unionAwardRecord")
@Data
@NoArgsConstructor
public class UnionAwardRecordBO extends BaseEntity<UnionAwardRecordBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key",indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "int(2)", fieldname = "awardNum", comment = "颁奖次数")
    private int awardNum;
    @DataBaseField(type = "int(11)", fieldname = "awardTime", comment = "颁奖时间")
    private int awardTime;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionAwardRecord` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`awardNum` int(2) NOT NULL DEFAULT '-1' COMMENT '颁奖次数',"
                + "`awardTime` int(11) NOT NULL DEFAULT '0' COMMENT '颁奖时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId` (`unionId`)"
                + ") COMMENT='颁奖记录'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    /**
     * 异步保存
     */
    public void insert() {
        long id=this.getBaseService().save(this, new AsyncInfo(this.getUnionId()));
        if(id<0){
            CommLogD.error("unionAwardRecord save error:"+this.toString());
        }
    }


}
