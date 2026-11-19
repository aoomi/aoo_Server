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
 * 赛事禁止玩家游戏数据表
 */
@TableName(value = "unionBanGamePlayer")
@Data
@NoArgsConstructor
public class UnionBanGamePlayerBO extends BaseEntity<UnionBanGamePlayerBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "varchar(50)", fieldname = "name", comment = "玩家名称/昵称")
    private String name = "";
    @DataBaseField(type = "varchar(300)", fieldname = "headImageUrl", comment = "头像url地址")
    private String headImageUrl = "";
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public UnionBanGamePlayerBO(long unionId, String name, String headImageUrl, long pid, int createTime) {
        this.unionId = unionId;
        this.pid = pid;
        this.name = name;
        this.headImageUrl = headImageUrl;
        this.createTime = createTime;
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().saveIgnoreOrUpDate(this, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `unionBanGamePlayer` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`name` varchar(50) NOT NULL DEFAULT '' COMMENT '玩家名称/昵称',"
                + "`headImageUrl` varchar(300) NOT NULL DEFAULT '' COMMENT '头像url地址',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家Pid',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionId_pid` (`unionId`,`pid`) USING BTREE,"
                + "KEY `name` (`name`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='赛事禁止玩家游戏数据表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public String getInsertSql() {
        return "INSERT ignore INTO unionBanGamePlayer"
                + "(`unionId`,`name`,`headImageUrl`,`pid`,`createTime`)"
                + "values(?, ?, ?, ?, ?)";
    }

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public Object[] addToBatch() {
        Object[] params = new Object[5];
        params[0] = unionId;
        params[1] = name;
        params[2] = headImageUrl;
        params[3] = pid;
        params[4] = createTime;
        return params;
    }


}
