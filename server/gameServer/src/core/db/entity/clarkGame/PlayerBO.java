package core.db.entity.clarkGame;

import business.shareplayer.SharePlayerMgr;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.Config;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@TableName("player")
@Data
@NoArgsConstructor
public class PlayerBO extends BaseEntity<PlayerBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "accountID", comment = "玩家账号ID")
    private long accountID;
    @DataBaseField(type = "int(11)", fieldname = "sid", comment = "服务器id")
    private int sid;
    @DataBaseField(type = "varchar(50)", fieldname = "name", comment = "玩家名称/昵称")
    private String name = "";
    @DataBaseField(type = "int(11)", fieldname = "icon", comment = "头像(0:玩家，1:游客)")
    private int icon;
    @DataBaseField(type = "int(11)", fieldname = "sex", comment = "性别 0：男，1：女")
    private int sex;
    @DataBaseField(type = "varchar(300)", fieldname = "headImageUrl", comment = "头像url地址")
    private String headImageUrl = "";
    @DataBaseField(type = "bigint(20)", fieldname = "createTime", comment = "账号创建时间毫秒")
    private long createTime;
    @DataBaseField(type = "int(11)", fieldname = "lv", comment = "等级(连续登陆天数)")
    private int lv;
    @DataBaseField(type = "int(11)", fieldname = "gmLevel", comment = "gm权限")
    private int gmLevel;
    @DataBaseField(type = "int(11)", fieldname = "vipLevel", comment = "vip等级(是否试玩用户)")
    private int vipLevel;
    @DataBaseField(type = "int(11)", fieldname = "vipExp", comment = "vip经验(活跃次数(周))")
    private int vipExp;
    @DataBaseField(type = "int(11)", fieldname = "totalRecharge", comment = "累计充值, 统计玩家充值RMB")
    private int totalRecharge;
    @DataBaseField(type = "int(11)", fieldname = "crystal", comment = "钻石(兑换奖品积分)")
    private int crystal;
    @DataBaseField(type = "int(11)", fieldname = "gold", comment = "游戏币")
    private int gold;
    @DataBaseField(type = "int(11)", fieldname = "roomCard", comment = "房卡数量")
    private int roomCard;
    @DataBaseField(type = "int(11)", fieldname = "fastCard", comment = "闪电卷")
    private int fastCard;
    @DataBaseField(type = "int(11)", fieldname = "cheatTimes", comment = "作弊次数(每天首抽奖)")
    private int cheatTimes;
    @DataBaseField(type = "int(11)", fieldname = "banned_ChatExpiredTime", comment = "禁言过期时间(初始活跃记录时间)")
    private int bannedChatExpiredTime;
    @DataBaseField(type = "int(11)", fieldname = "banned_LoginExpiredTime", comment = "禁登过期时间")
    private int bannedLoginExpiredTime;
    @DataBaseField(type = "int(11)", fieldname = "bannedTimes", comment = "封号次数(连续登陆奖励领取记录)")
    private int bannedTimes;
    @DataBaseField(type = "int(11)", fieldname = "lastLogin", comment = "最近一次登陆時間")
    private int lastLogin;
    @DataBaseField(type = "int(11)", fieldname = "lastLogout", comment = "最近一次登出時間")
    private int lastLogout;
    @DataBaseField(type = "int(11)", fieldname = "currentGameType", comment = "当前的游戏类型")
    private int currentGameType = -1;
    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "工会ID")
    private long familyID = 10001;
    @DataBaseField(type = "varchar(12)", fieldname = "realName", comment = "真实名字")
    private String realName = "";
    @DataBaseField(type = "varchar(50)", fieldname = "realNumber", comment = "真实号码")
    private String realNumber = "";
    @DataBaseField(type = "int(11)", fieldname = "real_referer", comment = "直接推荐人")
    private int realReferer;
    @DataBaseField(type = "varchar(255)", fieldname = "wx_unionid", comment = "微信ID")
    private String wx_unionid = "";
    @DataBaseField(type = "varchar(500)", fieldname = "game_list", comment = "玩家游戏列表")
    private String gameList = "";
    @DataBaseField(type = "bigint(20)", fieldname = "sendClubReward", comment = "俱乐部奖励是否赠送0:没有赠送,否则传入俱乐部ID")
    private long sendClubReward;
    @DataBaseField(type = "int(11)", fieldname = "clubTotalRecharge", comment = "亲友圈累计充值, 统计玩家充值RMB")
    private int clubTotalRecharge;
    @DataBaseField(type = "int(11)", fieldname = "mjPoint", comment = "麻将分数")
    private int mjPoint;
    @DataBaseField(type = "int(11)", fieldname = "pkPoint", comment = "扑克分数")
    private int pkPoint;
    @DataBaseField(type = "bigint(20)", fieldname = "phone", comment = "电话号码")
    private long phone;
    @DataBaseField(type = "int(2)", fieldname = "os", comment = "app操作系统类型(0:未知或者网页,1:android,2:ios)")
    private int os;


    public void saveAccountID(long accountID) {
        if (accountID == this.accountID) {
            return;
        }
        this.accountID = accountID;
        getBaseService().update("accountID", accountID, id, new AsyncInfo(id));
    }

    public void saveSid(int sid) {
        if (sid == this.sid) {
            return;
        }
        this.sid = sid;
        getBaseService().update("sid", sid, id, new AsyncInfo(id));
    }

    public void saveName(String name) {
        if (name.equals(this.name)) {
            return;
        }
        this.name = name;
        getBaseService().update("name", name, id, new AsyncInfo(id));
    }

    public void saveIcon(int icon) {
        if (icon == this.icon) {
            return;
        }
        this.icon = icon;
        getBaseService().update("icon", icon, id, new AsyncInfo(id));
    }

    public void saveSex(int sex) {
        if (sex == this.sex) {
            return;
        }
        this.sex = sex;
        getBaseService().update("sex", sex, id, new AsyncInfo(id));
    }

    public void saveHeadImageUrl(String headImageUrl) {
        if (headImageUrl.equals(this.headImageUrl)) {
            return;
        }
        this.headImageUrl = headImageUrl;
        getBaseService().update("headImageUrl", headImageUrl, id, new AsyncInfo(id));
    }

    public void saveCreateTime(long createTime) {
        if (createTime == this.createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    public void saveLv(int lv) {
        if (lv == this.lv) {
            return;
        }
        this.lv = lv;
        getBaseService().update("lv", lv, id, new AsyncInfo(id));
    }

    public void saveGmLevel(int gmLevel) {
        if (gmLevel == this.gmLevel) {
            return;
        }
        this.gmLevel = gmLevel;
        getBaseService().update("gmLevel", gmLevel, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "gmLevel");
        }
    }

    public void saveVipLevel(int vipLevel) {
        if (vipLevel == this.vipLevel) {
            return;
        }
        this.vipLevel = vipLevel;
        getBaseService().update("vipLevel", vipLevel, id, new AsyncInfo(id));
    }

    public void saveVipExp(int vipExp) {
        if (vipExp == this.vipExp) {
            return;
        }
        this.vipExp = vipExp;
        getBaseService().update("vipExp", vipExp, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "vipExp");
        }
    }

    public void saveTotalRecharge(int totalRecharge) {
        if (totalRecharge == this.totalRecharge) {
            return;
        }
        this.totalRecharge = totalRecharge;
        getBaseService().update("totalRecharge", totalRecharge, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "totalRecharge");
        }
    }

    public void saveCrystal(int crystal) {
        if (crystal == this.crystal) {
            return;
        }
        this.crystal = crystal;
        getBaseService().update("crystal", crystal, id, new AsyncInfo(id));
    }

    public void saveGold(int gold) {
        if (gold == this.gold) {
            return;
        }
        this.gold = gold;
        getBaseService().update("gold", gold, id, new AsyncInfo(id));
        //更新缓存金币
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "gold");
        }
    }

    public void saveRoomCard(int roomCard) {
        if (roomCard == this.roomCard) {
            return;
        }
        this.roomCard = roomCard;
        getBaseService().update("roomCard", roomCard, id, new AsyncInfo(id));
        //更新缓存房卡
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "roomCard");
        }
    }

    public void saveFastCard(int fastCard) {
        if (fastCard == this.fastCard) {
            return;
        }
        this.fastCard = fastCard;
        getBaseService().update("fastCard", fastCard, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "fastCard");
        }
    }

    public void saveCheatTimes(int cheatTimes) {
        if (cheatTimes == this.cheatTimes) {
            return;
        }
        this.cheatTimes = cheatTimes;
        getBaseService().update("cheatTimes", cheatTimes, id, new AsyncInfo(id));
    }

    public void saveBannedChatExpiredTime(int banned_ChatExpiredTime) {
        if (banned_ChatExpiredTime == this.bannedChatExpiredTime) {
            return;
        }
        this.bannedChatExpiredTime = banned_ChatExpiredTime;
        getBaseService().update("banned_ChatExpiredTime", banned_ChatExpiredTime, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "bannedChatExpiredTime");
        }
    }

    public void saveBannedLoginExpiredTime(int banned_LoginExpiredTime) {
        if (banned_LoginExpiredTime == this.bannedLoginExpiredTime) {
            return;
        }
        this.bannedLoginExpiredTime = banned_LoginExpiredTime;
        getBaseService().update("banned_LoginExpiredTime", banned_LoginExpiredTime, id, new AsyncInfo(id));
    }

    public void saveBannedTimes(int bannedTimes) {
        if (bannedTimes == this.bannedTimes) {
            return;
        }
        this.bannedTimes = bannedTimes;
        getBaseService().update("bannedTimes", bannedTimes, id, new AsyncInfo(id));
    }

    public void saveLastLogin() {
        getBaseService().update("lastLogin", this.lastLogin, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "lastLogin");
        }
    }

    public void saveLastLoginSync(int lastLogin) {
        if (lastLogin == this.lastLogin) {
            return;
        }
        this.lastLogin = lastLogin;
        getBaseService().update("lastLogin", lastLogin, id);
    }

    public void saveLastLogout(int lastLogout) {
        if (lastLogout == this.lastLogout) {
            return;
        }
        this.lastLogout = lastLogout;
        getBaseService().update("lastLogout", lastLogout, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "lastLogout");
        }
    }

    public void savecurrentGameType(int currentGameType) {
        if (currentGameType == this.currentGameType) {
            return;
        }
        this.currentGameType = currentGameType;
//        getBaseService().update("currentGameType", currentGameType,id,new AsyncInfo(id));
    }

    public void saveFamilyID(long familyID) {
        if (familyID == this.familyID) {
            return;
        }
        this.familyID = familyID;
        getBaseService().update("familyID", familyID, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "familyID");
        }
    }

    public void saveFamilyID_sync(long familyID) {
        if (familyID == this.familyID) {
            return;
        }
        this.familyID = familyID;
        getBaseService().update("familyID", familyID, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "familyID");
        }
    }

    public void saveRealName(String realName) {
        if (StringUtils.isEmpty(realName)) {
            return;
        }
        if (realName.equals(this.realName)) {
            return;
        }
        this.realName = realName;
        getBaseService().update("realName", realName, id, new AsyncInfo(id));
    }

    public void saveRealNumber(String realNumber) {
        if (StringUtils.isEmpty(realNumber)) {
            return;
        }
        if (realNumber.equals(this.realNumber)) {
            return;
        }
        this.realNumber = realNumber;
        getBaseService().update("realNumber", realNumber, id, new AsyncInfo(id));
    }

    public void saveReal_referer(int real_referer) {
        if (this.realReferer == real_referer) {
            return;
        }
        this.realReferer = real_referer;
        getBaseService().update("real_referer", real_referer, id, new AsyncInfo(id));
    }

    public void saveWx_unionid(String wx_unionid) {
        if (StringUtils.isEmpty(wx_unionid)) {
            return;
        }
        if (wx_unionid.equals(this.wx_unionid)) {
            return;
        }
        this.wx_unionid = wx_unionid;
        getBaseService().update("wx_unionid", wx_unionid, id, new AsyncInfo(id));
    }

    public void saveGameList(String game_list) {
        if (this.gameList.equals(game_list)) {
            return;
        }
        this.gameList = game_list;
        getBaseService().update("game_list", game_list, id, new AsyncInfo(id));
    }

    public void saveClubTotalRecharge(int clubTotalRecharge) {
        if (clubTotalRecharge == this.clubTotalRecharge) {
            return;
        }
        this.clubTotalRecharge = clubTotalRecharge;
        getBaseService().update("clubTotalRecharge", clubTotalRecharge, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "clubTotalRecharge");
        }
    }

    public void saveSendClubReward(long sendClubReward) {
        if (this.sendClubReward == sendClubReward) {
            return;
        }
        this.sendClubReward = sendClubReward;
        getBaseService().update("sendClubReward", sendClubReward, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "sendClubReward");
        }
    }

    public void setWxUnionid(String wxUnionid) {
        if (StringUtils.isEmpty(wxUnionid)) {
            return;
        }
        if (wxUnionid.equals(this.wx_unionid)) {
            return;
        }
        this.wx_unionid = wxUnionid;
    }

    public void savePhone(long phone) {
        if (this.phone == phone) {
            return;
        }
        this.phone = phone;
        getBaseService().update("phone", phone, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "phone");
        }
    }

    public void saveMjPoint(int mjPoint) {
        if (this.mjPoint == mjPoint) {
            return;
        }
        this.mjPoint = mjPoint;
        getBaseService().update("mjPoint", mjPoint, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "mjPoint");
        }
    }

    public void savePkPoint(int pkPoint) {
        if (this.pkPoint == pkPoint) {
            return;
        }
        this.pkPoint = pkPoint;
        getBaseService().update("pkPoint", pkPoint, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "pkPoint");
        }
    }

    public void saveOs(int os) {
        if (os == this.os) {
            return;
        }
        this.os = os;
        getBaseService().update("os", os, id, new AsyncInfo(id));
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "os");
        }
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `player` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`accountID` bigint(20) NOT NULL DEFAULT '0' COMMENT '玩家账号ID',"
                + "`sid` int(11) NOT NULL DEFAULT '0' COMMENT '服务器id',"
                + "`name` varchar(50) NOT NULL DEFAULT '' COMMENT '玩家名称/昵称',"
                + "`icon` int(11) NOT NULL DEFAULT '0' COMMENT '头像',"
                + "`sex` int(11) NOT NULL DEFAULT '0' COMMENT '性别',"
                + "`headImageUrl` varchar(300) NOT NULL DEFAULT '' COMMENT '头像url地址',"
                + "`createTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '账号创建时间毫秒',"
                + "`lv` int(11) NOT NULL DEFAULT '0' COMMENT '等级',"
                + "`gmLevel` int(11) NOT NULL DEFAULT '0' COMMENT 'gm权限',"
                + "`vipLevel` int(11) NOT NULL DEFAULT '0' COMMENT 'vip等级',"
                + "`vipExp` int(11) NOT NULL DEFAULT '0' COMMENT 'vip经验',"
                + "`totalRecharge` int(11) NOT NULL DEFAULT '0' COMMENT '累计充值, 统计玩家充值RMB',"
                + "`crystal` int(11) NOT NULL DEFAULT '0' COMMENT '钻石',"
                + "`gold` int(11) NOT NULL DEFAULT '0' COMMENT '游戏币',"
                + "`roomCard` int(11) NOT NULL DEFAULT '0' COMMENT '房卡数量',"
                + "`fastCard` int(11) NOT NULL DEFAULT '0' COMMENT '闪电卷',"
                + "`cheatTimes` int(11) NOT NULL DEFAULT '0' COMMENT '作弊次数',"
                + "`banned_ChatExpiredTime` int(11) NOT NULL DEFAULT '0' COMMENT '禁言过期时间',"
                + "`banned_LoginExpiredTime` int(11) NOT NULL DEFAULT '0' COMMENT '禁登过期时间',"
                + "`bannedTimes` int(11) NOT NULL DEFAULT '0' COMMENT '封号次数',"
                + "`lastLogin` int(11) NOT NULL DEFAULT '0' COMMENT '最近一次登陆時間',"
                + "`lastLogout` int(11) NOT NULL DEFAULT '0' COMMENT '最近一次登出時間',"
                + "`currentGameType` int(11) NOT NULL DEFAULT '-1' COMMENT '当前的游戏类型',"
                + "`familyID` bigint(20) NOT NULL DEFAULT '10001' COMMENT '工会ID',"
                + "`realName` varchar(12) NOT NULL DEFAULT '' COMMENT '真实名字',"
                + "`realNumber` varchar(50) NOT NULL DEFAULT '' COMMENT '真实号码',"
                + "`zleRoomCard` int(11) NOT NULL DEFAULT '0' COMMENT 'zle后台房卡数量',"
                + "`real_referer` int(11) NOT NULL DEFAULT '0' COMMENT '直接推荐人',"
                + "`wx_unionid` varchar(255) DEFAULT NULL COMMENT '微信ID',"
                + "`game_list` varchar(500) DEFAULT NULL COMMENT '玩家游戏列表',"
                + "`sendClubReward` bigint(20) NOT NULL DEFAULT '0' COMMENT '俱乐部奖励是否赠送0:没有赠送1:已赠送',"
                + "`clubTotalRecharge` int(11) NOT NULL DEFAULT '0' COMMENT '亲友圈累计充值, 统计玩家充值RMB',"
                + "`mjPoint` int(11) NOT NULL DEFAULT '0' COMMENT '麻将分数',"
                + "`pkPoint` int(11) NOT NULL DEFAULT '0' COMMENT '扑克分数',"
                + "`phone` bigint(20) NOT NULL DEFAULT '0' COMMENT '电话号码',"
                + "`os` int(2) NOT NULL DEFAULT '0' COMMENT 'app操作系统类型(0:未知或者网页,1:android,2:ios)',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `accountID` (`accountID`) USING BTREE,"
                + "KEY `name` (`name`) USING BTREE,"
                + "KEY `wx_unionid` (`wx_unionid`),"
                + "KEY `fsn` (`familyID`,`sid`,`name`) USING BTREE"
                + ") COMMENT='玩家信息表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }
}
