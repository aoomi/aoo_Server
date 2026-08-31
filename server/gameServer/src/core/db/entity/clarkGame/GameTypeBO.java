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
 * 游戏列表
 *
 * @author Administrator
 */
@TableName(value = "gameType")
@Data
@NoArgsConstructor
public class GameTypeBO extends BaseEntity<GameTypeBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "gametype", comment = "游戏类型")
    private int gametype;
    @DataBaseField(type = "varchar(20)", fieldname = "name", comment = "游戏名称")
    private String name = "";
    @DataBaseField(type = "varchar(200)", fieldname = "logoico", comment = "图标")
    private String logoico = "";
    @DataBaseField(type = "varchar(255)", fieldname = "barColors", comment = "首页柱状图背景色")
    private String barColors = "";
    @DataBaseField(type = "varchar(20)", fieldname = "gameName", comment = "游戏名")
    private String gameName = "";
    @DataBaseField(type = "int(2)", fieldname = "have_xifen", comment = "1:有细分游戏,0没有")
    private int have_xifen;
    @DataBaseField(type = "int(2)", fieldname = "tab", comment = "0:不显示,1:默认显示,2:禁用")
    private int tab;
    @DataBaseField(type = "varchar(255)", fieldname = "hutypelist", comment = "胡类型列表")
    private String hutypelist = "";
    @DataBaseField(type = "int(11)", fieldname = "sort", comment = "排序")
    private int sort;
    @DataBaseField(type = "int(1)", fieldname = "classType", comment = "房间种类(1、麻将，2、扑克)")
    private int classType;
    @DataBaseField(type = "varchar(255)", fieldname = "webSocketUrl", comment = "websocket地址")
    private String webSocketUrl;
    @DataBaseField(type = "varchar(255)", fieldname = "httpUrl", comment = "http地址")
    private String httpUrl;
    @DataBaseField(type = "varchar(255)", fieldname = "gameServerIP", comment = "服务器ip")
    private String gameServerIP;
    @DataBaseField(type = "int(1)", fieldname = "gameServerPort", comment = "服务器端口")
    private int gameServerPort;
    @DataBaseField(type = "int(2)", fieldname = "openType", comment = "开放类型(0:所有人,1:指定城市,2:指定赛事)")
    private int openType;
    @DataBaseField(type = "text", fieldname = "openContent", comment = "开放内容:[100,114,111]")
    private String openContent;
    private List<Long> openContentList = null;


    public void saveGametype(int gametype) {
        if (this.gametype == gametype) {
            return;
        }
        this.gametype = gametype;
        getBaseService().update("gametype", gametype, id, new AsyncInfo(id));
    }

    public void saveName(String name) {
        if (name.equals(this.name)) {
            return;
        }
        this.name = name;
        getBaseService().update("name", name, id, new AsyncInfo(id));
    }

    public void saveLogoico(String logoico) {
        if (logoico.equals(this.logoico)) {
            return;
        }
        this.logoico = logoico;
        getBaseService().update("logoico", logoico, id, new AsyncInfo(id));
    }

    public void saveBarColors(String barColors) {
        if (barColors.equals(this.barColors)) {
            return;
        }
        this.barColors = barColors;
        getBaseService().update("barColors", barColors, id, new AsyncInfo(id));
    }

    public void saveGameName(String gameName) {
        if (gameName.equals(this.gameName)) {
            return;
        }
        this.gameName = gameName;
        getBaseService().update("gameName", gameName, id, new AsyncInfo(id));
    }

    public void saveHave_xifen(int have_xifen) {
        if (have_xifen == this.have_xifen) {
            return;
        }
        this.have_xifen = have_xifen;
        getBaseService().update("have_xifen", have_xifen, id, new AsyncInfo(id));
    }

    public void saveSign(int sign) {
        if (this.tab == sign) {
            return;
        }
        this.tab = sign;
        getBaseService().update("sign", sign, id, new AsyncInfo(id));
    }

    public void saveHutypelist(String hutypelist) {
        if (StringUtils.isEmpty(hutypelist)) {
            return;
        }
        if (hutypelist.equals(this.hutypelist)) {
            return;
        }
        this.hutypelist = hutypelist;
        getBaseService().update("hutypelist", hutypelist, id);
    }

    public void saveSort(int sort) {
        if (this.sort == sort) {
            return;
        }
        this.sort = sort;
        getBaseService().update("sort", sort, id);
    }


    public void saveTab(int tab) {
        if (this.tab == tab) {
            return;
        }
        this.tab = tab;
        getBaseService().update("tab", tab, id);
    }

    public void saveGameServerPort(int gameServerPort) {
        if (this.gameServerPort == gameServerPort) {
            return;
        }
        this.gameServerPort = gameServerPort;
        getBaseService().update("gameServerPort", gameServerPort, id);
    }

    public boolean openTypeAndContent(int tab, int openType, List<Long> openContent) {
        HashMap<String, Object> map = new HashMap<>();
        if (this.tab != tab) {
            this.tab = tab;
            map.put("tab", tab);
        }
        if (this.openType != openType) {
            this.openType = openType;
            map.put("openType", openType);
        }
        if (Objects.nonNull(openContent)) {
            this.openContent = new Gson().toJson(openContent);
            this.openContentList = openContent;
            map.put("openContent", this.openContent);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id);
        }
        return true;
    }

    public List<Long> getOpenContentToList() {
        try {
            if (Objects.nonNull(this.openContentList)) {
                // 指定开放内容列表
                return this.openContentList;
            }
            this.openContentList = StringUtils.isNotEmpty(this.openContent) ? new Gson().fromJson(this.openContent, new TypeToken<List<Long>>() {
            }.getType()) : Collections.emptyList();
            return this.openContentList;
        } catch (Exception e) {
            CommLogD.error("OpenContent error:{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `gameType` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`gametype` int(11) NOT NULL DEFAULT '0' COMMENT '游戏类型',"
                + "`name` varchar(20) NOT NULL DEFAULT '0' COMMENT '游戏名称',"
                + "`logoico` varchar(200) NOT NULL DEFAULT '0' COMMENT '图标',"
                + "`barColors` varchar(255) NOT NULL DEFAULT '0' COMMENT '首页柱状图背景色',"
                + "`gameName` varchar(20) NOT NULL DEFAULT '0' COMMENT '游戏名',"
                + "`have_xifen` int(2) NOT NULL DEFAULT '0' COMMENT '1:有细分游戏,0没有',"
                + "`tab` int(2) NOT NULL DEFAULT '0' COMMENT '0:不显示,1:默认显示,2:禁用',"
                + "`hutypelist` varchar(255) NOT NULL DEFAULT '0' COMMENT '胡类型列表',"
                + "`sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',"
                + "`classType` int(2) NOT NULL DEFAULT '0' COMMENT '房间种类(1、麻将，2、扑克)',"
                + "`webSocketUrl` varchar(255) NULL COMMENT 'websocket地址',"
                + "`httpUrl` varchar(255) NULL COMMENT 'http地址',"
                + "`gameServerIP` varchar(255) NULL COMMENT '服务器ip',"
                + "`gameServerPort` int(5) NULL COMMENT '服务器端口',"
                + "`openType` int(2) NULL COMMENT '开放类型(0:所有人,1:指定城市,2:指定赛事)',"
                + "`openContent` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '开放内容:[100,114,111]',"
                + "PRIMARY KEY (`id`)"
                + ") COMMENT='游戏列表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
