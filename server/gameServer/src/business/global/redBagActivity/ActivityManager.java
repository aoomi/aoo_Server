package business.global.redBagActivity;

import BaseTask.AsynTask.AsyncCallBackTaskBase;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import core.db.entity.clarkGame.RedBagActivityBO;
import core.db.other.AsyncInfo;
import core.db.service.clarkGame.RedBagActivityBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.GameType;

import java.util.List;
import java.util.Map;

public class ActivityManager {
    private static ActivityManager instance = new ActivityManager();
    private Map<Long, Activity> m_ActivityMap = Maps.newConcurrentMap();

    public static ActivityManager getInstance() {
        return instance;
    }

    /**
     * 获取活动
     */
    public Activity getActivity(long activityID) {
        return m_ActivityMap.get(activityID);
    }

    public void init() {
        ContainerMgr.get().getComponent(RedBagActivityBOService.class).findAll(null, new AsyncInfo(new AsyncCallBackTaskBase<List<RedBagActivityBO>>() {
            @Override
            public void runSuc(List<RedBagActivityBO> o) {
                CommLogD.info("查询红包活动信息成功！size =  " + o.size());
                for (RedBagActivityBO hBo : o) {
                    if (CommTime.nowSecond() > hBo.getEnd_time()) {
                        continue;
                    }
                    m_ActivityMap.put(hBo.getId(), new Activity(hBo));
                }
            }

            @Override
            public void runError() {
                CommLogD.error("查询红包活动信息失败！ ");
            }
        }, 1, true));
    }

    /**
     * 刷新活动
     */
    public boolean onUpdateActivity(long activityID, int crowd, String crowdDaili, String gameType, int beginTime, int endTime, int maxMoney, int everyMoney) {
        Activity activity = m_ActivityMap.get(activityID);

        if (null == activity) {
            RedBagActivityBO bo = ContainerMgr.get().getComponent(RedBagActivityBOService.class).findOne(activityID, null);
            if (null == bo) {
                return false;
            }
            activity = new Activity(bo);
            m_ActivityMap.put(bo.getId(), activity);
        }

        activity.onUpdateActivity(crowd, crowdDaili, gameType, beginTime, endTime, maxMoney, everyMoney);

        if (activity.getRedActivityBO().getEnd_time() < CommTime.nowSecond()) {
            m_ActivityMap.remove(activityID);
        }

        return true;
    }

    /**
     * 刷新活动
     */
    public boolean onInstertActivity(int crowd, String crowdDaili, String gameType, int beginTime, int endTime, int maxMoney, int everyMoney) {
        RedBagActivityBO redActivityBO = new RedBagActivityBO();
        redActivityBO.setCrowd(crowd);
        redActivityBO.setCrowd_daili(crowdDaili);
        redActivityBO.setGame_type(gameType);
        redActivityBO.setBegin_time(beginTime);
        redActivityBO.setEnd_time(endTime);
        redActivityBO.setMax_money(maxMoney);
        redActivityBO.setEvery_money(everyMoney);
        redActivityBO.setCreate_time(CommTime.nowSecond());
        boolean flag = redActivityBO.getBaseService().saveOrUpDate(redActivityBO) > -1;
        if (!flag) {
            return false;
        }

        Activity activity = new Activity(redActivityBO);
//不可能为空
//		if (null == activity) {
//			return false;
//		}

        m_ActivityMap.put(activity.getActivityID(), activity);

        if (activity.getRedActivityBO().getEnd_time() < CommTime.nowSecond()) {
            m_ActivityMap.remove(activity.getActivityID());
        }

        return true;
    }

    /**
     * 关闭所有活动
     */
    public boolean closeAllActivity() {
        for (Map.Entry<Long, Activity> entry : m_ActivityMap.entrySet()) {
            entry.getValue().onCloseAllActivity(CommTime.nowSecond());
        }
        m_ActivityMap.clear();
        return true;
    }

    /**
     * 关闭所有活动
     */
    public boolean closeActivity(long activityID, int endTime) {
        Activity activity = m_ActivityMap.get(activityID);

        if (null == activity) {
            return false;
        }

        activity.onCloseAllActivity(endTime);

        if (activity.getRedActivityBO().getEnd_time() < CommTime.nowSecond()) {
            m_ActivityMap.remove(activityID);
        }

        return true;
    }


    /**
     * 检查是否有红包    如果在房间结束时，玩家同时满足两个活动的参与条件，则本房间随机参与一个金额未达到上限的活动；
     */
    public void checkHaveHongBao(List<Long> pidList, GameType gameType, long roomID) {
        for (Long pid : pidList) {
            for (Map.Entry<Long, Activity> entry : m_ActivityMap.entrySet()) {
                Activity activity = entry.getValue();
                if (null == activity) {
                    continue;
                }
                if (activity.checkGameType(gameType) && activity.checkJoinActivity(pid)) {
                    activity.sendActivityReward(pid, gameType, roomID);
                    break;
                }
            }
        }
    }
}
