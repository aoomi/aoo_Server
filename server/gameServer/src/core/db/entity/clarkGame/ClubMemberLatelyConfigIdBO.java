package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "clubMemberLatelyConfigId")
@Data
@NoArgsConstructor
public class ClubMemberLatelyConfigIdBO extends BaseEntity<ClubMemberLatelyConfigIdBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    @DataBaseField(type = "bigint(20)", fieldname = "unionID", comment = "赛事ID")
    private long unionID;
    @DataBaseField(type = "bigint(20)", fieldname = "memberID", comment = "亲友圈成员ID")
    private long memberID;
    @DataBaseField(type = "bigint(20)", fieldname = "configID", comment = "配置ID")
    private long configID;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始时间")
    private int startTime;


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubMemberLatelyConfigId` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部ID',"
                + "`unionID` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`memberID` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈成员ID',"
                + "`configID` bigint(20) NOT NULL DEFAULT '0' COMMENT '配置ID',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始时间',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间'," + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `unionID_clubID_memberID_configID` (`unionID`,`clubID`,`memberID`,`configID`) USING BTREE"
                + ") COMMENT='成员最近加入房间信息表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
