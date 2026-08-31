package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 * 亲友圈禁止玩家数据表
 *
 */
@TableName(value = "clubBanPlayer")
@Data
@NoArgsConstructor
public class ClubBanPlayerBO extends BaseEntity<ClubBanPlayerBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "varchar(50)", fieldname = "name", comment = "玩家名称/昵称")
    private String name = "";
    @DataBaseField(type = "varchar(300)", fieldname = "headImageUrl", comment = "头像url地址")
    private String headImageUrl = "";
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;
    @DataBaseField(type = "varchar(50)", fieldname = "deletePidName", comment = "操作玩家名称/昵称")
    private String deletePidName = "";
    public ClubBanPlayerBO(long unionId, String name, String headImageUrl, long pid, int createTime,String deletePidName) {
        this.clubId = unionId;
        this.pid = pid;
        this.name = name;
        this.headImageUrl = headImageUrl;
        this.createTime = createTime;
        this.deletePidName = deletePidName;
    }
    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().saveIgnoreOrUpDate(this, new AsyncInfo(id));
    }
    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubBanPlayer` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈ID',"
                + "`name` varchar(50) NOT NULL DEFAULT '' COMMENT '玩家名称/昵称',"
                + "`headImageUrl` varchar(300) NOT NULL DEFAULT '' COMMENT '头像url地址',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`deletePidName` varchar(50) NOT NULL DEFAULT '' COMMENT '操作玩家名称/昵称',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `clubId_pid` (`clubId`,`pid`) USING BTREE,"
                + "KEY `name` (`name`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='亲友圈禁止玩家数据表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public String getInsertSql() {
        return "INSERT ignore INTO clubBanPlayer"
                + "(`clubId`,`name`,`headImageUrl`,`pid`,`createTime`,`deletePidName`)"
                + "values(?, ?, ?, ?, ?,?)";
    }

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public Object[] addToBatch() {
        Object[] params = new Object[6];
        params[0] = clubId;
        params[1] = name;
        params[2] = headImageUrl;
        params[3] = pid;
        params[4] = createTime;
        params[5] = deletePidName;
        return params;
    }



}
