package core.db.entity.clarkGame;

import BaseCommon.CommLog;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@TableName(value = "luckDrawConfig")
@Data
@NoArgsConstructor
public class LuckDrawConfigBO extends BaseEntity<LuckDrawConfigBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(3)", fieldname = "condition", comment = "抽奖条件(1：房卡消费、2：局数、3：大赢家)")
    private int condition;
    @DataBaseField(type = "int(11)", fieldname = "conditionValue", comment = "抽奖条件值")
    private int conditionValue;
    @DataBaseField(type = "int(11)", fieldname = "luckDrawValue", comment = "抽奖次数")
    private int luckDrawValue;
    @DataBaseField(type = "int(11)", fieldname = "costFreeluckDrawValue", comment = "免费抽奖次数")
    private int costFreeluckDrawValue;
    @DataBaseField(type = "int(2)", fieldname = "dateType", comment = "日期类型：0:每日、1:每周、2:具体日期")
    private int dateType;
    @DataBaseField(type = "int(2)", fieldname = "timeSlot", comment = "时间段 0：全天、1:具体时间")
    private int timeSlot;
    @DataBaseField(type = "int(11)", fieldname = "startTime", comment = "开始时间")
    private int startTime;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "int(2)", fieldname = "assignCrowd", comment = "指定人群(0：所有人、1：代理、2：亲友圈)")
    private int assignCrowd;
    @DataBaseField(type = "text", fieldname = "assignCrowdValue", comment = "指定人群值")
    private String assignCrowdValue;
    private List<Long> assignCrowdValueList = Lists.newArrayList();

    private HashMap<String, Object> map = Maps.newHashMapWithExpectedSize(10);

    public void setAssignCrowdValue(String assignCrowdValue) {
        if (StringUtils.isEmpty(assignCrowdValue)) {
            return;
        }
        if (StringUtils.isEmpty(assignCrowdValue.trim())) {
            return;
        }
        CommLog.info("LuckDrawConfigBO setAssignCrowdValue:{} ", assignCrowdValue);
        assignCrowdValue = assignCrowdValue.trim();
        this.setAssignCrowdValueList(getAssignCrowdValueList(assignCrowdValue));
    }

    /**
     * 解析字符串游戏列表
     *
     * @param strList
     * @return
     */
    public List<Long> getAssignCrowdValueList(String strList) {
        if (StringUtils.isNotEmpty(strList)) {
            List<Long> list = Lists.newArrayList();
            // 解析游戏列表
            String[] ids = strList.split("-");
            for (String str : ids) {
                if (StringUtils.isNumeric(str.trim())) {
                    long valueId = Long.parseLong(str.trim());
                    if (valueId > 0L) {
                        list.add(valueId);
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }


    public void saveAssignCrowdValue(String assignCrowdValue) {
        if (StringUtils.isEmpty(assignCrowdValue)) {
            if (CollectionUtils.isEmpty(assignCrowdValueList)) {
                return;
            }
        }
        this.setAssignCrowdValue(assignCrowdValue);
        this.getMap().put("assignCrowdValue", assignCrowdValue);
    }

    public void saveCondition(int condition) {
        if (condition == this.condition) {
            return;
        }
        this.condition = condition;
        this.getMap().put("condition", condition);

    }

    public void saveConditionValue(int conditionValue) {
        if (conditionValue == this.conditionValue) {
            return;
        }
        this.conditionValue = conditionValue;
        this.getMap().put("conditionValue", conditionValue);

    }

    public void saveLuckDrawValue(int luckDrawValue) {
        if (this.luckDrawValue == luckDrawValue) {
            return;
        }
        this.luckDrawValue = luckDrawValue;
        this.getMap().put("luckDrawValue", luckDrawValue);

    }

    public void saveCostFreeluckDrawValue(int costFreeluckDrawValue) {
        if (this.costFreeluckDrawValue == costFreeluckDrawValue) {
            return;
        }
        this.costFreeluckDrawValue = costFreeluckDrawValue;
        this.getMap().put("costFreeluckDrawValue", costFreeluckDrawValue);

    }

    public void saveDateType(int dateType) {
        if (this.dateType == dateType) {
            return;
        }
        this.dateType = dateType;
        this.getMap().put("dateType", dateType);

    }

    public void saveTimeSlot(int timeSlot) {
        if (this.timeSlot == timeSlot) {
            return;
        }
        this.timeSlot = timeSlot;
        this.getMap().put("timeSlot", timeSlot);
    }

    public void saveStartTimeTime(int startTime) {
        if (this.startTime == startTime) {
            return;
        }
        this.startTime = startTime;
        this.getMap().put("startTime", startTime);

    }

    public void saveEndTime(int endTime) {
        if (this.endTime == endTime) {
            return;
        }
        this.endTime = endTime;
        this.getMap().put("endTime", endTime);
    }

    public void saveAssignCrowd(int assignCrowd) {
        if (this.assignCrowd == assignCrowd) {
            return;
        }
        this.assignCrowd = assignCrowd;
        this.getMap().put("assignCrowd", assignCrowd);
    }


    public void clear() {
        this.getMap().clear();
    }

    public void save() {
        getBaseService().update(this.getMap(), id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `luckDrawConfig` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`condition` int(3) NOT NULL DEFAULT '0' COMMENT '抽奖条件(1：房卡消费、2：局数、3：大赢家)',"
                + "`conditionValue` int(11) NOT NULL DEFAULT '0' COMMENT '抽奖条件值',"
                + "`luckDrawValue` int(11) NOT NULL DEFAULT '0' COMMENT '抽奖次数',"
                + "`costFreeluckDrawValue` int(11) NOT NULL DEFAULT '0' COMMENT '免费抽奖次数',"
                + "`dateType` int(11) NOT NULL DEFAULT '0' COMMENT '日期类型：0:每日、1:每周、2:具体日期',"
                + "`timeSlot` int(11) NOT NULL DEFAULT '0' COMMENT '时间段 0：全天、1:具体时间',"
                + "`startTime` int(11) NOT NULL DEFAULT '0' COMMENT '开始时间',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`assignCrowd` int(11) NOT NULL DEFAULT '0' COMMENT '指定人群(0：所有人、1：代理、2：亲友圈)',"
                + "`assignCrowdValue` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '指定人群值',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='抽奖配置表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
