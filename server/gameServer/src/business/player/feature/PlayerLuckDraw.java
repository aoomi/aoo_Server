package business.player.feature;

import BaseCommon.CommLog;
import business.global.club.ClubMgr;
import business.global.config.LuckDrawConfigMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import cenum.ItemFlow;
import cenum.LuckDrawEnum.LuckDrawAssignCrowd;
import cenum.LuckDrawEnum.LuckDrawType;
import cenum.PrizeType;
import cenum.RebateEnum;
import com.ddm.server.common.utils.ChanceSelect;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Maps;
import core.db.entity.clarkGame.LuckDrawBO;
import core.db.entity.clarkGame.LuckDrawConfigBO;
import core.db.entity.clarkGame.PlayerLuckDrawInfoBO;
import core.db.entity.clarkGame.RebateBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.LuckDrawBOService;
import core.db.service.clarkGame.PlayerLuckDrawInfoBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.luckdraw.AssignCrowdAndConditionItem;
import jsproto.c2s.cclass.luckdraw.LuckDrawInfo;
import jsproto.c2s.cclass.luckdraw.LuckDrawValueInfo;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 玩家幸运转盘
 */
@Data
public class PlayerLuckDraw extends Feature {
    /**
     * 玩家抽奖信息列表
     */
    Map<Integer, PlayerLuckDrawInfoBO> luckDrawInfoBOMap = Maps.newConcurrentMap();

    public PlayerLuckDraw(Player player) {
        super(player);
    }

    @Override
    public void loadDB() {
        List<PlayerLuckDrawInfoBO> playerLuckDrawInfoBOList = ContainerMgr
                .get()
                .getComponent(PlayerLuckDrawInfoBOService.class)
                .findAll(Restrictions.eq("pid", getPid()));
        if (CollectionUtils.isNotEmpty(playerLuckDrawInfoBOList)) {
            playerLuckDrawInfoBOList.stream().forEach(k -> {
                int key = (int) (k.getConfigId());
                PlayerLuckDrawInfoBO playerLuckDrawInfo = getLuckDrawInfoBOMap().get(key);
                if (Objects.isNull(playerLuckDrawInfo)) {
                    getLuckDrawInfoBOMap().put(key, k);
                } else {
                    playerLuckDrawInfo.gainCount(playerLuckDrawInfo.getCount() + k.getCount(), playerLuckDrawInfo.getConditionValue() + k.getConditionValue());
                    k.getBaseService().delete(k.getId(), new AsyncInfo(k.getId()));
                }
            });
        }
    }

    private PlayerLuckDrawInfoBO getPlayerLuckDrawInfoBO(int id, int type) {
        int configId = type <= 0 ? id : 0;
        int key = (type + configId);
        PlayerLuckDrawInfoBO playerLuckDrawInfoBO = this.getLuckDrawInfoBOMap().get(key);
        if (Objects.isNull(playerLuckDrawInfoBO)) {
            playerLuckDrawInfoBO = new PlayerLuckDrawInfoBO();
            playerLuckDrawInfoBO.setPid(this.getPid());
            playerLuckDrawInfoBO.setConfigId(type <= 0 ? id : type);
            playerLuckDrawInfoBO.setCount(type <= 0 ? LuckDrawConfigMgr.getInstance().luckDrawValue(type) : 0);
            playerLuckDrawInfoBO.setCreateTime(CommTime.nowSecond());
            playerLuckDrawInfoBO.setUpdateTime(CommTime.nowSecond());
            playerLuckDrawInfoBO.setType(type);
            playerLuckDrawInfoBO.getBaseService().save(playerLuckDrawInfoBO);
            this.getLuckDrawInfoBOMap().put(key, playerLuckDrawInfoBO);
        } else {
            if (type <= 0 && playerLuckDrawInfoBO.getConfigId() > 0L && !CommTime.isSameDayWithInTimeZone(playerLuckDrawInfoBO.getUpdateTime(), CommTime.nowSecond())) {
                playerLuckDrawInfoBO.saveCount(LuckDrawConfigMgr.getInstance().luckDrawValue(type));
            }
        }

        return playerLuckDrawInfoBO;
    }


    public int getPlayerLuckDrawInfoValue(int id, int type) {
        PlayerLuckDrawInfoBO playerLuckDrawInfoBO = getPlayerLuckDrawInfoBO(id, type);
        if (Objects.isNull(playerLuckDrawInfoBO)) {
            return 0;
        }
        return playerLuckDrawInfoBO.getCount();
    }

    /**
     * 检查玩家本身是否可以抽奖
     *
     * @param assignCrowd 指定人群
     * @param clubId      亲友圈Id
     * @return
     */
    public boolean checkLuckDraw(LuckDrawAssignCrowd assignCrowd, long clubId) {
        switch (assignCrowd) {
            case ALL:
                return LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, player.getId());
            case AGENT:
                return LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, player.getFamiliID());
            case CLUB:
                return clubId > 0L && player.getSendClubReward() > 0L && LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, clubId);
            case UNION:
                return clubId > 0L && player.getSendClubReward() > 0L && LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, clubId);
            default:
                return false;
        }
    }

    /**
     * 任务操作
     *
     * @param clubId         亲友圈Id
     * @param conditionValue 抽奖条件值
     * @return
     */
    public boolean execCondition(AssignCrowdAndConditionItem assignCrowdAndConditionItem, long clubId, int conditionValue) {
        if (LuckDrawConfigMgr.getInstance().getCondition() != assignCrowdAndConditionItem.getLuckDrawType().ordinal()) {
            return false;
        }
        if (this.checkLuckDraw(assignCrowdAndConditionItem.getAssignCrowd(), clubId)) {
            return gainLuckDraw((int) LuckDrawConfigMgr.getInstance().getLuckDrawConfigBO().getId(), assignCrowdAndConditionItem.getLuckDrawType().ordinal(), conditionValue);
        }
        return false;
    }

    /**
     * 检查玩家本身是否可以抽奖
     *
     * @return
     */
    public boolean checkLuckDraw() {
        LuckDrawAssignCrowd assignCrowd = LuckDrawConfigMgr.getInstance().getAssignCrowd();
        if (Objects.isNull(assignCrowd)) {
            return false;
        }
        switch (assignCrowd) {
            case ALL:
                return LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, player.getId());
            case AGENT:
                return LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, player.getFamiliID());
            case CLUB:
                return player.getSendClubReward() > 0L && LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, ClubMgr.getInstance().getClubMemberMgr().playerClubList(player.getPid()));
            case UNION:
                return player.getSendClubReward() > 0L && LuckDrawConfigMgr.getInstance().checkLuckDraw(player.getPid(), assignCrowd, UnionMgr.getInstance().getUnionMemberMgr().playerUnionList(player.getPid()));
            default:
                return false;
        }
    }


    /**
     * 检查是否有抽奖权限
     *
     * @return
     */
    public SData_Result checkLuckDrawResult() {
        if (checkLuckDraw()) {
            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.NotAllow);
    }

    /**
     * 获取抽奖信息
     *
     * @return
     */
    public SData_Result getLuckDrawInfo() {
        if (!this.checkLuckDraw()) {
            return SData_Result.make(ErrorCode.Activity_Close, "LuckDraw error not Open");
        }
        LuckDrawConfigBO configBO = LuckDrawConfigMgr.getInstance().getLuckDrawConfigBO();
        int free = this.getPlayerLuckDrawInfoValue((int) LuckDrawConfigMgr.getInstance().getLuckDrawConfigBO().getId(), LuckDrawType.FREE.ordinal());
        if (free > 0) {
            // 有免费次数,返回抽奖类型和次数
            return SData_Result.make(ErrorCode.Success, new LuckDrawValueInfo(LuckDrawType.FREE.ordinal(), 0, configBO.getCostFreeluckDrawValue(), free, configBO.getDateType(), configBO.getTimeSlot(), configBO.getStartTime(), configBO.getEndTime()));
        }
        int value = this.getPlayerLuckDrawInfoValue((int) LuckDrawConfigMgr.getInstance().getLuckDrawConfigBO().getId(), LuckDrawConfigMgr.getInstance().getCondition());
        // 返回抽奖类型和次数
        return SData_Result.make(ErrorCode.Success, new LuckDrawValueInfo(LuckDrawConfigMgr.getInstance().getCondition(), LuckDrawConfigMgr.getInstance().getConditionValue(), LuckDrawConfigMgr.getInstance().luckDrawValue(), value, configBO.getDateType(), configBO.getTimeSlot(), configBO.getStartTime(), configBO.getEndTime()));
    }


    /**
     * 执行抽奖
     *
     * @return
     */
    public SData_Result execLuckDraw() {
        if (!this.checkLuckDraw()) {
            return SData_Result.make(ErrorCode.Activity_Close, "LuckDraw error not Open");
        }
        List<LuckDrawBO> findAll = ContainerMgr.get().getComponent(LuckDrawBOService.class).findAll(null).stream().sorted(Comparator.comparing(LuckDrawBO::getChance)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findAll)) {
            return SData_Result.make(ErrorCode.NotAllow, "LuckDraw error not Open");
        }
        // 获取配置ID
        int configId = (int) LuckDrawConfigMgr.getInstance().getLuckDrawConfigBO().getId();
        // 检查是否有足够的抽奖次数
        if (consumeLuckDraw(configId, LuckDrawType.FREE.ordinal(), 1) || consumeLuckDraw(configId, LuckDrawConfigMgr.getInstance().getCondition(), 1)) {
            return execLuckDraw(findAll);
        }
        return SData_Result.make(ErrorCode.NotEnough_CompleteCount);
    }

    /**
     * 执行抽奖操作
     *
     * @param findAll 奖品列表
     * @return
     */
    private SData_Result execLuckDraw(List<LuckDrawBO> findAll) {
        // 奖励列表牌型分组
        Map<Long, Integer> map = findAll.stream().sorted(Comparator.comparing(LuckDrawBO::getChance)).collect(Collectors.toMap(k -> k.getId(), k -> k.getChance()));
        // 随机
        Long id = ChanceSelect.ChanceSelect(map);
        LuckDrawBO luckDraw = null;
        if (Objects.nonNull(id)) {
            // 找到对应的奖品
            luckDraw = findAll.stream().filter(k -> k.getId() == id.longValue()).findAny().orElse(null);
        }
        if (Objects.isNull(luckDraw)) {
            // 没有奖品拿最后一个
            luckDraw = findAll.get(findAll.size() - 1);
        }
        // 领取奖励
        this.execReward(luckDraw);
        FlowLogger.luckDrawRecordLog(player.getPid(), luckDraw.getPrizeName(), luckDraw.getPrizeType(), luckDraw.getRewardNum(), CommTime.nowSecond());
        return SData_Result.make(ErrorCode.Success, new LuckDrawInfo(luckDraw.getId(), luckDraw.getPrizeName(), luckDraw.getPrizeType(), luckDraw.getRewardNum()));
    }

    /**
     * 领取奖励
     */
    private void execReward(LuckDrawBO luckDrawBO) {
        PrizeType prizeType = PrizeType.valueOf(luckDrawBO.getPrizeType());
        switch (prizeType) {
            case Gold:
                getPlayer().getFeature(PlayerCurrency.class).gainItemFlow(prizeType, luckDrawBO.getRewardNum(), ItemFlow.LuckDraw);
                return;
            case RoomCard:
                getPlayer().getFeature(PlayerWallet.class).gainItemFlow(luckDrawBO.getRewardNum(), ItemFlow.LuckDraw);
                return;
            case RedEnvelope:
                RebateBO rebateBO = new RebateBO();
                rebateBO.setAccountID(this.player.getAccountID());
                rebateBO.setSourceOfTime(CommTime.nowSecond() + "");
                rebateBO.setRebateType(RebateEnum.RebateType.REBATETYPE_LUCK_DRAW.value());
                rebateBO.setSourceOfAccount(this.player.getAccountID());
                rebateBO.setApp_price(luckDrawBO.getRewardNum());
                rebateBO.setFamilyID(this.player.getFamiliID());
                rebateBO.setClassificationRegionId(0);
                rebateBO.getBaseService().saveOrUpDate(rebateBO);
                return;
            default:
                CommLog.info("execReward Pid：{}，PrizeType:{},RewardNum:{}", getPid(), luckDrawBO.getPrizeType(), luckDrawBO.getRewardNum());
        }
    }


    /**
     * 获取抽奖次数
     *
     * @param configId
     * @param type
     * @param conditionValue
     * @return
     */
    public boolean gainLuckDraw(int configId, int type, int conditionValue) {
        if (conditionValue <= 0) {
            return false;
        }
        // 获取抽奖次数
        PlayerLuckDrawInfoBO luckDrawInfoBO = this.getPlayerLuckDrawInfoBO(configId, type);
        if (Objects.isNull(luckDrawInfoBO)) {
            return false;
        }
        // 抽奖次数
        int luckDrawValue = LuckDrawConfigMgr.getInstance().luckDrawValue();
        // 条件值
        int curConditionValue = LuckDrawConfigMgr.getInstance().getConditionValue();
        if (luckDrawValue <= 0 || conditionValue <= 0) {
            return false;
        }
        this.lock();
        // 获取玩家当前条件值
        int beforeConditionValue = luckDrawInfoBO.getConditionValue();
        // 玩家总条件值
        int finalconditionValue = Math.min(1999999999, beforeConditionValue + conditionValue);
        // 玩家抽奖次数
        int before = luckDrawInfoBO.getCount();
        int finalValue = 0;
        if (finalconditionValue >= curConditionValue) {
            // 计算可兑换
            int value = finalconditionValue / curConditionValue;
            // 计算消耗
            int mValue = value * curConditionValue;
            // 兑换结果
            finalValue = Math.min(1999999999, before + value * luckDrawValue);
            // 抽奖结果
            finalconditionValue = Math.max(0, finalconditionValue - mValue);
            luckDrawInfoBO.gainCount(finalValue, finalconditionValue);
        } else {
            luckDrawInfoBO.saveConditionValue(finalconditionValue);
        }
        this.unlock();
        return true;

    }


    /**
     * 消耗抽奖次数
     *
     * @return
     */
    public boolean consumeLuckDraw(int configId, int type, int value) {
        if (value <= 0) {
            return false;
        }
        // 获取抽奖次数
        PlayerLuckDrawInfoBO luckDrawInfoBO = this.getPlayerLuckDrawInfoBO(configId, type);
        if (Objects.isNull(luckDrawInfoBO)) {
            return false;
        }
        if (luckDrawInfoBO.getCount() < value) {
            return false;
        }
        this.lock();
        int before = luckDrawInfoBO.getCount();
        int finalValue = Math.max(0, before - value);
        luckDrawInfoBO.consumeCount(finalValue);
        this.unlock();
        return true;
    }

}
