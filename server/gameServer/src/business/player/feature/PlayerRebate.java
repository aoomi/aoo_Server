package business.player.feature;

import business.global.config.DictionariesMapController;
import business.global.family.Family;
import business.global.family.FamilyManager;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.RebateEnum.RebateFlag;
import cenum.RebateEnum.RebateType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.RebateBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerBOService;
import core.db.service.clarkGame.RebateBOService;
import core.db.service.dbZle.ShareTiXianService;
import core.ioc.ContainerMgr;
import jsproto.c2s.iclass.SPlayer_Rebate;

import java.util.HashMap;
import java.util.Objects;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
public class PlayerRebate extends Feature {

    private RebateBOService rebateBOService;
    private ShareTiXianService shareTiXianService;
    private PlayerBOService playerBOService;

    public PlayerRebate(Player data) {
        super(data);
        rebateBOService = ContainerMgr.get().getComponent(RebateBOService.class);
        shareTiXianService = ContainerMgr.get().getComponent(ShareTiXianService.class);
        playerBOService = ContainerMgr.get().getComponent(PlayerBOService.class);
    }

    @Override
    public void loadDB() {
    }

    /**
     * 玩家可提现金额
     * TODO 2018/8/23  可提现金额  = ((玩家直推不包含代理直推的返利   RebateType == 1 && Flag == 0 ) + 任务奖励金 RebateType == 3) - 已领取的金额
     */
    public int drawMoney() {
        // ((玩家直推不包含代理直推的返利   RebateType == 1 && Flag == 0 ) + 任务奖励金 RebateType == 3)
        int totalRebate = rebateBOService.RebateSumFlag(this.player.getAccountID());
        //已经领取返利

        Criteria criteria = Restrictions.and(Restrictions.eq("player_id", this.player.getPid()), Restrictions.eq("status", 1));
        Long sum = shareTiXianService.sum(criteria, "money");
        int alreadyReceivedRebate = sum == null ? 0 : Math.toIntExact(sum);
        //  可提现金额  = ((玩家直推不包含代理直推的返利   RebateType == 1 && Flag == 0 ) + 任务奖励金 RebateType == 3) - 已领取的金额
        int rebateInt = totalRebate - alreadyReceivedRebate;
        if (rebateInt <= 0) {
            return 0;
        }
        return rebateInt;
    }


    /**
     * 玩家返利
     */
    public void rebate(String orderID, int app_price) {
        int reffer_rebate = DictionariesMapController.getIntByKey("reffer_rebate");
        int real_referer = this.player.getPlayerBO().getRealReferer();
        Player refererPlayer = PlayerMgr.getInstance().getPlayerByAccountID(real_referer);
        if (null == refererPlayer) {
            agentRebate(orderID, app_price, 0);
        } else {
            int flag = checkAgent() ? RebateFlag.REBATEFLAG_AGENT.value() : RebateFlag.REBATEFLAG_PLAYER.value();
            refferRebate(orderID, app_price * reffer_rebate, real_referer, flag);
            agentRebate(orderID, app_price, reffer_rebate);
        }
    }

    /**
     * 推荐人返利
     */
    public void refferRebate(String orderID, int app_price, int real_referer, int flag) {
        if (app_price <= 0.0f) {
            return;
        }
        if (real_referer <= 0) {
            return;
        }
        RebateBO rebateBO = new RebateBO();
        rebateBO.setAccountID(real_referer);
        rebateBO.setSourceOfTime(CommTime.nowSecond() + "");
        rebateBO.setRebateType(RebateType.REBATETYPE_REFFER.value());
        rebateBO.setSourceOfAccount(this.player.getAccountID());
        rebateBO.setApp_price(app_price);
        rebateBO.setOrder_id(orderID);
        rebateBO.setFamilyID(this.player.getFamiliID());
        rebateBO.setFlag(flag);
        rebateBO.setClassificationRegionId(0);
        rebateBO.getBaseService().saveOrUpDate(rebateBO);
    }


    /**
     * 代理返利
     *
     * @param orderID       //订单号
     * @param app_price     //单价
     * @param reffer_rebate //推荐人分成
     */
    public void agentRebate(String orderID, int app_price, int reffer_rebate) {
        if (app_price <= 0.0f) {
            return;
        }

        //检查公会是否存在
        Family family = FamilyManager.getInstance().getFamily(this.player.getFamiliID());
        if (family == null) {
            CommLogD.info("agentRebate playerId Not Family getFamiliID " + this.player.getFamiliID());
            return;
        }

        //会长
        long familyPlayerPid = family.getOwnerID();

        //公会
        Player familyPlayer = PlayerMgr.getInstance().getPlayer(familyPlayerPid);
        if (null == familyPlayer) {
            CommLogD.info("agentRebate Family player not find familyPlayer familyPid=" + familyPlayerPid);
            return;
        }

        int price = app_price * (family.getFamilyBO().getFencheng() - reffer_rebate);
        if (price <= 0) {
            return;
        }

        RebateBO rebateBO = new RebateBO();
        rebateBO.setAccountID(familyPlayer.getAccountID());
        rebateBO.setSourceOfTime(CommTime.nowSecond() + "");
        rebateBO.setRebateType(RebateType.REBATETYPE_AGENT.value());
        rebateBO.setSourceOfAccount(this.player.getAccountID());
        rebateBO.setApp_price(price);
        rebateBO.setOrder_id(orderID);
        rebateBO.setFamilyID(this.player.getFamiliID());
        rebateBO.setFlag(RebateFlag.REBATEFLAG_AGENT.value());
        rebateBO.setClassificationRegionId(0);
        rebateBO.getBaseService().saveOrUpDate(rebateBO);
    }


    /**
     * 判断是否是直推
     */
    public boolean checkAgent() {
        //检查公会是否存在
        Family family = FamilyManager.getInstance().getFamily(this.player.getFamiliID());
        if (family == null) {
            CommLogD.info("agentRebate playerId Not Family getFamiliID " + this.player.getFamiliID());
            return false;
        }

        //会长
        long familyPlayerPid = family.getOwnerID();
        //公会
        Player familyPlayer = PlayerMgr.getInstance().getPlayer(familyPlayerPid);
        if (null == familyPlayer) {
            CommLogD.info("agentRebate Family player not find familyPlayer familyPid=" + familyPlayerPid);
            return false;
        }

        int real_referer = this.player.getPlayerBO().getRealReferer();
        return real_referer == familyPlayer.getAccountID();
    }


    /**
     * 获取返利信息
     */
    public void onGetRebateInfo() {
        //返利比例
        float rebatePercentage = DictionariesMapController.getFloatByKey("reffer_rebate");

        //总返利
        int totalRebate = rebateBOService.RebateSumFlag(this.player.getAccountID());

        //已经领取返利
        Criteria criteria = Restrictions.and(Restrictions.eq("player_id", this.player.getPid()), Restrictions.eq("status", 1));
        Long sum = shareTiXianService.sum(criteria, "money");

        int alreadyReceivedRebate = sum == null ? 0 : Math.toIntExact(sum);
        //绑定人数
        HashMap<String, Object> refererConditions = new HashMap<String, Object>(1);
        refererConditions.put("real_referer", this.player.getAccountID());
        Long count = playerBOService.count(Restrictions.eq("real_referer", this.player.getAccountID()));
        int refererCount = count == null ? 0 : Math.toIntExact(count);

        this.player.pushProto(SPlayer_Rebate.make(rebatePercentage, totalRebate, alreadyReceivedRebate, refererCount));
    }
}
