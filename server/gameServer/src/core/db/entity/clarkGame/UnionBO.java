package core.db.entity.clarkGame;

import business.global.shareunion.ShareUnionListMgr;
import cenum.PrizeType;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.Config;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 大赛事
 *
 * @author Huaxing
 */
@TableName(value = "bigUnion")

@Data
@NoArgsConstructor
public class UnionBO extends BaseEntity<UnionBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "int(11)", fieldname = "unionSign", comment = "赛事标识ID")
    private int unionSign;
    @DataBaseField(type = "varchar(50)", fieldname = "name", comment = "赛事名称")
    private String name = "";
    @DataBaseField(type = "bigint(20)", fieldname = "ownerId", comment = "赛事盟主")
    private long ownerId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈ID")
    private long clubId;
    @DataBaseField(type = "int(2)", fieldname = "join", comment = "加入申请(0需要审核、1不需要审核)")
    private int join;
    @DataBaseField(type = "int(2)", fieldname = "quit", comment = "退出申请(0需要审核、1不需要审核)")
    private int quit;
    @DataBaseField(type = "int(2)", fieldname = "expression", comment = "魔法表情(0可以使用、1不可以使用)")
    private int expression;
    @DataBaseField(type = "int(2)", fieldname = "state", comment = "赛事状态(0启用、1停用)")
    private int state;
    @DataBaseField(type = "int(2)", fieldname = "sports", comment = "竞技点(0不清零、1每天清零、2每周清零、3每月清零)")
    private int sports;
    @DataBaseField(type = "int(11)", fieldname = "distime", comment = "解散时间")
    private int distime;// 解散时间
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;
    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "俱乐部代理ID")
    private long agentsID;// 工会ID
    @DataBaseField(type = "int(11)", fieldname = "level", comment = "俱乐部代理等级")
    private int level;// 代理等级
    @DataBaseField(type = "double(11,2)", fieldname = "initSports", comment = "裁判力度")
    private double initSports;
    @DataBaseField(type = "int(3)", fieldname = "matchRate", comment = "比赛频率（30天，7天，每天）")
    private int matchRate;
    @DataBaseField(type = "double(11,2)", fieldname = "outSports", comment = "赛事淘汰")
    private double outSports;
    @DataBaseField(type = "int(11)", fieldname = "startRoundTime", comment = "本轮开始时间戳（秒）")
    private int startRoundTime;
    @DataBaseField(type = "int(11)", fieldname = "endRoundTime", comment = "本轮结束时间戳（秒）")
    private int endRoundTime;
    @DataBaseField(type = "int(1)", fieldname = "prizeType", comment = "消耗类型")
    private int prizeType;
    @DataBaseField(type = "int(3)", fieldname = "ranking", comment = "排名")
    private int ranking;
    @DataBaseField(type = "int(11)", fieldname = "value", comment = "数量")
    private int value;
    @DataBaseField(type = "int(11)", fieldname = "newUnionTime", comment = "新赛事时间")
    private int newUnionTime;
    @DataBaseField(type = "int(1)", fieldname = "sort", comment = "排序")
    private int sort;
    @DataBaseField(type = "int(11)", fieldname = "roundId", comment = "回合Id")
    private int roundId;
    @DataBaseField(type = "varchar(50)", fieldname = "clubName", comment = "主办亲友圈名称")
    private String clubName = "";
    @DataBaseField(type = "int(11)", fieldname = "unionDiamondsAttentionMinister", comment = "联盟管理员钻石提醒")
    private int unionDiamondsAttentionMinister;
    @DataBaseField(type = "int(11)", fieldname = "unionDiamondsAttentionAll", comment = "联盟全员钻石提醒")
    private int unionDiamondsAttentionAll;
    @DataBaseField(type = "int(11)", fieldname = "tableNum", comment = "显示的桌子数量")
    private int tableNum;
    @DataBaseField(type = "int(2)", fieldname = "joinClubSameUnion", comment = "允许亲友圈添加同赛事玩家 0:允许,1:不允许", defaultValue = "1")
    private int joinClubSameUnion = 1;
    @DataBaseField(type = "int(2)", fieldname = "showLostConnect", comment = "显示失去连接(0:仅管理员,1:所有人)")
    private int showLostConnect;
    @DataBaseField(type = "int(2)", fieldname = "caseStatus", comment = "保险箱功能(0关闭、1开启)")
    private int caseStatus;
    @DataBaseField(type = "int(2)", fieldname = "shareStatus", comment = "分成方式储存到保险箱(0关闭、1开启)")
    private int shareStatus;
    @DataBaseField(type = "int(2)", fieldname = "examineStatus", comment = "审核功能(0关闭、1开启)")
    private int examineStatus;
    @DataBaseField(type = "int(2)", fieldname = "skinType", comment = "皮肤类型(0皮肤一 ,1皮肤二,3中至皮肤)")
    private int skinType = 0;
    @DataBaseField(type = "int(2)", fieldname = "showUplevelId", comment = "显示上级及所属亲友圈")
    private int showUplevelId = 0;
    @DataBaseField(type = "int(2)", fieldname = "showClubSign", comment = "显示本圈标志")
    private int showClubSign = 0;
    @DataBaseField(type = "int(2)", fieldname = "changeAllyLeader", comment = "修改圈主状态")
    private int changeAllyLeader = 0;
    @DataBaseField(type = "int(2)", fieldname = "unionType", comment = "联赛类型 0 正常 1中至")
    private int unionType = 0;
    @DataBaseField(type = "int(2)", fieldname = "zhongZhiShowStatus", comment = "显示状态(是否显示成员总积分)")
    private int zhongZhiShowStatus = 0;
    @DataBaseField(type = "int(2)", fieldname = "skinTable", comment = "桌子类型")
    private int skinTable = -1;
    @DataBaseField(type = "int(2)", fieldname = "skinBackColor", comment = "背景类型")
    private int skinBackColor = -1;
    @DataBaseField(type = "int(2)", fieldname = "hideStatus", comment = "隐藏功能(0 关闭 1开启)")
    private int hideStatus = 0;
    @DataBaseField(type = "int(2)", fieldname = "rankedOpenZhongZhi", comment = "对所有用户开放 排行榜功能(0 关闭 1开启)")
    private int rankedOpenZhongZhi = 1;
    @DataBaseField(type = "int(2)", fieldname = "rankedOpenEntryZhongZhi", comment = "开放入口 排行榜功能(0 关闭 1开启)")
    private int rankedOpenEntryZhongZhi = 1;

    public void clear() {

    }

    /**
     * 修改开放入口
     *
     * @param rankedOpenEntryZhongZhi
     */
    public void saveRankedOpenEntryZhongZhi(int rankedOpenEntryZhongZhi) {
        if (this.rankedOpenEntryZhongZhi == rankedOpenEntryZhongZhi) {
            return;
        }
        this.rankedOpenEntryZhongZhi = rankedOpenEntryZhongZhi;
        getBaseService().update("rankedOpenEntryZhongZhi", rankedOpenEntryZhongZhi, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "rankedOpenEntryZhongZhi");
        }
    }

    /**
     * 修改对所有用户开放
     *
     * @param rankedOpenZhongZhi
     */
    public void saveRankedOpenZhongZhi(int rankedOpenZhongZhi) {
        if (this.rankedOpenZhongZhi == rankedOpenZhongZhi) {
            return;
        }
        this.rankedOpenZhongZhi = rankedOpenZhongZhi;
        getBaseService().update("rankedOpenZhongZhi", rankedOpenZhongZhi, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "rankedOpenZhongZhi");
        }
    }

    /**
     * 保存钻石消耗提醒
     *
     * @param unionDiamondsAttentionMinister
     */
    public void saveUnionDiamondsAttentionMinister(int unionDiamondsAttentionMinister) {
        if (this.unionDiamondsAttentionMinister == unionDiamondsAttentionMinister) {
            return;
        }
        this.unionDiamondsAttentionMinister = unionDiamondsAttentionMinister;
        getBaseService().update("unionDiamondsAttentionMinister", unionDiamondsAttentionMinister, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "unionDiamondsAttentionMinister");
        }
    }

    /**
     * 保存钻石消耗提醒
     *
     * @param unionDiamondsAttentionAll
     */
    public void saveUnionDiamondsAttentionAll(int unionDiamondsAttentionAll) {
        if (this.unionDiamondsAttentionAll == unionDiamondsAttentionAll) {
            return;
        }
        this.unionDiamondsAttentionAll = unionDiamondsAttentionAll;
        getBaseService().update("unionDiamondsAttentionAll", unionDiamondsAttentionAll, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "unionDiamondsAttentionAll");
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
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "skinType");
        }
    }

    /**
     * 保存桌子类型
     *
     * @param skinTable
     */
    public void saveSkinTable(int skinTable) {
        if (this.skinTable == skinTable) {
            return;
        }
        this.skinTable = skinTable;
        getBaseService().update("skinTable", this.skinTable, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "skinTable");
        }
    }

    /**
     * 保存背景类型
     *
     * @param skinBackColor
     */
    public void saveSkinBackColor(int skinBackColor) {
        if (this.skinBackColor == skinBackColor) {
            return;
        }
        this.skinBackColor = skinBackColor;
        getBaseService().update("skinBackColor", this.skinBackColor, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "skinBackColor");
        }
    }

    /**
     * 保存皮肤
     *
     * @param showUplevelId
     */
    public void saveShowUplevelId(int showUplevelId) {
        if (this.showUplevelId == showUplevelId) {
            return;
        }
        this.showUplevelId = showUplevelId;
        getBaseService().update("showUplevelId", this.showUplevelId, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "showUplevelId");
        }
    }

    /**
     * 保存皮肤
     *
     * @param showClubSign
     */
    public void saveShowClubSign(int showClubSign) {
        if (this.showClubSign == showClubSign) {
            return;
        }
        this.showClubSign = showClubSign;
        getBaseService().update("showClubSign", this.showClubSign, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "showClubSign");
        }
    }

    /**
     * 保存
     *
     * @param changeAllyLeader
     */
    public void saveChangeAllyLeader(int changeAllyLeader) {
        if (this.changeAllyLeader == changeAllyLeader) {
            return;
        }
        this.changeAllyLeader = changeAllyLeader;
        getBaseService().update("changeAllyLeader", this.changeAllyLeader, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "changeAllyLeader");
        }
    }

    /**
     * 保存
     *
     * @param zhongZhiShowStatus
     */
    public void saveZhongZhiShowStatus(int zhongZhiShowStatus) {
        if (this.zhongZhiShowStatus == zhongZhiShowStatus) {
            return;
        }
        this.zhongZhiShowStatus = zhongZhiShowStatus;
        getBaseService().update("zhongZhiShowStatus", this.zhongZhiShowStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "zhongZhiShowStatus");
        }
    }

    /**
     * 保存联赛类型
     *
     * @param unionType
     */
    public void saveUnionType(int unionType) {
        if (this.unionType == unionType) {
            return;
        }
        this.unionType = unionType;
        getBaseService().update("unionType", this.unionType, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "unionType");
        }
    }

    /**
     * 保存设置排序
     *
     * @param sort
     */
    public void saveSort(int sort) {
        if (this.sort == sort) {
            return;
        }
        this.sort = sort;
        getBaseService().update("sort", this.sort, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "sort");
        }
    }

    public void saveClubName(String clubName) {
        if (StringUtils.isEmpty(clubName)) {
            return;
        }
        if (clubName.equals(this.clubName)) {
            return;
        }
        this.clubName = clubName;
        getBaseService().update("clubName", this.clubName, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "clubName");
        }
    }

    public void setName(String name) {
        this.name = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(name);
    }

    public void setUnionName(String name) {
        this.name = name;
    }


    /**
     * 裁判分
     */
    public double getInitSports() {
        return CommMath.FormatDouble(initSports);

    }

    /**
     * 淘汰分
     *
     * @return
     */
    public double getOutSports() {
        return CommMath.FormatDouble(outSports);
    }

    /**
     * 裁判分
     *
     * @param initSports
     */
    public void setInitSports(double initSports) {
        this.initSports = CommMath.FormatDouble(initSports);
    }


    /**
     * 保存初始裁判分
     *
     * @param initSports 裁判分
     */
    public void saveInitSports(double initSports) {
        Map<String, Object> update = com.google.common.collect.Maps.newHashMapWithExpectedSize(2);
        this.initSports = CommMath.FormatDouble(initSports);
        this.newUnionTime = CommTime.nowSecond();
        update.put("initSports", this.initSports);
        update.put("newUnionTime", this.newUnionTime);
        getBaseService().update(update, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "initSports", "newUnionTime");
        }

    }

    /**
     * 设置淘汰分
     *
     * @param outSports
     */
    public void setOutSports(double outSports) {
        this.outSports = CommMath.FormatDouble(outSports);
    }

    public void setPrizeType(int prizeType) {
        if (prizeType <= PrizeType.Gold.value()) {
            this.prizeType = PrizeType.Gold.value();
        }
        this.prizeType = prizeType;
    }

    public void setStartRoundTime(int startRoundTime) {
        if (startRoundTime <= 0) {
            startRoundTime = CommTime.getWithHourOfDay(6);
        }
        this.startRoundTime = startRoundTime;
    }

    public int getEndRoundTime() {
        if (this.endRoundTime <= 0) {
            this.setEndRoundTime(CommTime.StartTimeCalcToEndTime(this.getStartRoundTime(), getMatchRate()));
        }
        return endRoundTime;

    }

    public int getMatchRate() {
        return UnionDefine.UNION_MATCH_RATE.valueOf(this.matchRate).value();
    }

    public int getMatchRateValue() {
        return this.matchRate;
    }

    /**
     * 更新本轮的开始、结束时间
     */
    public void saveRoundTimeAndRoundId() {
        Map<String, Object> update = com.google.common.collect.Maps.newHashMapWithExpectedSize(3);
        // 设置本轮开始时间
        this.setStartRoundTime(CommTime.getWithHourOfDay(6));
        update.put("startRoundTime", this.getStartRoundTime());

        // 设置本轮结束时间
        this.setEndRoundTime(CommTime.StartTimeCalcToEndTime(this.getStartRoundTime(), this.getMatchRate()));
        update.put("endRoundTime", this.getEndRoundTime());

        // 回合Id递增
        this.setRoundId(this.getRoundId() + 1);
        update.put("roundId", this.getRoundId());
        getBaseService().update(update, getId(), new AsyncInfo(getId()));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "startRoundTime", "endRoundTime", "roundId");
        }
    }

    /**
     * 更新本轮的开始、结束时间
     */
    public void saveRoundTime() {
        Map<String, Object> update = com.google.common.collect.Maps.newHashMapWithExpectedSize(3);
        // 设置本轮开始时间
        this.setStartRoundTime(CommTime.getWithHourOfDay(6));
        update.put("startRoundTime", this.getStartRoundTime());

        // 设置本轮结束时间
        this.setEndRoundTime(CommTime.StartTimeCalcToEndTime(this.getStartRoundTime(), this.getMatchRate()));
        update.put("endRoundTime", this.getEndRoundTime());

        getBaseService().update(update, getId(), new AsyncInfo(getId()));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "startRoundTime", "endRoundTime");
        }
    }


    /**
     * 更新本轮的开始、结束时间
     */
    public void setRoundTime() {
        // 设置本轮开始时间
        this.setStartRoundTime(CommTime.getWithHourOfDay(6));
        // 设置本轮结束时间
        this.setEndRoundTime(CommTime.StartTimeCalcToEndTime(this.getStartRoundTime(), this.getMatchRate()));
    }


    public void saveOwnerId(long ownerId) {
        if (this.ownerId == ownerId) {
            return;
        }
        this.ownerId = ownerId;
        getBaseService().update("ownerId", ownerId, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "ownerId");
        }
    }

    public void saveTableNum(int tableNum) {
        if (this.tableNum == tableNum) {
            return;
        }
        this.tableNum = tableNum;
        getBaseService().update("tableNum", tableNum, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "tableNum");
        }
    }

    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "createTime");
        }
    }

    /**
     * 赛事设置
     *
     * @param name       	赛事名称：描述性文字；
     * @param join       	加入申请：需要审核、不需要审核；
     * @param quit       	退出申请：需要审核、不需要审核；
     * @param expression 	魔法表情：可以使用、不可以使用；
     * @param state      	赛事状态：启用、停用；
     * @param           竞技点清零：不清零、每天清零、每周清零、每月清零；
     */
    public void saveMap(String name, int join, int quit, int tableNum, int expression, int state, int sports, double initSports, int matchRate, double outSports, int prizeType, int ranking, int value, long exePid, int joinClubSameUnion) {
        HashMap<String, Object> map = new HashMap<>();

        if (initSports != this.initSports) {
            double oldInitSports = this.initSports;
            // 裁判力度修改
            this.initSports = CommMath.FormatDouble(initSports);
            map.put("initSports", this.initSports);
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_INIT_SPORTS, String.valueOf(this.initSports), String.valueOf(this.initSports), String.valueOf(oldInitSports));
        }
        if (matchRate != this.matchRate) {
            // 比赛频率
            this.matchRate = matchRate;
            map.put("matchRate", matchRate);
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_MATCH_RATE, String.valueOf(getMatchRate()));
        }
        if (outSports != this.outSports) {
            double oldOutSports = this.outSports;
            // 淘汰分
            this.outSports = CommMath.FormatDouble(outSports);
            map.put("outSports", this.outSports);
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_OUT_SPORTS, String.valueOf(this.outSports), String.valueOf(this.outSports), String.valueOf(oldOutSports));
        }

        boolean isPrizeType = false;
        if (prizeType != this.prizeType) {
            // 奖励类型
            this.prizeType = prizeType;
            map.put("prizeType", prizeType);
            isPrizeType = true;
        }
        if (ranking != this.ranking) {
            // 奖励排名
            this.ranking = ranking;
            map.put("ranking", ranking);
            isPrizeType = true;
        }
        if (value != this.value) {
            // 奖励值
            this.value = value;
            map.put("value", value);
            isPrizeType = true;
        }
        if (isPrizeType) {
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_REWARD);
        }

        if (StringUtils.isNotEmpty(name) && !name.equals(this.name)) {
            // 修改名称,敏感字符屏蔽
            this.name = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(name);
            map.put("name", name);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_UPDATE_NAME, this.name);
        }
        if (join != this.join) {
            this.join = join;
            map.put("join", join);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_JOIN.UNION_JOIN_NEED_AUDIT.equals(UnionDefine.UNION_JOIN.valueOf(join)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_OPEN : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_STOP);
        }

        if (joinClubSameUnion != this.joinClubSameUnion) {
            this.joinClubSameUnion = joinClubSameUnion;
            map.put("joinClubSameUnion", joinClubSameUnion);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_JOIN_CLUB_SAME_UNION.UNION_JOIN_NEED_AUDIT.equals(UnionDefine.UNION_JOIN_CLUB_SAME_UNION.valueOf(joinClubSameUnion)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_CLUB_SAME_UNION_OPEN : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_CLUB_SAME_UNION_STOP);

        }

        if (quit != this.quit) {
            this.quit = quit;
            map.put("quit", quit);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_QUIT.UNION_QUIT_NEED_AUDIT.equals(UnionDefine.UNION_QUIT.valueOf(quit)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_QUIT_OPEN : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_QUIT_STOP);
        }
        if (tableNum != this.tableNum) {
            this.tableNum = tableNum;
            map.put("tableNum", tableNum);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CHANGE_TABLENUM, String.valueOf(UnionDefine.UNION_QUIT_TABLENUM.valueOf(tableNum).value()));
        }
        if (expression != this.expression) {
            this.expression = expression;
            map.put("expression", expression);
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_EXPRESSION.UNION_EXPRESSION_USE.equals(UnionDefine.UNION_EXPRESSION.valueOf(expression)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXPRESSION_START_UP : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXPRESSION_STOP_USING);

        }
        if (state != this.state && state >= 0) {
            if (this.state == 2 && state == 1) {
                // 这个时候已经是停赛状态了，所有没必要添加赛事动态记录
            } else {
                // 添加赛事动态记录
                this.insertUnionDynamicBO(exePid, UnionDefine.UNION_STATE.UNION_STATE_ENABLE.equals(UnionDefine.UNION_STATE.valueOf(state)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_START_UP : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_STOP_USING);
            }
            this.state = state;
            map.put("state", state);
        }
        if (sports != this.sports) {
            this.sports = sports;
            map.put("sports", sports);
            // 添加赛事动态记录
            UnionDefine.UNION_SPORTS unionSports = UnionDefine.UNION_SPORTS.valueOf(this.sports);
            UnionDefine.UNION_EXEC_TYPE unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_NOT;
            if (UnionDefine.UNION_SPORTS.UNION_SPORTS_NO_CLEAR.equals(unionSports)) {
                unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_NO_CLEAR;
            } else if (UnionDefine.UNION_SPORTS.UNION_SPORTS_CLEAR_DAILY.equals(unionSports)) {
                unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CLEAR_DAILY;
            } else if (UnionDefine.UNION_SPORTS.UNION_SPORTS_CLEAR_MONTHLY.equals(unionSports)) {
                unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CLEAR_MONTHLY;
            } else {
                unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CLEAR_WEEKLY;
            }
            this.insertUnionDynamicBO(exePid, unionExecType);
        }
        getBaseService().update(map, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoMap(this, map);
        }
    }


    public void saveJoinClubSameUnion(long exePid, int joinClubSameUnion) {
        if (joinClubSameUnion != this.joinClubSameUnion) {
            this.joinClubSameUnion = joinClubSameUnion;
            getBaseService().update("joinClubSameUnion", joinClubSameUnion, id, new AsyncInfo(id));
            // 添加赛事动态记录
            this.insertUnionDynamicBO(exePid, UnionDefine.UNION_JOIN_CLUB_SAME_UNION.UNION_JOIN_NEED_AUDIT.equals(UnionDefine.UNION_JOIN_CLUB_SAME_UNION.valueOf(joinClubSameUnion)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_CLUB_SAME_UNION_OPEN : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JOIN_CLUB_SAME_UNION_STOP);
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().updateUnionBoField(this, "joinClubSameUnion");
            }
        }
    }

    /**
     * 奖励不足
     */
    public void saveNotEnoughRewardStopState() {
        Map<String, Object> update = com.google.common.collect.Maps.newHashMapWithExpectedSize(2);
        // 停赛状态
        this.state = UnionDefine.UNION_STATE.UNION_STATE_NOT_ENOUGH_REWARD.ordinal();
        update.put("state", state);
        // 回合Id递增
        this.setRoundId(this.getRoundId() + 1);
        update.put("roundId", this.getRoundId());

        getBaseService().update(update, id, new AsyncInfo(id));
        // 添加赛事动态记录
        this.insertUnionDynamicBO(0, UnionDefine.UNION_STATE.UNION_STATE_ENABLE.equals(UnionDefine.UNION_STATE.valueOf(state)) ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_START_UP : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_STOP_USING);
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "roundId", "state");
        }
    }

    /**
     * 手动操作停止
     */
    public void saveStopState() {
        Map<String, Object> update = com.google.common.collect.Maps.newHashMapWithExpectedSize(2);
        // 回合Id递增
        this.setRoundId(this.getRoundId() + 1);
        update.put("roundId", this.getRoundId());

        this.setEndRoundTime(CommTime.nowSecond());
        update.put("endRoundTime", this.getEndRoundTime());

        getBaseService().update(update, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "roundId", "endRoundTime");
        }

    }

    /**
     * 只存在开始和停止状态
     *
     * @return
     */
    public int getStateValue() {
        return UnionDefine.UNION_STATE.isEnable(this.state) ? 0 : 1;
    }

    /**
     * 添加赛事动态记录
     *
     * @param pid           用户Pid
     * @param unionExecType 执行类型
     */
    private void insertUnionDynamicBO(long pid, UnionDefine.UNION_EXEC_TYPE unionExecType) {
        UnionDynamicBO.insertUnionConfig(pid, this.getClubId(), this.getId(), CommTime.nowSecond(), unionExecType.value(), "");
    }

    /**
     * 添加赛事动态记录
     *
     * @param pid           用户Pid
     * @param unionExecType 执行类型
     */
    private void insertUnionDynamicBO(long pid, UnionDefine.UNION_EXEC_TYPE unionExecType, String value) {
        UnionDynamicBO.insertUnionConfig(pid, this.getClubId(), this.getId(), CommTime.nowSecond(), unionExecType.value(), value);
    }

    /**
     * 添加赛事动态记录
     * 裁判力度修改：玩家@玩家名称[ID:@id] 修改了联赛的裁判力度为@值，修改前为@值；
     * 	赛事淘汰值修改：玩家@玩家名称[ID:@id] 修改了联赛的淘汰值为@值，修改前为@值；
     *
     * @param pid           用户Pid
     * @param unionExecType 执行类型
     */
    private void insertUnionDynamicBO(long pid, UnionDefine.UNION_EXEC_TYPE unionExecType, String value, String curValue, String preValue) {
        UnionDynamicBO.insertUnionConfig(pid, this.getClubId(), this.getId(), CommTime.nowSecond(), unionExecType.value(), value, curValue, preValue);
    }

    public void saveShowLostConnect(int showLostConnect) {
        if (this.showLostConnect == showLostConnect) {
            return;
        }
        this.showLostConnect = showLostConnect;
        getBaseService().update("showLostConnect", showLostConnect, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "showLostConnect");
        }
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `bigUnion` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionSign` int(11) NOT NULL DEFAULT '0' COMMENT '赛事标识ID',"
                + "`name` varchar(50) NOT NULL DEFAULT ''  COMMENT '赛事名称',"
                + "`ownerId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事盟主',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈ID',"
                + "`join` int(2) NOT NULL DEFAULT '0' COMMENT '加入申请(0需要审核、1不需要审核)',"
                + "`quit` int(2) NOT NULL DEFAULT '0' COMMENT '退出申请(0需要审核、1不需要审核)',"
                + "`expression` int(2) NOT NULL DEFAULT '0' COMMENT '魔法表情(0可以使用、1不可以使用)',"
                + "`state` int(2) NOT NULL DEFAULT '0' COMMENT '赛事状态(0启用、1停用)',"
                + "`sports` int(2) NOT NULL DEFAULT '0' COMMENT '竞技点(0不清零、1每天清零、2每周清零、3每月清零)',"
                + "`distime` varchar(15) NOT NULL DEFAULT ''  COMMENT '解散时间',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`agentsID` bigint(20) NOT NULL DEFAULT '0'  COMMENT '俱乐部代理ID',"
                + "`level` int(11) NOT NULL DEFAULT '0'  COMMENT '俱乐部代理等级',"
                + "`initSports` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '裁判力度',"
                + "`matchRate` int(11) NOT NULL DEFAULT '0'  COMMENT '比赛频率（30天，7天，每天）',"
                + "`outSports` double(11,2) NOT NULL DEFAULT '0.00' COMMENT '赛事淘汰',"
                + "`prizeType` int(11) NOT NULL DEFAULT '0'  COMMENT '消耗类型',"
                + "`ranking` int(11) NOT NULL DEFAULT '0'  COMMENT '排名',"
                + "`value` int(11) NOT NULL DEFAULT '0'  COMMENT '数量',"
                + "`startRoundTime` int(11) NOT NULL DEFAULT '0'  COMMENT '本轮开始时间戳（秒）',"
                + "`endRoundTime` int(11) NOT NULL DEFAULT '0'  COMMENT '本轮结束时间戳（秒）',"
                + "`newUnionTime` int(11) NOT NULL DEFAULT '0'  COMMENT '新赛事时间',"
                + "`sort` int(1) NOT NULL DEFAULT '0'  COMMENT '排序',"
                + "`roundId` int(11) NOT NULL DEFAULT '0'  COMMENT '回合Id',"
                + "`clubName` varchar(50) NOT NULL DEFAULT ''  COMMENT '主办亲友圈名称',"
                + "`unionDiamondsAttentionMinister` int(11) NOT NULL DEFAULT '500' COMMENT '俱乐部管理员钻石提醒',"
                + "`unionDiamondsAttentionAll` int(11) NOT NULL DEFAULT '100' COMMENT '俱乐部全员钻石提醒',"
                + "`tableNum` int(11) NOT NULL DEFAULT '0'  COMMENT '显示的桌子数量',"
                + "`joinClubSameUnion` int(2) NOT NULL DEFAULT '1' COMMENT '允许亲友圈添加同赛事玩家 0:允许,1:不允许',"
                + "`showLostConnect` int(2) NOT NULL DEFAULT '0' COMMENT '显示失去连接(0:仅管理员,1:所有人)',"
                + "`caseStatus` int(2) NOT NULL DEFAULT '1' COMMENT '保险箱功能(0关闭、1开启)',"
                + "`shareStatus` int(2) NOT NULL DEFAULT '1' COMMENT '分成方式储存到保险箱(0关闭、1开启)',"
                + "`examineStatus` int(2) NOT NULL DEFAULT '1' COMMENT '审核功能(0关闭、1开启)',"
                + "`skinType` int(2) NOT NULL DEFAULT '0' COMMENT '皮肤类型)',"
                + "`showUplevelId` int(2) NOT NULL DEFAULT '0' COMMENT '显示上级及所属亲友圈)',"
                + "`showClubSign` int(2) NOT NULL DEFAULT '0' COMMENT '显示本圈标志)',"
                + "`unionType` int(2) NOT NULL DEFAULT '0' COMMENT '联赛类型 0 正常 1中至)',"
                + "`skinTable` int(2) NOT NULL DEFAULT '-1' COMMENT '桌子类型)',"
                + "`skinBackColor` int(2) NOT NULL DEFAULT '-1' COMMENT '背景类型)',"
                + "`hideStatus` int(2) NOT NULL DEFAULT '0' COMMENT '隐藏功能(0 关闭 1开启))',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `clubId` (`clubId`)"
                + ") COMMENT='大赛事'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

    /**
     * 关闭、解散亲友圈状态
     */
    public void closeUnion() {
//        HashMap<String, Object> map = new HashMap<>(3);
//        map.put("distime", this.getDistime());
//        map.put("state", this.getState());
//        this.getBaseService().update(map, getId());
        this.getBaseService().delete(this.getId());
    }

    public void saveCaseStatus(int caseStatus) {
        if (this.caseStatus == caseStatus) {
            return;
        }
        this.caseStatus = caseStatus;
        getBaseService().update("caseStatus", caseStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "caseStatus");
        }
    }

    public void saveShareStatus(int shareStatus) {
        if (this.shareStatus == shareStatus) {
            return;
        }
        this.shareStatus = shareStatus;
        getBaseService().update("shareStatus", shareStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "shareStatus");
        }
    }

    public void saveExamineStatus(int examineStatus) {
        if (this.examineStatus == examineStatus) {
            return;
        }
        this.examineStatus = examineStatus;
        getBaseService().update("examineStatus", examineStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "examineStatus");
        }
    }

    public void saveHideStatus(int hideStatus) {
        if (this.hideStatus == hideStatus) {
            return;
        }
        this.hideStatus = hideStatus;
        getBaseService().update("hideStatus", hideStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().updateUnionBoField(this, "hideStatus");
        }
    }
}
