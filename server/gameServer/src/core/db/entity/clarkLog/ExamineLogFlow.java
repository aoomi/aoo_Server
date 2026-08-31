package core.db.entity.clarkLog;

import cenum.DispatcherComponentLogEnum;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "ExamineLog")
@Data
@NoArgsConstructor
public class ExamineLogFlow extends BaseClarkLogEntity<ExamineLogFlow> {
    @DataBaseField(type = "int(11)", fieldname = "server_id", comment = "服务器ID")
    private int server_id;
    @DataBaseField(type = "int(11)", fieldname = "timestamp", comment = "日志时间(时间戳)")
    private int timestamp;
    @DataBaseField(type = "varchar(20)", fieldname = "date_time", comment = "日志时间(yyyymmdd)")
    private String date_time;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家Pid")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "toClubMemberId", comment = "玩家亲友圈身份id")
    private long toClubMemberId;
    @DataBaseField(type = "bigint(20)", fieldname = "doClubMemberId", comment = "执行审核玩家亲友圈身份id")
    private long doClubMemberId;
    @DataBaseField(type = "double(11,2)", fieldname = "beforeValue", comment = "审核前的值")
    private double beforeValue;
    @DataBaseField(type = "double(11,2)", fieldname = "value", comment = "审核的值")
    private double value;
    @DataBaseField(type = "double(11,2)", fieldname = "curValue", comment = "审核后的值")
    private double curValue;

    public ExamineLogFlow(String date_time, long pid, long toClubMemberId, long doClubMemberId, double beforeValue, double value, double curValue) {
        this.server_id = Constant.serverIid;
        this.timestamp = CommTime.nowSecond();
        this.date_time = date_time;
        this.pid = pid;
        this.toClubMemberId = toClubMemberId;
        this.doClubMemberId = doClubMemberId;
        this.beforeValue = beforeValue;
        this.value = value;
        this.curValue = curValue;
    }

    //增加
    public final static ExamineLogFlow examineLogFlowInit(String date_time, long pid, long toClubMemberId, long doClubMemberId, double beforeValue, double value, double curValue) {
        return new ExamineLogFlow(date_time, pid, toClubMemberId, doClubMemberId, beforeValue, value, curValue);
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ExamineLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`toClubMemberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家亲友圈身份id',"
                + "`doClubMemberId` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行审核玩家亲友圈身份id',"
                + "`beforeValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '审核前的值',"
                + "`value` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '审核的值',"
                + "`curValue` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '审核后的值',"
                + "PRIMARY KEY (`id`),"
                + "KEY `dateTime_toClubMemberId` (`date_time`,`toClubMemberId`) USING BTREE"
                + ") COMMENT='审核记录日志'  DEFAULT CHARSET=utf8";
        return sql;
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getPid()));
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ExamineLog"
                + "(`server_id`, `timestamp`, `date_time`, `pid`, `toClubMemberId`, `doClubMemberId`, `beforeValue`, `value`, `curValue`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ?,? )";
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[9];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = date_time;
        params[3] = pid;
        params[4] = toClubMemberId;
        params[5] = doClubMemberId;
        params[6] = beforeValue;
        params[7] = value;
        params[8] = curValue;
        return params;
    }

    @Override
    public String toString() {
        return "ExamineLogFlow{" +
                "server_id=" + server_id +
                ", timestamp=" + timestamp +
                ", date_time='" + date_time + '\'' +
                ", pid=" + pid +
                ", toClubMemberId=" + toClubMemberId +
                ", doClubMemberId=" + doClubMemberId +
                ", beforeValue=" + beforeValue +
                ", value=" + value +
                ", curValue=" + curValue +
                '}';
    }

    /**
     * 进程Id
     *
     * @return
     */
    @Override
    public int threadId() {
        return DispatcherComponentLogEnum.OTHER_BD_LOG.id();
    }

    /**
     * 环大小
     *
     * @return
     */
    @Override
    public int bufferSize() {
        return DispatcherComponentLogEnum.OTHER_BD_LOG.bufferSize();
    }
}
