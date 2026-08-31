package core.db.entity.clarkGame;


import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "ClubMemberRelation")
@Data
@NoArgsConstructor
public class ClubMemberRelationBO extends BaseEntity<ClubMemberRelationBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "uid", comment = "亲友圈成员Id")
    private long uid;
    @DataBaseField(type = "bigint(20)", fieldname = "puid", comment = "上级亲友圈成员Id")
    private long puid;

    public ClubMemberRelationBO(long uid, long puid) {
        this.uid = uid;
        this.puid = puid;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubMemberRelation` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`uid` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈成员Id',"
                + "`puid` bigint(20) NOT NULL DEFAULT '0' COMMENT '上级亲友圈成员Id',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uid_puid` (`uid`,`puid`) USING BTREE,"
                + "KEY `uid` (`uid`),"
                + "KEY `puid` (`puid`)"
                + ") COMMENT='亲友圈成员级别关联表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
