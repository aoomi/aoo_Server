package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 胡牌类型奖励记录
 *
 * @author Huaxing
 */
@TableName(value = "huRewardRecord")
@Data
@NoArgsConstructor
public class HuRewardRecordBO extends BaseEntity<HuRewardRecordBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "roomId", comment = "房间ID")
    private long roomId;
    @DataBaseField(type = "int(11)", fieldname = "setId", comment = "当局ID")
    private int setId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家ID")
    private long pid;
    @DataBaseField(type = "int(11)", fieldname = "gameType", comment = "游戏类型")
    private int gameType;
    @DataBaseField(type = "text", fieldname = "prize", comment = "奖品信息")
    private String prize;
    @DataBaseField(type = "int(11)", fieldname = "create_time", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "state", comment = "状态(0:未领取,1:领取)")
    private int state;
    @DataBaseField(type = "int(11)", fieldname = "huType", comment = "胡类型")
    private int huType;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `huRewardRecord` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`roomId` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`setId` int(11) NOT NULL DEFAULT '0' COMMENT '当局ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家ID',"
                + "`gameType` int(11) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`prize` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '奖品信息',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`state` int(11) NOT NULL DEFAULT '0' COMMENT '状态(0:未领取,1:领取)',"
                + "`huType` int(11) NOT NULL DEFAULT '0' COMMENT '胡类型'," + "PRIMARY KEY (`id`),"
                + "KEY `createTime` (`createTime`)," + "KEY `r_s_p_h` (`roomId`,`setId`,`pid`,`huType`) USING BTREE"
                + ") COMMENT='胡牌类型奖励记录'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }
}
