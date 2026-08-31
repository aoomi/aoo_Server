package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.CommLogD;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 游戏对应节点
 * @author Administrator
 *
 */
@TableName(value = "gameNode")
@Data
@NoArgsConstructor
public class GameNodeBO extends BaseEntity<GameNodeBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key",indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "varchar(30)", fieldname = "nodeId", comment = "节点Id")
    private String nodeId;
    @DataBaseField(type = "int(6)", fieldname = "gameId", comment = "游戏Id")
    private int gameId;
    @DataBaseField(type = "int(2)", fieldname = "state", comment = "状态0:正常,1:异常")
    private int state;


	public GameNodeBO(String nodeId, int gameId) {
		this.nodeId = nodeId;
		this.gameId = gameId;
	}

	public GameNodeBO(String nodeId, int gameId, int state) {
		this.nodeId = nodeId;
		this.gameId = gameId;
		this.state = state;
	}

	public void saveNodeId(String nodeId) {
		if (nodeId.equals(this.nodeId)) {
			return;
		}
		this.nodeId = nodeId;
		getBaseService().update("nodeId", nodeId,id,new AsyncInfo(id));
	}

	public void saveGameId(int gameId) {
		if (this.gameId == gameId) {
			return;
		}
		this.gameId = gameId;
		getBaseService().update("gameId", gameId,id,new AsyncInfo(id));
	}

	public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `gameNode` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`nodeId` varchar(30) NOT NULL DEFAULT '' COMMENT '节点Id',"
                + "`gameId` int(6) NOT NULL DEFAULT '0' COMMENT '游戏Id',"
                + "`state` int(2) NOT NULL DEFAULT '0' COMMENT '状态0:正常,1:异常',"
				+ "PRIMARY KEY (`id`),"
				+ "KEY `gameId` (`gameId`),"
				+ "UNIQUE KEY `nodeId_gameId` (`nodeId`,`gameId`)"
		+ ") COMMENT='游戏对应节点'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
