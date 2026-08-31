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
 * 用户签到表
 *
 * @author Huaxing
 */
@TableName(value = "playerSign")
@Data
@NoArgsConstructor
public class PlayerSignBO extends BaseEntity<PlayerSignBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "pid", comment = "角色ID")
    private long pid;
    @DataBaseField(type = "int(5)", fieldname = "signCount", comment = "签到统计")
    private int signCount = 0;
    @DataBaseField(type = "int(11)", fieldname = "signTime", comment = "签到时间")
    private int signTime = 0;
    @DataBaseField(type = "int(11)", fieldname = "rewardTime", comment = "领取时间")
    private int rewardTime = 0;
    @DataBaseField(type = "int(2)", fieldname = "rewardState", comment = "领取状态")
    private int rewardState = 1;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime = 0;


    public void savePid(long pid) {
        if (pid == this.pid) {
            return;
        }
        this.pid = pid;
        getBaseService().update("pid", pid, id, new AsyncInfo(id));
    }


    public void saveSignCount(int signCount) {
        if (this.signCount == signCount) {
            return;
        }
        this.signCount = signCount;
        getBaseService().update("signCount", signCount, id, new AsyncInfo(id));
    }

    public void saveSignTime(int signTime) {
        if (this.signTime == signTime) {
            return;
        }
        this.signTime = signTime;
        getBaseService().update("signTime", signTime, id, new AsyncInfo(id));
    }

    public void saveRewardTime(int rewardTime) {
        if (this.rewardTime == rewardTime) {
            return;
        }
        this.rewardTime = rewardTime;
        getBaseService().update("rewardTime", rewardTime, id, new AsyncInfo(id));
    }

    public void saveRewardState(int rewardState) {
        if (this.rewardState == rewardState) {
            return;
        }
        this.rewardState = rewardState;
        getBaseService().update("rewardState", rewardState, id, new AsyncInfo(id));
    }


    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    /**
     * 保存领取奖励
     */
    public void saveReward() {
        // 领取状态
        this.setRewardState(0);
        // 领取时间
        this.setRewardTime(CommTime.nowSecond());
        HashMap<String, Object> map = new HashMap<>(2);
        map.put("rewardState", this.getRewardState());
        map.put("rewardTime", this.getRewardTime());
        this.getBaseService().update(map, id);
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `playerSign` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '角色ID',"
                + "`signCount` int(5) NOT NULL DEFAULT '0' COMMENT '签到统计',"
                + "`signTime` int(11) NOT NULL DEFAULT '0' COMMENT '签到时间',"
                + "`rewardTime` int(11) NOT NULL DEFAULT '0' COMMENT '领取时间',"
                + "`rewardState` int(2) NOT NULL DEFAULT '0' COMMENT '领取状态',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "UNIQUE INDEX `pid` (`pid`),"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='用户签到表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
