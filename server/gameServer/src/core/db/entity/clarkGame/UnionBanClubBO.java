package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 * 赛事禁止亲友圈配置数据表
 *
 * @author Huaxing
 */
@TableName(value = "unionBanClub")
@Data
@NoArgsConstructor
public class UnionBanClubBO extends BaseEntity<UnionBanClubBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "clubSign", comment = "随机的俱乐部标识ID")
    private int clubSign;// 随机的俱乐部标识ID
    @DataBaseField(type = "varchar(255)", fieldname = "name", comment = "俱乐部名称")
    private String name = "";// 俱乐部名称
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public UnionBanClubBO(long unionId, long clubId, int clubsign,String name, int createTime) {
        this.unionId = unionId;
        this.clubId = clubId;
        this.clubSign = clubsign;
        this.name = name;
        this.createTime = createTime;
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionBanClub` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`clubSign` int(11) NOT NULL DEFAULT '0' COMMENT '随机的俱乐部标识ID',"
                + "`name` varchar(255) NOT NULL DEFAULT ''  COMMENT '俱乐部名称',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionId_clubId` (`unionId`,`clubId`)"
                + ") COMMENT='赛事禁止亲友圈配置数据表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public String getInsertSql() {
        return "INSERT ignore INTO unionBanRoomConfig"
                + "(`unionId`,`clubId`,`clubSign`,`name`,`createTime`)"
                + "values(?, ?,?,? ?)";
    }

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public Object[] addToBatch() {
        Object[] params = new Object[5];
        params[0] = unionId;
        params[1] = clubId;
        params[2] = clubSign;
        params[3] = name;
        params[4] = createTime;
        return params;
    }



}
