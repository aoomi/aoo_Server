package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

/**
 * 亲友圈换圈主记录表
 */
@TableName(value = "ClubChangeCreateLog")
@Data
public class ClubChangeCreateLogFlow extends BaseClarkLogEntity<ClubChangeCreateLogFlow> {


    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "旧的圈主id")
    private long oldPid = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "newPid", comment = "新的圈主id")
    private long newPid = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "int(11)", fieldname = "creattime", comment = "创建时间")
    private int creattime;// 申请时间
    public ClubChangeCreateLogFlow() {
    }

    public ClubChangeCreateLogFlow( long oldPid,  long newPid, long clubId, int creattime) {
        this.oldPid = oldPid;
        this.newPid = newPid;
        this.clubId = clubId;
        this.creattime = creattime;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO ClubChangeCreateLog"
                + "(`server_id`, `timestamp`, `date_time`, `oldPid`, `newPid`, `clubId`, `creattime`)"
                + "values(?, ?, ?, ?, ?, ?, ?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `ClubChangeCreateLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`server_id` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`date_time` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`oldPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '旧的圈主id',"
                + "`newPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '新的圈主id',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`creattime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='亲友圈圈主改变日志表' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[7];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = oldPid;
        params[4] = newPid;
        params[5] = clubId;
        params[6] = creattime;
        return params;
    }
}
