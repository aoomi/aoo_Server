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
 * 中至禁止游戏列表
 * */
@TableName(value = "UnionBanCfgZhongZhi")
@Data
@NoArgsConstructor
public class UnionBanCfgZhongZhiBO extends BaseEntity<UnionBanCfgZhongZhiBO> {
	@DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key")
	private long id;
	@DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事id")
	private long unionId;
	@DataBaseField(type = "bigint(20)", fieldname = "playerID", comment = "玩家游戏短ID 长的")
	private long playerID;
	@DataBaseField(type = "int(11)", fieldname = "createTime", comment = "创建时间")
	private int createTime;
	@DataBaseField(type = "text", fieldname = "configIdList", comment = "禁止的游戏")
	private String configIdList ;//分组列表
	private List<Long> configIds = new ArrayList<>();



	public static String getSql_TableCreate() {
		String sql = "CREATE TABLE IF NOT EXISTS `UnionBanCfgZhongZhi` ("
				+ "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
				+ "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事id',"
				+ "`playerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家游戏长ID',"
				+ "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',"
				+ "`configIdList` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '禁止的游戏' ,"
				+ "PRIMARY KEY (`id`) ,"
				+ "KEY `unionId_playerID` (`unionId`,`playerID`) USING BTREE"
				+ ") COMMENT='中至禁止游戏列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (ServerConfig.getInitialID() + 1);
		return sql;
	}




	/**
	 * 获取分组列表
	 * @return
	 */
	public List<Long> getGroupingToList() {
		// 检查分组数据
		if (StringUtils.isEmpty(this.configIdList)) {
			return this.configIds;
		}
		// 检查分组列表
		if (this.configIds.size() <= 0) {
			this.configIds = new Gson().fromJson(this.configIdList, new TypeToken<List<Long>>() {}.getType());
		}
		return this.configIds;
	}

	/**
	 * 增加组
	 * @param pid
	 */
	public boolean addGrouping(long pid) {
		// 检查是否有存在数据
		if (Objects.isNull(configIds)) {
			this.configIds = Lists.newArrayList();
		}
		// 增加分组
		if (this.configIds.contains(pid)) {
			return false;
		} else {
			this.configIds.add(pid);
		}
		this.setConfigIdList(this.configIds.toString());
		getBaseService().update("configIdList",getConfigIdList(),id,new AsyncInfo(id));
		return true;
	}

	/**
	 * 移除组
	 * @param pid
	 */
	public boolean removeGrouping(long pid) {
		// 检查是否有存在数据
		if (CollectionUtils.isEmpty(configIds)) {
			return false;
		}
		// 移除分组
		this.configIds.remove(pid);
		this.setConfigIdList(this.configIds.toString());
		getBaseService().update("configIdList",getConfigIdList(),id,new AsyncInfo(id));
		return true;
	}

	public void del() {
		getBaseService().delete(this.getId(),"id",new AsyncInfo(id));
	}

}
