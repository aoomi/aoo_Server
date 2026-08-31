package core.db.entity.clarkLog;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseClarkLogEntity;
import core.ioc.Constant;
import lombok.Data;

@TableName(value = "PromotionMultiChangLog")
@Data
public class PromotionMultiChangLogFlow extends BaseClarkLogEntity<PromotionMultiChangLogFlow> {

    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "玩家pid")
    private long pid = 0;//批量移动到哪个人下
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈Id")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "exePid", comment = "玩家pid移动的")
    private long exePid;//批量移动的最高级人员
    @DataBaseField(type = "int(11)", fieldname = "oldNum", comment = "目标玩家原来有多少人")
    private int oldNum = 0;
    @DataBaseField(type = "int(11)", fieldname = "nowNum", comment = "目标玩家现在有多少人")
    private int nowNum = 0;
    @DataBaseField(type = "int(11)", fieldname = "exeOldNum", comment = "被移动的原来有多少人")
    private int exeOldNum = 0;
    @DataBaseField(type = "int(11)", fieldname = "exeNowNum", comment = "被移动的现在有多少人")
    private int exeNowNum = 0;
    @DataBaseField(type = "bigint(20)", fieldname = "doPid", comment = "执行操作的玩家")
    private long doPid;//执行操作的玩家
    @DataBaseField(type = "longtext", fieldname = "pidList", comment = "被移动的玩家列表（包括被移动的推广员）")
    private String pidList;
    @DataBaseField(type = "longtext", fieldname = "pidListNow", comment = "目前被移动的玩家列表（不包括被移动的推广员）")
    private String pidListNow;//执行操作的玩家
    @DataBaseField(type = "longtext", fieldname = "upPidList", comment = "目标玩家列表（不包括被移动到的推广员）")
    private String upPidList;
    @DataBaseField(type = "longtext", fieldname = "upPidListNow", comment = "目前目标玩家列表（不包括被移动到的推广员）")
    private String upPidListNow;//执行操作的玩家

    public PromotionMultiChangLogFlow() {
    }

    public PromotionMultiChangLogFlow(long pid, long clubId, long exePid, int oldNum, int nowNum, int exeOldNum, int exeNowNum,
                                      long doPid, String pidList, String pidListNow, String upPidList, String upPidListNow) {
        this.pid = pid;
        this.clubId = clubId;
        this.exePid = exePid;
        this.oldNum = oldNum;
        this.nowNum = nowNum;
        this.exeOldNum = exeOldNum;
        this.exeNowNum = exeNowNum;
        this.doPid = doPid;
        this.pidList = pidList;
        this.pidListNow = pidListNow;
        this.upPidList = upPidList;
        this.upPidListNow = upPidListNow;
    }

    public long getDoPid() {
        return doPid;
    }

    public void setDoPid(long doPid) {
        this.doPid = doPid;
    }

    public String getPidList() {
        return pidList;
    }

    public void setPidList(String pidList) {
        this.pidList = pidList;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public long getExePid() {
        return exePid;
    }

    public void setExePid(long exePid) {
        this.exePid = exePid;
    }

    public int getOldNum() {
        return oldNum;
    }

    public void setOldNum(int oldNum) {
        this.oldNum = oldNum;
    }

    public int getNowNum() {
        return nowNum;
    }

    public void setNowNum(int nowNum) {
        this.nowNum = nowNum;
    }

    public int getExeOldNum() {
        return exeOldNum;
    }

    public void setExeOldNum(int exeOldNum) {
        this.exeOldNum = exeOldNum;
    }

    public int getExeNowNum() {
        return exeNowNum;
    }

    public void setExeNowNum(int exeNowNum) {
        this.exeNowNum = exeNowNum;
    }

    public String getPidListNow() {
        return pidListNow;
    }

    public void setPidListNow(String pidListNow) {
        this.pidListNow = pidListNow;
    }

    public String getUpPidList() {
        return upPidList;
    }

    public void setUpPidList(String upPidList) {
        this.upPidList = upPidList;
    }

    public String getUpPidListNow() {
        return upPidListNow;
    }

    public void setUpPidListNow(String upPidListNow) {
        this.upPidListNow = upPidListNow;
    }

    @Override
    public String getInsertSql() {
        return "INSERT INTO PromotionMultiChangLog"
                + "(`serverId`, `timestamp`, `dateTime`,`pid`, `clubId`, `exePid`, `oldNum`, `nowNum`,`exeOldNum`, `exeNowNum`, `doPid`,`pidList`,`pidListNow`,`upPidList`,`upPidListNow`)"
                + "values(?,?,?,?, ?, ?, ?, ?, ?,?,?,?,?,?,?)";
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE IF NOT EXISTS `PromotionMultiChangLog` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`serverId` int(11) NOT NULL DEFAULT '0' COMMENT '服务器ID',"
                + "`timestamp` int(11) NOT NULL DEFAULT '0' COMMENT '日志时间(时间戳)',"
                + "`dateTime` varchar(20) NOT NULL DEFAULT '20160801' COMMENT '日志时间(yyyymmdd)',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈Id',"
                + "`exePid` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家pid移动的',"
                + "`oldNum` int(11) NOT NULL DEFAULT '0' COMMENT '原来有多少人',"
                + "`nowNum` int(11) NOT NULL DEFAULT '0' COMMENT '现在有多少人',"
                + "`exeOldNum` int(11) NOT NULL DEFAULT '0' COMMENT '被移动的原来有多少人',"
                + "`exeNowNum` int(11) NOT NULL DEFAULT '0' COMMENT '被移动的现在有多少人',"
                + "`doPid` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行操作玩家',"
                + "`pidList` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '被移动的玩家列表（包括被移动的推广员）',"
                + "`pidListNow` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目前被移动的玩家列表（不包括被移动的推广员）',"
                + "`upPidList` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '移动到的玩家列表（不包括自己）',"
                + "`upPidListNow` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目前移动到的玩家列表（不包括自己）',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='推广员批量修改日志' DEFAULT CHARSET=utf8";
        return sql;
    }

    @Override
    public Object[] addToBatch() {
        Object[] params = new Object[15];
        params[0] = Constant.serverIid;
        params[1] = CommTime.nowSecond();
        params[2] = CommTime.getNowTimeStringYMD();
        params[3] = pid;
        params[4] = clubId;
        params[5] = exePid;
        params[6] = oldNum;
        params[7] = nowNum;
        params[8] = exeOldNum;
        params[9] = exeNowNum;
        params[10] = doPid;
        params[11] = pidList;
        params[12] = pidListNow;
        params[13] = upPidList;
        params[14] = upPidListNow;
        return params;
    }
}
