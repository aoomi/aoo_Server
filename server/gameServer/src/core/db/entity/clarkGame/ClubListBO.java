package core.db.entity.clarkGame;

import business.global.shareclub.ShareClubListMgr;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.Config;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.google.gson.Gson;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import jsproto.c2s.cclass.club.ClubConfig;
import jsproto.c2s.cclass.club.ClubPromotionShowConfig;
import jsproto.c2s.cclass.club.Club_define.Club_DISSOLVE_SET;
import jsproto.c2s.cclass.club.Club_define.Club_DISSOLVE_TIME;
import jsproto.c2s.cclass.club.Club_define.Club_KICK_OUT;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

@TableName(value = "dbClubList")
@Data
@NoArgsConstructor
public class ClubListBO extends BaseEntity<ClubListBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "俱乐部ID 自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id; // 俱乐部ID
    @DataBaseField(type = "int(11)", fieldname = "clubsign", comment = "随机的俱乐部标识ID")
    private int clubsign;// 随机的俱乐部标识ID
    @DataBaseField(type = "varchar(255)", fieldname = "name", comment = "俱乐部名称")
    private String name = "";// 俱乐部名称
    @DataBaseField(type = "varchar(255)", fieldname = "minister", comment = "俱乐部管理员玩家ID(可多个,ID用逗号隔开)")
    private String minister = "";// 俱乐部管理员玩家ID(可多个,ID用逗号隔开)
    @DataBaseField(type = "int(4)", fieldname = "status", comment = "俱乐部状态 1为正常,2为已解散")
    private int status;// 1为正常,2为已解散
    @DataBaseField(type = "varchar(255)", fieldname = "notice", comment = "俱乐部公告")
    private String notice = "";// 俱乐部公告
    @DataBaseField(type = "varchar(255)", fieldname = "have_youxi", comment = "俱乐部可配游戏种类 用逗号隔开")
    private String have_youxi = "";// 俱乐部可配游戏种类
    @DataBaseField(type = "int(11)", fieldname = "maxplayernum", comment = "俱乐部玩家上限人数")
    private int maxplayernum;// 俱乐部玩家上限人数
    @DataBaseField(type = "int(11)", fieldname = "roomcard", comment = "俱乐部房卡数量")
    private int roomcard;// 俱乐部房卡数量
    @DataBaseField(type = "int(11)", fieldname = "maxplayeruse", comment = "俱乐部玩家每人每日最高消耗俱乐部房卡上限")
    private int maxplayeruse;// 俱乐部玩家每人每日最高消耗俱乐部房卡上限
    @DataBaseField(type = "int(11)", fieldname = "roomcardattention", comment = "俱乐部房卡提醒限度")
    private int roomcardattention;// 俱乐部房卡提醒限度
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;// 工会ID
    @DataBaseField(type = "int(11)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;// 代理等级
    @DataBaseField(type = "int(11)", fieldname = "creattime", comment = "俱乐部创建时间")
    private int creattime;// 创建时间
    @DataBaseField(type = "int(11)", fieldname = "distime", comment = "俱乐部解散时间")
    private int distime;// 解散时间
    @DataBaseField(type = "text", fieldname = "chatMsg", comment = "聊天信息")
    private String chatMsg = "";// 聊天信息
    @DataBaseField(type = "text", fieldname = "createGameSet", comment = "俱乐部创建房间一键设置")
    private String createGameSet = "";// 俱乐部创建房间一键设置
    @DataBaseField(type = "int(4)", fieldname = "autoRoomCreation", comment = "俱乐部自动创建房间  0:不自动创建 1:自动创建")
    private int autoRoomCreation;
    @DataBaseField(type = "int(4)", fieldname = "memberCreationRoom", comment = "俱乐部成员创建房间 0:成员不可以创建 1:成员可以创建")
    private int memberCreationRoom;
    @DataBaseField(type = "int(11)", fieldname = "lastIntoCard", comment = "最近转入房卡")
    private int lastIntoCard;
    @DataBaseField(type = "int(11)", fieldname = "lastOutCard", comment = "最近转出房卡")
    private int lastOutCard;
    @DataBaseField(type = "bigint(20)", fieldname = "ownerID", comment = "亲友圈创建者ID")
    private long ownerID;
    @DataBaseField(type = "text", fieldname = "clubConfig", comment = "亲友圈设置")
    private String clubConfig = "";// 亲友圈设置
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "int(2)", fieldname = "join", comment = "加入申请(0需要审核、1不需要审核)")
    private int join;
    @DataBaseField(type = "int(2)", fieldname = "quit", comment = "退出申请(0需要审核、1不需要审核)")
    private int quit;
    @DataBaseField(type = "int(11)", fieldname = "diamondsAttentionMinister", comment = "俱乐部管理员钻石提醒")
    private int diamondsAttentionMinister;
    @DataBaseField(type = "int(11)", fieldname = "diamondsAttentionAll", comment = "俱乐部全员钻石提醒")
    private int diamondsAttentionAll;
    @DataBaseField(type = "int(2)", fieldname = "showLostConnect", comment = "显示失去连接(0:仅管理员,1:所有人)")
    private int showLostConnect;
    @DataBaseField(type = "text", fieldname = "promotionShowConfig", comment = "推广员列表显示")
    private String promotionShowConfig = "";// 推广员列表显示
    @DataBaseField(type = "int(2)", fieldname = "skinType", comment = "皮肤类型")
    private int skinType = 0;
    @DataBaseField(type = "int(2)", fieldname = "showOnlinePlayerNum", comment = "查看在线人数(0:全部可见,1:推广员不可见)")
    private int showOnlinePlayerNum = 0;

    @DataBaseField(type = "int(2)", fieldname = "totalPointShowStatus", comment = "总积分显示状态 0不显示 1显示")
    private int totalPointShowStatus;

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `dbClubList` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`clubsign` int(11) NOT NULL DEFAULT '0' COMMENT '随机的俱乐部标识ID',"
                + "`name` varchar(255) NOT NULL DEFAULT ''  COMMENT '俱乐部名称',"
                + "`minister` varchar(255) NOT NULL DEFAULT ''  COMMENT '俱乐部管理员玩家ID(可多个长ID,ID用逗号隔开)(暂不使用)',"
                + "`status` int(4) NOT NULL DEFAULT '0' COMMENT '俱乐部状态 1为正常,2为已解散',"
                + "`notice` varchar(255) NOT NULL DEFAULT ''  COMMENT '俱乐部公告',"
                + "`have_youxi` varchar(255) NOT NULL DEFAULT ''  COMMENT '俱乐部可配游戏种类',"
                + "`maxplayernum` int(11) NOT NULL DEFAULT '0' COMMENT '俱乐部玩家上限人数',"
                + "`roomcard` int(11) NOT NULL DEFAULT '0' COMMENT '俱乐部房卡数量',"
                + "`maxplayeruse` int(11) NOT NULL DEFAULT '0' COMMENT '俱乐部玩家每人每日最高消耗俱乐部房卡上限',"
                + "`roomcardattention` int(11) NOT NULL DEFAULT '0' COMMENT '俱乐部房卡提醒限度',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0'  COMMENT '俱乐部代理ID',"
                + "`level` int(11) NOT NULL DEFAULT '0'  COMMENT '俱乐部代理等级',"
                + "`creattime` varchar(15) NOT NULL DEFAULT ''  COMMENT '俱乐部创建时间',"
                + "`distime` varchar(15) NOT NULL DEFAULT ''  COMMENT '俱乐部解散时间',"
                + "`chatMsg` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '聊天信息',"
                + "`createGameSet` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '俱乐部创建房间一键设置',"
                + "`autoRoomCreation` int(4) NOT NULL DEFAULT '0' COMMENT '俱乐部自动创建房间  0:不自动创建 1:自动创建',"
                + "`memberCreationRoom` int(4) NOT NULL DEFAULT '0' COMMENT '俱乐部成员创建房间 0:成员不可以创建 1:成员可以创建',"
                + "`lastIntoCard` int(11) NOT NULL DEFAULT '0' COMMENT '最近转入房卡',"
                + "`lastOutCard` int(11) NOT NULL DEFAULT '0' COMMENT '最近转出房卡',"
                + "`ownerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈创建者ID',"
                + "`clubConfig` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '亲友圈设置',"
                + "`fatigueConfig` varchar(500) NOT NULL DEFAULT ''  COMMENT '疲劳系统设置',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`join` int(2) NOT NULL DEFAULT '0' COMMENT '加入申请(0需要审核、1不需要审核)',"
                + "`quit` int(2) NOT NULL DEFAULT '0' COMMENT '退出申请(0需要审核、1不需要审核)',"
                + "`diamondsAttentionMinister` int(11) NOT NULL DEFAULT '500' COMMENT '俱乐部管理员钻石提醒',"
                + "`diamondsAttentionAll` int(11) NOT NULL DEFAULT '100' COMMENT '俱乐部全员钻石提醒',"
                + "`showLostConnect` int(2) NOT NULL DEFAULT '0' COMMENT '显示失去连接(0:仅管理员,1:所有人)',"
                + "`totalPointShowStatus` int(2) NOT NULL DEFAULT '0' COMMENT '总积分显示状态 0不显示 1显示',"
                + "`promotionShowConfig` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '推广员列表显示',"
                + "`skinType` int(2) NOT NULL DEFAULT '0' COMMENT '皮肤类型)',"
                + "`showOnlinePlayerNum` int(2) NOT NULL DEFAULT '0' COMMENT '查看在线人数(0:全部可见,1:推广员不可见)',"
                + "PRIMARY KEY (`id`),"
                + "KEY `als` (`agentsID`,`level`,`status`) USING BTREE"
                + ") COMMENT='亲友圈表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }


    public void setName(String name) {
        this.name = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(name);
    }

    public void setClubName(String name) {
        this.name = name;
    }

    public void saveStatus(int status) {
        if (status == this.status) {
            return;
        }
        this.status = status;
        getBaseService().update("status", status, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "status");
        }
    }

    /**
     * 保存查看在线人数
     *
     * @param showOnlinePlayerNum
     */
    public void saveShowOnlinePlayerNum(int showOnlinePlayerNum) {
        if (this.showOnlinePlayerNum == showOnlinePlayerNum) {
            return;
        }
        this.showOnlinePlayerNum = showOnlinePlayerNum;
        getBaseService().update("showOnlinePlayerNum", this.showOnlinePlayerNum, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "showOnlinePlayerNum");
        }
    }

    /**
     * 保存皮肤
     *
     * @param skinType
     */
    public void saveSkin(int skinType) {
        if (this.skinType == skinType) {
            return;
        }
        this.skinType = skinType;
        getBaseService().update("skinType", this.skinType, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "skinType");
        }
    }

    public void saveRoomcard(int roomcard) {
        if (this.roomcard == roomcard) {
            return;
        }
        this.roomcard = roomcard;
        getBaseService().update("roomcard", roomcard, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "roomcard");
        }
    }

    public void saveRoomcardattention(int roomcardattention) {
        if (this.roomcardattention == roomcardattention) {
            return;
        }
        this.roomcardattention = roomcardattention;
        getBaseService().update("roomcardattention", roomcardattention, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "roomcardattention");
        }
    }

    public void saveDiamondsAttentionMinister(int diamondsAttentionMinister) {
        if (this.diamondsAttentionMinister == diamondsAttentionMinister) {
            return;
        }
        this.diamondsAttentionMinister = diamondsAttentionMinister;
        getBaseService().update("diamondsAttentionMinister", diamondsAttentionMinister, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "diamondsAttentionMinister");
        }
    }

    public void saveDiamondsAttentionAll(int diamondsAttentionAll) {
        if (this.diamondsAttentionAll == diamondsAttentionAll) {
            return;
        }
        this.diamondsAttentionAll = diamondsAttentionAll;
        getBaseService().update("diamondsAttentionAll", diamondsAttentionAll, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "diamondsAttentionAll");
        }
    }

    public void saveCreateGameSet(String createGameSet) {
        if (this.createGameSet.equals(createGameSet)) {
            return;
        }
        this.createGameSet = createGameSet;
        getBaseService().update("createGameSet", createGameSet, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "createGameSet");
        }
    }

    public void saveSyncCreateGameSet(String createGameSet) {
        if (this.createGameSet.equals(createGameSet)) {
            return;
        }
        this.createGameSet = createGameSet;
        getBaseService().update("createGameSet", createGameSet, id);
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "createGameSet");
        }
    }

    public void saveAutoRoomCreation(int autoRoomCreation) {
        if (this.autoRoomCreation == autoRoomCreation) {
            return;
        }
        this.autoRoomCreation = autoRoomCreation;
        getBaseService().update("autoRoomCreation", autoRoomCreation, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "autoRoomCreation");
        }
    }

    public void saveMemberCreationRoom(int memberCreationRoom) {
        if (this.memberCreationRoom == memberCreationRoom) {
            return;
        }
        this.memberCreationRoom = memberCreationRoom;
        getBaseService().update("memberCreationRoom", memberCreationRoom, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "memberCreationRoom");
        }
    }

    public void saveLastIntoCard(int lastIntoCard) {
        if (this.lastIntoCard == lastIntoCard) {
            return;
        }
        this.lastIntoCard = lastIntoCard;
        getBaseService().update("lastIntoCard", lastIntoCard, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "lastIntoCard");
        }
    }

    public void saveLastoutCard(int lastOutCard) {
        if (this.lastOutCard == lastOutCard) {
            return;
        }
        this.lastOutCard = lastOutCard;
        getBaseService().update("lastoutCard", lastOutCard, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "lastoutCard");
        }
    }

    public void saveOwnerID(long ownerID) {
        if (this.ownerID == ownerID) {
            return;
        }
        this.ownerID = ownerID;
        getBaseService().update("ownerID", ownerID, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "ownerID");
        }
    }

    public void saveAgentsID(long agentsID) {
        if (this.agentsID == agentsID) {
            return;
        }
        this.agentsID = agentsID;
        getBaseService().update("agentsID", agentsID, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "agentsID");
        }
    }


    public void saveClubConfig(ClubConfig config) {
        if (null == config) {
            return;
        }
        String clubConfig = new Gson().toJson(config);
        if (StringUtils.isEmpty(clubConfig)) {
            return;
        }
        if (clubConfig.equals(this.clubConfig)) {
            return;
        }
        this.clubConfig = clubConfig;
        getBaseService().update("clubConfig", clubConfig, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "clubConfig");
        }
    }

    public ClubConfig getClubConfigJson() {
        if (StringUtils.isEmpty(this.clubConfig)) {
            this.saveClubConfig(new ClubConfig(Arrays.asList(0, 1), Club_KICK_OUT.NOT_LIMIT.ordinal(),
                    Club_DISSOLVE_SET.HALF.ordinal(), Club_DISSOLVE_TIME.T2M.ordinal()));
        }
        return new Gson().fromJson(this.clubConfig, ClubConfig.class);
    }

    public void savePromotionShowConfig(ClubPromotionShowConfig config) {
        if (null == config) {
            return;
        }
        String promotionShowConfig = new Gson().toJson(config);
        if (StringUtils.isEmpty(promotionShowConfig)) {
            return;
        }
        if (promotionShowConfig.equals(this.promotionShowConfig)) {
            return;
        }
        this.promotionShowConfig = promotionShowConfig;
        getBaseService().update("promotionShowConfig", promotionShowConfig, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "promotionShowConfig");
        }
    }

    public ClubPromotionShowConfig getPromotionShowClubConfigJson() {
        if (StringUtils.isEmpty(this.promotionShowConfig)) {
            this.savePromotionShowConfig(new ClubPromotionShowConfig(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8), Arrays.asList(0, 1, 2)));
        }
        ClubPromotionShowConfig clubPromotionShowConfig = new Gson().fromJson(this.promotionShowConfig, ClubPromotionShowConfig.class);
        if (Objects.isNull(clubPromotionShowConfig.getShowConfigSecond())) {
            clubPromotionShowConfig.setShowConfigSecond(Arrays.asList(0, 1, 2));
            this.savePromotionShowConfig(clubPromotionShowConfig);
        }
        return clubPromotionShowConfig;
    }

    public void saveUnionId(long unionId) {
        if (this.unionId > 0L && unionId > 0L) {
            return;
        }
        if (this.unionId == unionId) {
            return;
        }
        this.unionId = unionId;
        getBaseService().update("unionId", unionId, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "unionId");
        }
    }

    public void saveJoin(int join) {
        if (this.join == join) {
            return;
        }
        this.join = join;
        getBaseService().update("join", join, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "join");
        }
    }

    public void saveQuit(int quit) {
        if (this.quit == quit) {
            return;
        }
        this.quit = quit;
        getBaseService().update("quit", quit, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "quit");
        }
    }

    public void saveShowLostConnect(int showLostConnect) {
        if (this.showLostConnect == showLostConnect) {
            return;
        }
        this.showLostConnect = showLostConnect;
        getBaseService().update("showLostConnect", showLostConnect, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "showLostConnect");
        }
    }

    public void saveTotalPointShowStatus(int totalPointShowStatus) {
        if (this.totalPointShowStatus == totalPointShowStatus) {
            return;
        }
        this.totalPointShowStatus = totalPointShowStatus;
        getBaseService().update("totalPointShowStatus", totalPointShowStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "totalPointShowStatus");
        }
    }

    /**
     * 关闭、解散亲友圈状态
     */
    public void closeClub() {
        HashMap<String, Object> map = new HashMap<>(3);
        map.put("distime", this.getDistime());
        map.put("createGameSet", this.getCreateGameSet());
        map.put("status", this.getStatus());
        this.getBaseService().update(map, getId());
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateClubListField(this, "distime", "createGameSet", "status");
        }
    }
}
