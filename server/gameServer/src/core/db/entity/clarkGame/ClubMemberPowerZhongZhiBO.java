package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中至成员权限表
 */
@TableName(value = "clubMemberPowerZhongZhi")
@Data
@NoArgsConstructor
public class ClubMemberPowerZhongZhiBO extends BaseEntity<ClubMemberPowerZhongZhiBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key",indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "memberId", comment = "成员id")
    private long memberId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "俱乐部ID")
    private long clubID;
    // 玩家游戏ID 长的
    @DataBaseField(type = "bigint(20)", fieldname = "playerID", comment = "玩家游戏短ID 长的")
    private long playerID;
    @DataBaseField(type = "int(2)", fieldname = "joinPower", comment = "加入审核")
    private int joinPower;
    @DataBaseField(type = "int(2)", fieldname = "kickPower", comment = "踢出成员")
    private int kickPower;
    @DataBaseField(type = "int(2)", fieldname = "changeCfgPower", comment = "调整玩法")
    private int changeCfgPower;
    @DataBaseField(type = "int(2)", fieldname = "edictNoticePower", comment = "编辑公告")
    private int edictNoticePower;
    @DataBaseField(type = "int(2)", fieldname = "invitePower", comment = "邀请成员")
    private int invitePower;
    @DataBaseField(type = "int(2)", fieldname = "kickTablePower", comment = "桌子踢人")
    private int kickTablePower;
    @DataBaseField(type = "int(2)", fieldname = "recordPower", comment = "战绩查看")
    private int recordPower;
    @DataBaseField(type = "int(2)", fieldname = "reportPower", comment = "举报成员")
    private int reportPower;
    @DataBaseField(type = "int(2)", fieldname = "matchPower", comment = "比赛管理")
    private int matchPower;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "更新时间")
    private int updateTime;




    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `clubMemberPowerZhongZhi` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`memberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '成员id',"
                + "`clubID` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部编号',"
                + "`playerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家游戏长ID',"
                + "`joinPower` int(2) NOT NULL DEFAULT 0  COMMENT '加入审核',"
                + "`kickPower` int(2) NOT NULL DEFAULT 0  COMMENT '踢出成员',"
                + "`changeCfgPower` int(2) NOT NULL DEFAULT 0  COMMENT '调整玩法',"
                + "`edictNoticePower` int(2) NOT NULL DEFAULT 0  COMMENT '编辑公告',"
                + "`invitePower` int(2) NOT NULL DEFAULT 0  COMMENT '邀请成员',"
                + "`kickTablePower` int(2) NOT NULL DEFAULT 0  COMMENT '桌子踢人',"
                + "`recordPower` int(2) NOT NULL DEFAULT 0  COMMENT '战绩查看',"
                + "`reportPower` int(2) NOT NULL DEFAULT 0  COMMENT '举报成员',"
                + "`matchPower` int(2) NOT NULL DEFAULT 0  COMMENT '比赛管理',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `m` (`memberId`) USING BTREE,"
                + "KEY `cp` (`clubID`,`playerID`) USING BTREE"
                + ") COMMENT='中至成员权限表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
    public void saveUpdateTime(int updateTime) {
        if (this.updateTime == updateTime) {
            return;
        }
        this.updateTime = updateTime;
        getBaseService().update("updateTime", updateTime,id,new AsyncInfo(id));
    }

}
