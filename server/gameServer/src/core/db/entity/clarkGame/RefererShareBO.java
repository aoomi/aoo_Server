package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推广分享表
 *
 * @author Huaxing
 */
@TableName(value = "refererShare")
@Data
@NoArgsConstructor
public class RefererShareBO extends BaseEntity<RefererShareBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid;
    @DataBaseField(type = "int(4)", fieldname = "type", comment = "类型")
    private int type;
    @DataBaseField(type = "int(11)", fieldname = "receiveCardNum", comment = "领取卡数")
    private int receiveCardNum;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public void savePid(long pid) {
        if (this.pid == pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }

    public void saveType(int type) {
        if (this.type == type) {
            return;
        }
        this.type = type;
        getBaseService().update("type", type, id, new AsyncInfo(id));
    }

    public void saveReceiveCardNum(int receiveCardNum) {
        if (this.receiveCardNum == receiveCardNum) {
            return;
        }
        this.receiveCardNum = receiveCardNum;
        getBaseService().update("receiveCardNum", receiveCardNum, id, new AsyncInfo(id));
    }

    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `refererShare` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`type` int(4) NOT NULL DEFAULT '0' COMMENT '类型',"
                + "`receiveCardNum` int(11) NOT NULL DEFAULT '0' COMMENT '领取卡数',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='推广分享表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

}
