package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 推广领取列表
 *
 * @author Huaxing
 */
@TableName(value = "refererReceiveList")
@Data
@NoArgsConstructor
public class RefererReceiveListBO extends BaseEntity<RefererReceiveListBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "refererId", comment = "推广人ID")
    private long refererId;
    @DataBaseField(type = "int(4)", fieldname = "sumCount", comment = "总次数")
    private int sumCount = 4;
    @DataBaseField(type = "int(4)", fieldname = "completeCount", comment = "完成次数")
    private int completeCount;
    @DataBaseField(type = "int(4)", fieldname = "receive", comment = "领取（0:No,1:Ok,2:Complete）")
    private int receive;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;

    public void savePid(long pid) {
        if (this.pid == pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }

    public void saveRefererId(long refererId) {
        if (this.refererId == refererId) {
            return;
        }
        this.refererId = refererId;
        getBaseService().update("refererId", refererId, id, new AsyncInfo(id));
    }

    public void saveSumCount(int sumCount) {
        if (this.sumCount == sumCount) {
            return;
        }
        this.sumCount = sumCount;
        getBaseService().update("sumCount", sumCount, id, new AsyncInfo(id));
    }

    public void saveCompleteCount(int completeCount) {
        if (this.completeCount == completeCount) {
            return;
        }
        this.completeCount = completeCount;
        getBaseService().update("completeCount", completeCount, id, new AsyncInfo(id));
    }

    public void saveReceive(int receive) {
        if (this.receive == receive) {
            return;
        }
        this.receive = receive;
        getBaseService().update("receive", receive, id, new AsyncInfo(id));
    }

    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `refererReceiveList` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`refererId` bigint(20) NOT NULL DEFAULT '0' COMMENT '推广人ID',"
                + "`sumCount` int(4) NOT NULL DEFAULT '4' COMMENT '总次数',"
                + "`completeCount` int(4) NOT NULL DEFAULT '0' COMMENT '完成次数',"
                + "`receive` int(4) NOT NULL DEFAULT '0' COMMENT '领取（0:No,1:Ok,2:Complete）',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "PRIMARY KEY (`id`),"
                + "KEY `refererId` (`refererId`),"
                + "KEY `pid` (`pid`)"
                + ") COMMENT='推广领取列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
