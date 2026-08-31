package business.player.feature.achievement;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.Feature;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerTask;
import cenum.RefererEnum.RefererListState;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.RefererReceiveListBO;
import core.db.entity.clarkGame.RefererShareBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.RefererReceiveListBOService;
import core.db.service.clarkGame.RefererShareBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.RefererReceiveItem;
import jsproto.c2s.cclass.RefererReceiveList;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskTargetType;
import jsproto.c2s.iclass.SPlayer_RefererReward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @author max
 * @date 2016年1月21日
 */
public class AchievementFeature extends Feature {
    // 页面大小
    private final static int PAGE_SIZE = 10;
    private RefererShareBOService refererShareBOService;
    private RefererReceiveListBOService refererReceiveListBOService;

    public AchievementFeature(Player owner) {
        super(owner);
        refererShareBOService = ContainerMgr.get().getComponent(RefererShareBOService.class);
        refererReceiveListBOService = ContainerMgr.get().getComponent(RefererReceiveListBOService.class);
    }


    @Override
    public void loadDB() {

    }


    /**
     * 领取分享奖励
     */
    public void receiveShare(WebSocketRequest request) {
        this.player.getFeature(PlayerTask.class).exeTask(TaskTargetType.Share.ordinal());
        //今日是否已经领取分享奖励。
        List<RefererShareBO> rShareBOs = refererShareBOService.findTodayAll(player.getPid());
        if (null != rShareBOs && rShareBOs.size() > 0) {
            request.error(ErrorCode.ALREADY_FETCH, "receiveShare");
            return;
        }
        int shareCard = GameConfig.ShareCard();
        //如果没有，增加数据库字段并更新用户房卡。
        RefererShareBO shareBO = new RefererShareBO();
        shareBO.setPid(player.getPid());
        shareBO.setReceiveCardNum(shareCard);
        shareBO.setType(1);
        shareBO.setCreateTime(CommTime.nowSecond());
        shareBO.getBaseService().saveOrUpDate(shareBO);
        player.getFeature(PlayerWallet.class).roomCardRefererReward(shareCard);
        request.response(SPlayer_RefererReward.make(player.getId(), "SUCCESS", shareCard));

    }


    /**
     * 查询推广奖励次数
     *
     * @return
     */
    public boolean checkReferer() {
        Criteria criteria = Restrictions.and(Restrictions.eq("refererId", this.player.getAccountID()), Restrictions.eq("completeCount", 4), Restrictions.eq("receive", RefererListState.Allow.value()));
        RefererReceiveListBO refererReceiveBO = refererReceiveListBOService.findOne(criteria, null);
        if (null == refererReceiveBO) {
            return false;
        }
        refererReceiveBO.setReceive(RefererListState.Complete.value());
        refererReceiveBO.getBaseService().saveOrUpDate(refererReceiveBO);
        return true;
    }

    /**
     * 累计成功邀请X位新用户，且该用户完成X局游戏；
     *
     * @return
     */
    public int refererCount() {
        long count = refererReceiveListBOService.count(Restrictions.and(Restrictions.eq("refererId", this.player.getAccountID()), Restrictions.eq("completeCount", 4)));
        return Math.toIntExact(count);
    }

    /**
     * 获取领取列表
     *
     * @return
     */
    public void getRefererReceiveList(WebSocketRequest request, int pageNum) {
        Criteria criteria = Restrictions.eq("refererId", this.player.getAccountID());
        criteria.desc("id");
        criteria.setPageNum(this.getPageNum(pageNum));
        criteria.setPageSize(PAGE_SIZE);
        List<RefererReceiveListBO> receiveList = refererReceiveListBOService.findAll(criteria, "");
        // 推广列表
        if (null == receiveList || receiveList.size() <= 0) {
            receiveList = new ArrayList<>();
        }

        List<RefererReceiveItem> receiveItems = new ArrayList<>();
        // 遍历推广列表
        for (RefererReceiveListBO refererReceiveListBO : receiveList) {
            // 获取指定的玩家
            Player player = PlayerMgr.getInstance().getPlayer(refererReceiveListBO.getPid());
            if (null == player) {
                continue;
            }
            // 玩家的总充值
            receiveItems.add(new RefererReceiveItem(
                    refererReceiveListBO.getPid(),
                    player.getName(),
                    refererReceiveListBO.getReceive(),
                    refererReceiveListBO.getCreateTime(),
                    player.getPlayerBO().getTotalRecharge()));
        }

        Map<String, Object> rTotalBO = refererReceiveListBOService.findRefererReceiveTotalBO(Restrictions.eq("refererId", this.player.getAccountID()));

        if (null == rTotalBO) {
            rTotalBO = new HashMap<>();
            rTotalBO.put("totalNumber", 0);
            rTotalBO.put("totalPrice", 0);
        }
        request.response(new RefererReceiveList(receiveItems,
                ((Number) rTotalBO.get("totalPrice")).intValue(),
                ((Number) rTotalBO.get("totalNumber")).intValue()));
    }

    /**
     * 获取当前页码
     *
     * @param pageNum
     * @return
     */
    private int getPageNum(int pageNum) {
        return pageNum <= 0 ? 0 : (pageNum - 1) * PAGE_SIZE;
    }


}
