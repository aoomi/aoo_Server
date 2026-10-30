package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 *中至俱乐部管理员功能
 *
 * @author Huaxing
 */
@TableName(value = "clubMinisterZhongZhi")
@Data
@NoArgsConstructor
public class ClubMinisterZhongZhiBO extends BaseEntity<ClubMinisterZhongZhiBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "俱乐部Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "opClubId", comment = "俱乐部Id(被管理的俱乐部id)")
    private long opClubId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public ClubMinisterZhongZhiBO(long unionId, long clubId, long opClubId, int createTime) {
        this.unionId = unionId;
        this.clubId = clubId;
        this.opClubId = opClubId;
        this.createTime = createTime;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubMinisterZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部Id',"
                + "`opClubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部Id(被管理的俱乐部id)',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionId_opClubId` (`unionId`,`opClubId`)"
                + ") COMMENT='中至俱乐部管理员功能'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public String getInsertSql() {
        return "INSERT ignore INTO clubMinisterZhongZhi"
                + "(`unionId`,`clubId`,`opClubId`,`createTime`)"
                + "values(?, ?, ?, ?)";
    }

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public Object[] addToBatch() {
        Object[] params = new Object[4];
        params[0] = unionId;
        params[1] = clubId;
        params[2] = opClubId;
        params[3] = createTime;
        return params;
    }



}
