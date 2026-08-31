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
@TableName(value = "XiPaiLog")
@Data
@NoArgsConstructor
public class XiPaiLogFlow extends BaseClarkLogEntity<XiPaiLogFlow> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "所属玩家亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "double(11,2)", fieldname = "value", comment = "值")
    private double value;
    @DataBaseField(type = "int(11)", fieldname = "dateTime", comment = "时间")
    private int dateTime;
    @DataBaseField(type = "varchar(8)", fieldname = "roomKey", comment = "房间key")
    private String roomKey = "";

    //增加
    public final static XiPaiLogFlow xiPaiLogInit(long pid, long clubId, long unionId, double value, String roomKey) {
        return new XiPaiLogFlow(pid, clubId, CommTime.nowSecond(), unionId, value, Integer.parseInt(CommTime.getNowTimeStringYMD()), roomKey);
    }

    public XiPaiLogFlow(long pid, long clubId, int createTime, long unionId, double value, int dateTime, String roomKey) {
        this.pid = pid;
        this.clubId = clubId;
        this.createTime = createTime;
        this.unionId = unionId;
        this.value = value;
        this.dateTime = dateTime;
        this.roomKey = roomKey;
    }


    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getPid()));
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO XiPaiLog"
                + "(`server_id`, `pid`, `clubId`, `createTime`, `unionId`, `value`, `dateTime`, `roomKey`)"
                + "values(?, ?, ?, ?, ?, ?, ?, ? )";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `XiPaiLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家亲友圈Id',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`value` varchar(50) NOT NULL DEFAULT '' COMMENT '值',"
                + "`dateTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`roomKey` varchar(8) NOT NULL DEFAULT '0' COMMENT '房间key',"
                + "PRIMARY KEY (`id`),"
                + "KEY `unionId_dateTime` (`unionId`,`dateTime`) USING BTREE"
                + ") COMMENT='洗牌日志'  DEFAULT CHARSET=utf8";
        return sql;
    }


    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[8];
        params[0] = Constant.serverIid;
        params[1] = pid;
        params[2] = clubId;
        params[3] = CommTime.nowSecond();
        params[4] = unionId;
        params[5] = value;
        params[6] = CommTime.getNowTimeStringYMD();
        params[7] = roomKey;
        return params;
    }

    @Override
    public String toString() {
        return "XiPaiLogFlow{" +
                "id=" + id +
                ", pid=" + pid +
                ", clubId=" + clubId +
                ", createTime=" + createTime +
                ", unionId=" + unionId +
                ", value='" + value + '\'' +
                ", dateTime=" + dateTime +
                ", roomKey='" + roomKey + '\'' +
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
