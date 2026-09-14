package business.global.redBagActivity;

import BaseThread.BaseMutexInstance;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.RebateEnum.RebateType;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.config.server.GameTypeMgr;
import core.db.entity.clarkGame.RebateBO;
import core.db.entity.clarkGame.RedBagActivityBO;
import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.RebateBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.redactivity.ActivityItem;
import jsproto.c2s.cclass.redactivity.RedActivity.JoinActivityType;
import jsproto.c2s.iclass.redactivity.SRedActivity_CheckReward;
import jsproto.c2s.iclass.redactivity.SRedActivity_GetReward;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;

public class Activity {
    protected RedBagActivityBO m_RedActivityBO;//红包活动数据
    protected ArrayList<GameType> m_GameTypes = new ArrayList<>();//活动游戏类型
    protected ArrayList<Long> m_AgentList = new ArrayList<>();    //活动可以参与的玩家
    protected ArrayList<Long> m_HaveRewardPidList = new ArrayList<>();    //有奖励的玩家列表
    protected Map<Long, ActivityItem> m_HaveRewardItemList = Maps.newConcurrentMap();    //有奖励的玩家列表

    protected final BaseMutexInstance _lock = new BaseMutexInstance();

    public Activity(RedBagActivityBO redActivityBO) {
        update(redActivityBO);
    }

    /**
     * 锁
     */
    public void lock() {
        _lock.lock();
    }

    /**
     * 解锁
     */
    public void unlock() {
        _lock.unlock();
    }

    /**
     * 更新数据
     */
    public void update(RedBagActivityBO redActivityBO) {
        this.m_RedActivityBO = redActivityBO;

        initGameType();

        initAgentID();
    }

    public boolean onUpdateActivity(int crowd, String crowdDaili, String gameType, int beginTime, int endTime, int maxMoney, int everyMoney) {
        this.m_RedActivityBO.setCrowd(crowd);
        this.m_RedActivityBO.setCrowd_daili(crowdDaili);
        this.m_RedActivityBO.setGame_type(gameType);
        this.m_RedActivityBO.setBegin_time(beginTime);
        this.m_RedActivityBO.setEnd_time(endTime);
        this.m_RedActivityBO.setMax_money(maxMoney);
        this.m_RedActivityBO.setEvery_money(everyMoney);
        this.m_RedActivityBO.setUpdatetime(CommTime.nowSecond());
        this.m_RedActivityBO.getBaseService().saveOrUpDate(m_RedActivityBO, new AsyncInfo(m_RedActivityBO.getId()));

        initGameType();

        initAgentID();
        return true;
    }

    public boolean onCloseAllActivity(int endTime) {

        this.m_RedActivityBO.setEnd_time(endTime);
        this.m_RedActivityBO.setUpdatetime(CommTime.nowSecond());
        this.m_RedActivityBO.getBaseService().saveOrUpDate(m_RedActivityBO, new AsyncInfo(m_RedActivityBO.getId()));

        initGameType();

        initAgentID();
        return true;
    }

    public void initGameType() {
        String gameTypeStr = m_RedActivityBO.getGame_type();
        // 检查工会是否指定游戏
        if (StringUtils.isNotEmpty(gameTypeStr)) {
            // 解析游戏列表
            String[] gameTypes = gameTypeStr.split(",");
            for (String str : gameTypes) {
                // 获取游戏类型
                int gameType = TypeUtils.StringTypeInt(str.trim());
                // 检查游戏类型是否正确
                if (gameType >= 0) {
                    m_GameTypes.add(GameTypeMgr.getInstance().gameType(gameType));
                }
            }
        }
    }

    public void initAgentID() {
        String agentStr = m_RedActivityBO.getCrowd_daili();
        // 检查工会是否指定游戏
        if (StringUtils.isNotEmpty(agentStr)) {
            // 解析游戏列表
            String[] gameTypes = agentStr.split(",");
            for (String str : gameTypes) {
                // 获取游戏类型
                long pid = TypeUtils.StringTypeLong(str.trim());
                // 检查游戏类型是否正确
                if (pid >= 0) {
                    m_AgentList.add(pid);
                }
            }
        }
    }


    public RedBagActivityBO getRedActivityBO() {
        return m_RedActivityBO;
    }

    /**
     * 检查玩家是否包含在该活动中
     */
    public boolean checkJoinActivity(long pid) {
        if (m_RedActivityBO.getBegin_time() > CommTime.nowSecond()) {
            return false;
        }
        if (m_RedActivityBO.getEnd_time() < CommTime.nowSecond()) {
            return false;
        }
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (null == player) {
            return false;
        }

        //已获得赏金
        Criteria criteria = Restrictions.and(Restrictions.eq("ActivityID", getActivityID()), Restrictions.eq("accountID", player.getAccountID()), Restrictions.eq("rebateType", RebateType.REBATETYPE_SHANGJIN.value()));
        long haveGetMoney = ContainerMgr.get().getComponent(RebateBOService.class).sum(criteria, "app_price");

        if (getMaxMoney() - haveGetMoney < 1) {
            return false;
        }

        JoinActivityType type = JoinActivityType.valueOf(m_RedActivityBO.getCrowd());
        if (JoinActivityType.AllPlayer.equals(type)) {
            return true;
        } else if (JoinActivityType.Agent.equals(type)) {
            return m_AgentList.contains(player.getFamiliID());
        } else if (JoinActivityType.AgentUnPlaying.equals(type)) {
            return !m_AgentList.contains(player.getFamiliID());
        }
        return false;
    }

    /**
     * 是否包含改游戏
     */
    public boolean checkGameType(GameType gameType) {
        String gameTypeStr = m_RedActivityBO.getGame_type();
        if (StringUtils.isEmpty(gameTypeStr)) {
            return false;
        }
        if ("-1".equals(gameTypeStr)) {
            return true;
        }
        return m_GameTypes.contains(gameType);
    }

    /**
     * 发放奖励
     */
    public void sendActivityReward(long pid, GameType gameType, long roomID) {
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (null == player) {
            return;
        }
        m_HaveRewardPidList.add(pid);
        m_HaveRewardItemList.put(pid, new ActivityItem(pid, gameType, roomID, CommTime.nowSecond()));

        player.pushProtoMq(SRedActivity_CheckReward.make(getActivityID(), true));
    }

    /**
     * 获取奖励
     */
    public void getReward(WebSocketRequest request, long pid, boolean isGetReward) {
        lock();
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (null == player) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "not player player pid = " + pid);
            }
            return;
        }

        if (!m_HaveRewardPidList.contains(pid)) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "you not have reward playerID =" + pid);
            }
            return;
        }
        ActivityItem activityItem = m_HaveRewardItemList.get(pid);
        if (null == activityItem) {
            if (null != request) {
                request.error(ErrorCode.NotAllow, "activityItem not have found playerID =" + pid);
            }
            return;
        }
        int money = 0;
        if (isGetReward) {
            //已获得赏金
            Criteria criteria = Restrictions.and(Restrictions.eq("ActivityID", getActivityID()), Restrictions.eq("accountID", player.getAccountID()), Restrictions.eq("rebateType", RebateType.REBATETYPE_SHANGJIN.value()));
            long haveGetMoney = ContainerMgr.get().getComponent(RebateBOService.class).sum(criteria, "app_price");

            if (getMaxMoney() - haveGetMoney >= 1) {
                money = CommMath.randomInt(1, (int) (getMaxMoney() - haveGetMoney));
            }
            if (money > getEveryMoney()) {
                money = CommMath.randomInt(1, getEveryMoney());
            }

            RebateBO rebateBO = new RebateBO();
            rebateBO.setAccountID(player.getAccountID());
            rebateBO.setSourceOfTime(CommTime.nowSecond() + "");
            rebateBO.setRebateType(RebateType.REBATETYPE_SHANGJIN.value());
            rebateBO.setApp_price(money);
            rebateBO.setActivityID(getActivityID());
            rebateBO.setGameType(activityItem.getGameType().getId());
            rebateBO.setRoomID(activityItem.getRoomID());
            rebateBO.setClassificationRegionId(0);
            rebateBO.getBaseService().saveOrUpDate(rebateBO, new AsyncInfo(1));
        }

        m_HaveRewardPidList.remove(pid);
        m_HaveRewardItemList.remove(pid);

        if (null != request) {
            request.response(SRedActivity_GetReward.make(getActivityID(), money));
        }
        unlock();
    }

    /**
     * 获取活动ID
     */
    public long getActivityID() {
        return this.m_RedActivityBO.getId();
    }

    /**
     * 最高赏金
     */
    public int getMaxMoney() {
        return this.m_RedActivityBO.getMax_money();
    }

    /**
     * 每次赏金
     */
    public int getEveryMoney() {
        return this.m_RedActivityBO.getEvery_money();
    }
}
