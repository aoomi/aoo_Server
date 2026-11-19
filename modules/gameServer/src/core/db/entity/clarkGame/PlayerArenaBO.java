package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

/**
 * 用户比赛场
 *
 * @author Huaxing
 */
@TableName(value = "playerArena")
@Data
@NoArgsConstructor
public class PlayerArenaBO extends BaseEntity<PlayerArenaBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "aid", comment = "比赛场ID")
    private long aid;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "用户ID")
    private long pid;
    @DataBaseField(type = "int(5)", fieldname = "freeCostCount", comment = "免费消耗使用次数")
    private int freeCostCount = 0;
    @DataBaseField(type = "int(11)", fieldname = "freeTime", comment = "初始免费使用时间")
    private int freeTime = 0;
    @DataBaseField(type = "int(11)", fieldname = "totalCount", comment = "总局数")
    private int totalCount = 0;
    @DataBaseField(type = "int(2)", fieldname = "costType", comment = "消耗类型1:免费,2:消费")
    private int costType = 0;
    @DataBaseField(type = "int(11)", fieldname = "curCount", comment = "当前完成局数")
    private int curCount = 0;

    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }

    public void saveAid(long aid) {
        if (this.aid == aid) {
            return;
        }
        this.aid = aid;
        getBaseService().update("aid", aid, id, new AsyncInfo(id));
    }

    public void saveFreeCostCount(int freeCostCount) {
        if (this.freeCostCount == freeCostCount) {
            return;
        }
        this.freeCostCount = freeCostCount;
        getBaseService().update("freeCostCount", freeCostCount, id, new AsyncInfo(id));
    }

    public void saveFreeTime(int freeTime) {
        if (this.freeTime == freeTime) {
            return;
        }
        this.freeTime = freeTime;
        getBaseService().update("freeTime", freeTime, id, new AsyncInfo(id));
    }

    public void saveTotalCount(int totalCount) {
        if (this.totalCount == totalCount) {
            return;
        }
        this.totalCount = totalCount;
        getBaseService().update("totalCount", totalCount, id, new AsyncInfo(id));
    }

    public void saveCostType(int costType) {
        if (this.costType == costType) {
            return;
        }
        this.costType = costType;
        getBaseService().update("costType", costType, id, new AsyncInfo(id));
    }

    public void saveFreeCostCountAndCostType(int freeCostCount, int costType, boolean isFreeTime) {
        if (freeCostCount < 0) {
            freeCostCount = 0;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("freeCostCount", freeCostCount);
        map.put("costType", costType);
        // 是否更新免费时间
        if (isFreeTime) {
            this.freeTime = CommTime.nowSecond();
            map.put("freeTime", this.freeTime);
        }
        this.freeCostCount = freeCostCount;
        this.costType = costType;
        getBaseService().update(map, id, new AsyncInfo(id));
    }

    public void saveCurCount(int curCount) {
        if (this.curCount == curCount) {
            return;
        }
        this.curCount = curCount;
        getBaseService().update("curCount", curCount, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerArena` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`aid` bigint(20) NOT NULL DEFAULT '0' COMMENT '比赛场ID',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',"
                + "`freeCostCount` int(5) NOT NULL DEFAULT '0' COMMENT '免费消耗使用次数',"
                + "`freeTime` int(11) NOT NULL DEFAULT '0' COMMENT '初始免费使用时间',"
                + "`totalCount` int(11) NOT NULL DEFAULT '0' COMMENT '总局数',"
                + "`costType` int(2) NOT NULL DEFAULT '0' COMMENT '消耗类型1:免费,2:消费',"
                + "`curCount` int(2) NOT NULL DEFAULT '0' COMMENT '当前完成局数',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='用户比赛场'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
