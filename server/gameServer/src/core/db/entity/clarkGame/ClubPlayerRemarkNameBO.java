package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "clubPlayerRemarkName")
@Data
@NoArgsConstructor
public class ClubPlayerRemarkNameBO extends BaseEntity<ClubPlayerRemarkNameBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "remarkID", comment = "备注的玩家ID")
    private long remarkID;
    @DataBaseField(type = "varchar(50)", fieldname = "remarkName", comment = "备注的名称")
    private String remarkName = "";

    public void savePid(String remarkName) {
        if (java.util.Objects.equals(remarkName, this.remarkName)) {
            return;
        }
        this.remarkName = remarkName;
        getBaseService().update("remarkName", remarkName, id, new AsyncInfo(id));
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubPlayerRemarkName` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`remarkID` bigint(20) NOT NULL DEFAULT '0' COMMENT '备注的玩家ID',"
                + "`remarkName` varchar(50) NOT NULL DEFAULT '0' COMMENT '备注的名称',"
                + "PRIMARY KEY (`id`),"
                + "KEY `PID` (`pid`)"
                + ") COMMENT='玩家圈卡'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getPid()));
    }
}
