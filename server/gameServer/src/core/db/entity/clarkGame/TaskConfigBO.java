package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import jsproto.c2s.cclass.RewardInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 活动任务
 *
 * @author Huaxing
 */
@TableName(value = "taskConfig")
@Data
@NoArgsConstructor
public class TaskConfigBO extends BaseEntity<TaskConfigBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "taskType", comment = "任务类型")
    private int taskType;
    @DataBaseField(type = "varchar(255)", fieldname = "title", comment = "标题")
    private String title = "";
    @DataBaseField(type = "varchar(255)", fieldname = "content", comment = "内容")
    private String content = "";
    @DataBaseField(type = "varchar(300)", fieldname = "url", comment = "图标")
    private String url = "";
    @DataBaseField(type = "int(11)", fieldname = "targetValue", comment = "目标值")
    private int targetValue;
    @DataBaseField(type = "int(11)", fieldname = "targetType", comment = "目标类型")
    private int targetType;
    @DataBaseField(type = "bigint(20)", fieldname = "preTaskId", comment = "前置任务")
    private long preTaskId;
    @DataBaseField(type = "text", fieldname = "reward", comment = "奖励")
    private String reward = "";
    private List<RewardInfo> rewardInfo = new ArrayList<>();
    @DataBaseField(type = "int(2)", fieldname = "timeType", comment = "时间类型(1-4(1 每日   2 每周   3 具体时间  4 无))")
    private int timeType;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;

    public void saveTaskType(int taskType) {
        if (this.targetType == taskType) {
            return;
        }
        this.taskType = taskType;
        getBaseService().update("taskType", taskType, id, new AsyncInfo(id));
    }

    public void saveTitle(String title) {
        if (StringUtils.isEmpty(title)) {
            return;
        }
        if (title.equals(this.title)) {
            return;
        }
        this.title = title;
        getBaseService().update("title", title, id, new AsyncInfo(id));
    }

    public void saveContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (content.equals(this.content)) {
            return;
        }
        this.content = content;
        getBaseService().update("content", content, id, new AsyncInfo(id));
    }

    public void saveUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return;
        }
        if (url.equals(this.url)) {
            return;
        }
        this.url = url;
        getBaseService().update("url", url, id, new AsyncInfo(id));
    }

    public void saveTargetValue(int targetValue) {
        if (this.targetValue == targetValue) {
            return;
        }
        this.targetValue = targetValue;
        getBaseService().update("targetValue", targetValue, id, new AsyncInfo(id));
    }

    public void saveTargetType(int targetType) {
        if (this.targetType == targetType) {
            return;
        }
        this.targetType = targetType;
        getBaseService().update("targetType", targetType, id, new AsyncInfo(id));
    }

    public void savePreTaskId(long preTaskId) {
        if (preTaskId == this.preTaskId) {
            return;
        }
        this.preTaskId = preTaskId;
        getBaseService().update("preTaskId", preTaskId, id, new AsyncInfo(id));
    }

    public void saveReward(String reward) {
        if (StringUtils.isEmpty(reward)) {
            return;
        }
        if (reward.equals(this.reward)) {
            return;
        }
        this.reward = reward;
        getBaseService().update("reward", reward, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `taskConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`taskType` int(11) NOT NULL DEFAULT '0' COMMENT '任务类型',"
                + "`title` varchar(255) NOT NULL DEFAULT '' COMMENT '标题',"
                + "`content` varchar(255) NOT NULL DEFAULT '' COMMENT '内容',"
                + "`url` varchar(300) NOT NULL DEFAULT '0' COMMENT '图标',"
                + "`targetValue` int(11) NOT NULL DEFAULT '0' COMMENT '目标值',"
                + "`targetType` int(11) NOT NULL DEFAULT '0' COMMENT '目标类型',"
                + "`preTaskId` bigint(20) NOT NULL DEFAULT '0' COMMENT '前置任务',"
                + "`reward` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '奖励',"
                + "`timeType` int(2) NOT NULL DEFAULT '0' COMMENT '时间类型(1-4(1 每日   2 每周   3 具体时间  4 无))',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='活动任务'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
