package core.db.entity.clarkGame;


import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "ClubMemberReversedValue")
@Data
@NoArgsConstructor
public class ClubMemberReversedValueBo extends BaseEntity<ClubMemberReversedValueBo> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "uid", comment = "亲友圈成员Id")
    private long uid;
    @DataBaseField(type = "bigint(20)", fieldname = "puid", comment = "上级亲友圈成员Id")
    private long puid;
    @DataBaseField(type = "bigint(20)", fieldname = "reservedValue", comment = "预留值")
    private double reversedValue;

    public ClubMemberReversedValueBo(long uid, long puid, double reversedValue) {
        this.uid = uid;
        this.puid = puid;
        this.reversedValue = reversedValue;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubMemberReversedValue` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`uid` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈成员Id',"
                + "`puid` bigint(20) NOT NULL DEFAULT '0' COMMENT '上级亲友圈成员Id',"
                + "`reservedValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '预留值',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uid_puid` (`uid`,`puid`) USING BTREE,"
                + "KEY `uid` (`uid`),"
                + "KEY `puid` (`puid`)"
                + ") COMMENT='亲友圈成员预留值'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
