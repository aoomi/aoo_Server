package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推广员动态记录。
 *
 * @author Administrator
 */
@TableName(value = "promotionDynamic")
@Data
@NoArgsConstructor
public class PromotionDynamicBO extends BaseEntity<PromotionDynamicBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "所属玩家亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事Id")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "所属玩家ID")
    private long pid;
    @DataBaseField(type = "bigint(20)", fieldname = "execPid", comment = "执行操作玩家ID")
    private long execPid;
    @DataBaseField(type = "int(11)", fieldname = "execTime", comment = "执行时间")
    private int execTime;
    @DataBaseField(type = "int(3)", fieldname = "execType", comment = "执行类型")
    private int execType;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "dateTime", comment = "时间")
    private int dateTime;
    @DataBaseField(type = "varchar(50)", fieldname = "value", comment = "值")
    private String value = "";
    @DataBaseField(type = "varchar(50)", fieldname = "curValue", comment = "当前值")
    private String curValue = "";
    @DataBaseField(type = "varchar(50)", fieldname = "preValue", comment = "前值")
    private String preValue = "";
    @DataBaseField(type = "varchar(8)", fieldname = "roomKey", comment = "房间key")
    private String roomKey = "";
    @DataBaseField(type = "bigint(20)", fieldname = "partnerPid", comment = "合作伙伴PID")
    private long partnerPid;
    /**
     * 玩家名称
     */
    private String name;
    /**
     * 执行玩家名称
     */
    private String execName;
    /**
     * 值名称
     */
    private String valueName;

    public PromotionDynamicBO(long clubId, long unionId, long pid, long execPid, int execTime, int execType, int createTime, int dateTime, String value, String curValue, String preValue, String roomKey, long partnerPid) {
        this.clubId = clubId;
        this.unionId = unionId;
        this.pid = pid;
        this.execPid = execPid;
        this.execTime = execTime;
        this.execType = execType;
        this.createTime = createTime;
        this.dateTime = dateTime;
        this.value = value;
        this.curValue = curValue;
        this.preValue = preValue;
        this.roomKey = roomKey;
        this.partnerPid = partnerPid;
    }


    public static void insertPromotionDynamicBO(long clubId, long unionId, long pid, long execPid, int execType, String value, long partnerPid) {
        new PromotionDynamicBO(clubId, unionId, pid, execPid, CommTime.nowSecond(), execType, CommTime.nowSecond(), Integer.parseInt(CommTime.getNowTimeStringYMD()), value, "", "", "", partnerPid).insert();
    }


    public static void insertPromotionDynamicBO(long clubId, long unionId, long pid, long execPid, int execType, String value, String curValue, String roomKey, long partnerPid) {
        new PromotionDynamicBO(clubId, unionId, pid, execPid, CommTime.nowSecond(), execType, CommTime.nowSecond(), Integer.parseInt(CommTime.getNowTimeStringYMD()), value, curValue, "", roomKey, partnerPid).insert();
    }

    public static void insertPromotionDynamicBO(long clubId, long unionId, long pid, long execPid, int execType, String value, String curValue, long partnerPid) {
        new PromotionDynamicBO(clubId, unionId, pid, execPid, CommTime.nowSecond(), execType, CommTime.nowSecond(), Integer.parseInt(CommTime.getNowTimeStringYMD()), value, curValue, "", "", partnerPid).insert();
    }

    public static void insertPromotionDynamicBO(long clubId, long unionId, long pid, long execPid, int execType, long partnerPid) {
        new PromotionDynamicBO(clubId, unionId, pid, execPid, CommTime.nowSecond(), execType, CommTime.nowSecond(), Integer.parseInt(CommTime.getNowTimeStringYMD()), "", "", "", "", partnerPid).insert();
    }

    /**
     * 异步保存
     */
    public void insert() {
        this.getBaseService().save(this, new AsyncInfo(this.getPid()));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `promotionDynamic` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家亲友圈Id',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事Id',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属玩家ID',"
                + "`execPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行操作玩家ID',"
                + "`execTime` int(11) NOT NULL DEFAULT '0' COMMENT '执行时间',"
                + "`execType` int(3) NOT NULL DEFAULT '0' COMMENT '执行类型',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`dateTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`value` varchar(50) NOT NULL DEFAULT '' COMMENT '值',"
                + "`curValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '当前值',"
                + "`preValue` varchar(50) NOT NULL DEFAULT '0' COMMENT '前值',"
                + "`roomKey` varchar(8) NOT NULL DEFAULT '0' COMMENT '房间key',"
                + "`partnerPid` bigint(20) NOT NULL DEFAULT 0  COMMENT '合作伙伴PID',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='推广员动态记录'  DEFAULT CHARSET=utf8 AUTO_INCREMENT="
                + (ServerConfig.getInitialID() + 1);
        return sql;
    }

    @Override
    public String toString() {
        return "PromotionDynamicBO{" +
                "id=" + id +
                ", clubId=" + clubId +
                ", unionId=" + unionId +
                ", pid=" + pid +
                ", execPid=" + execPid +
                ", execTime=" + execTime +
                ", execType=" + execType +
                ", createTime=" + createTime +
                ", dateTime=" + dateTime +
                ", value='" + value + '\'' +
                ", curValue='" + curValue + '\'' +
                ", preValue='" + preValue + '\'' +
                ", roomKey='" + roomKey + '\'' +
                ", partnerPid=" + partnerPid +
                ", name='" + name + '\'' +
                ", execName='" + execName + '\'' +
                ", valueName='" + valueName + '\'' +
                '}';
    }
}
