package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.server.ServerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 亲友圈分组
 */
@TableName(value = "UnionGrouping")
@Data
@NoArgsConstructor
public class UnionGroupingBO extends BaseEntity<UnionGroupingBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事id")
    private long unionId;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
    private int createTime;
    @DataBaseField(type = "text", fieldname = "grouping", comment = "分组")
    private String grouping;//分组列表
    private List<Long> groupingList = new ArrayList<>();


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `UnionGrouping` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事id',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
                + "`grouping` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '分组' ,"
                + "PRIMARY KEY (`id`) ,"
                + "KEY `unionId` (`unionId`) USING BTREE"
                + ") COMMENT='赛事分组'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
        return sql;
    }


    /**
     * 获取分组列表
     *
     * @return
     */
    public List<Long> getGroupingToList() {
        // 检查分组数据
        if (StringUtils.isEmpty(this.grouping)) {
            return this.groupingList;
        }
        // 检查分组列表
        if (this.groupingList.size() <= 0) {
            this.groupingList = new Gson().fromJson(this.grouping, new TypeToken<List<Long>>() {
            }.getType());
        }
        return this.groupingList;
    }

    /**
     * 增加组
     *
     * @param pid
     */
    public boolean addGrouping(long pid) {
        // 检查是否有存在数据
        if (Objects.isNull(groupingList)) {
            this.groupingList = Lists.newArrayList();
        }
        // 增加分组
        if (this.groupingList.contains(pid)) {
            return false;
        } else {
            this.groupingList.add(pid);
        }
        this.setGrouping(this.groupingList.toString());
        getBaseService().update("grouping", getGrouping(), id, new AsyncInfo(id));
        return true;
    }

    /**
     * 移除组
     *
     * @param pid
     */
    public boolean removeGrouping(long pid) {
        // 检查是否有存在数据
        if (CollectionUtils.isEmpty(groupingList)) {
            return false;
        }
        // 移除分组
        this.groupingList.remove(pid);
        this.setGrouping(this.groupingList.toString());
        getBaseService().update("grouping", getGrouping(), id, new AsyncInfo(id));
        return true;
    }

    public void del() {
        getBaseService().delete(this.getId(), "id", new AsyncInfo(id));
    }

}
