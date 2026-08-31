package business.player.feature;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.family.Family;
import business.global.family.FamilyManager;
import business.player.Player;
import cenum.ConstEnum.RechargeType;
import cenum.ItemFlow;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.PlayerRechargeBO;
import core.db.entity.clarkGame.ZleRechargeBO;
import core.db.service.clarkGame.PlayerRechargeBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.ZleData_Result;

/**
 * 玩家充值
 *
 * @author Administrator
 */
public class PlayerRecharge extends Feature {
    public PlayerRecharge(Player player) {
        super(player);
    }

    @Override
    public void loadDB() {

    }

    /**
     * 插入记录
     *
     * @param value  值
     * @param type   类型
     */
    private void saveZleRechargeBO(int value, int type, String beizhu) {
        ZleRechargeBO zleRechargeBO = new ZleRechargeBO();
        zleRechargeBO.setToId(getPid());
        zleRechargeBO.setRoomCard(value);
        zleRechargeBO.setType(type);
        zleRechargeBO.setCreateTime(CommTime.nowSecond());
        zleRechargeBO.setClassificationRegionId(0);
        zleRechargeBO.setKeyId(71031L);
        zleRechargeBO.setGive_roomcard_num(2);
        zleRechargeBO.setBeizhu(beizhu);
        zleRechargeBO.getBaseService().save(zleRechargeBO);
    }

    /**
     * 给玩家充值
     *
     * @param value  值
     * @return
     */
    public String rechargePHPToPlayer(int value, String beizhu) {
        getPlayer().getFeature(PlayerWallet.class).gainItemFlow(value, ItemFlow.PHPToPlayer);
        this.saveZleRechargeBO(value, 1, beizhu);
        return ZleData_Result.make(ErrorCode.Success, "success");
    }

    /**
     * 扣除玩家钻石
     *
     * @param value  值
     * @return
     */
    public String deductPHPToPlayer(int value, String beizhu) {
        if (getPlayer().getFeature(PlayerWallet.class).checkAndConsumeItemFlow(value, ItemFlow.PHPToPlayer)) {
            this.saveZleRechargeBO(value, 2, beizhu);
            return ZleData_Result.make(ErrorCode.Success, "success");
        } else {
            return ZleData_Result.make(ErrorCode.NotEnough_Currency, String.format("pid:{%d},value:{%d}", getPid(), value));
        }

    }


    /**
     * 充值
     *
     * @param AppPrice     金额
     * @param RechargeNum  数量
     * @param orderID      订单
     * @param orderTime    订单时间
     * @param sourceType   来源类型 （0：微信APP，1：微信H5...）
     * @param platformType 平台名称 (WZ)
     * @param rechargeType 充值类型
     */
    public String recharge(int AppPrice, int RechargeNum, String orderID, long orderTime, int sourceType,
                           String platformType, int rechargeType, long clubID) {
        if (ContainerMgr.get().getComponent(PlayerRechargeBOService.class).existsOrderId(orderID)) {
            return ZleData_Result.make(ErrorCode.REPEAT_SUBMIT, "repeat");
        }
        // 获取指定城市的钻石
        int playerCityCurrencyValue = this.getPlayer().getFeature(PlayerWallet.class).balance();
        PlayerRechargeBO pRechargeBO = new PlayerRechargeBO();
        pRechargeBO.setAccountID(this.player.getAccountID());
        pRechargeBO.setPid(this.player.getPid());
        pRechargeBO.setCreateTime(CommTime.nowSecond());
        pRechargeBO.setFamilyID(this.player.getFamiliID());
        pRechargeBO.setAppPrice(AppPrice);
        pRechargeBO.setOrderId(orderID);
        pRechargeBO.setPreValue(playerCityCurrencyValue);
        pRechargeBO.setCurValue(playerCityCurrencyValue + RechargeNum);
        pRechargeBO.setOrderTime(orderTime);
        pRechargeBO.setRechargeNum(RechargeNum);
        pRechargeBO.setSourceType(sourceType);
        pRechargeBO.setPlatformType(platformType);
        pRechargeBO.setRechargeType(rechargeType);

        if (rechargeType == RechargeMallEnum.ClubMall.ordinal() && clubID > 0) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
            // 检查俱乐部数据是否存在
            if (null == club) {
                return ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST, ErrorCode.CLUB_NOT_EXIST.name());
            }
            pRechargeBO.setAgentsID(club.getClubListBO().getAgentsID());
            pRechargeBO.setLevel(club.getClubListBO().getLevel());
            pRechargeBO.setClubID(clubID);
            pRechargeBO.setClassificationRegionId(0);
            if (pRechargeBO.getBaseService().saveIgnore(pRechargeBO) > 0L) {
                // 亲友圈商城
                this.clubMall(AppPrice, orderID, RechargeNum, clubID, club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel());
            } else {
                return ZleData_Result.make(ErrorCode.REPEAT_SUBMIT, "repeat");
            }
        } else {
            pRechargeBO.setClassificationRegionId(0);
            if (pRechargeBO.getBaseService().saveIgnore(pRechargeBO) > 0L) {
                // 普通商城
                this.ordinaryMall(AppPrice, orderID, RechargeNum);
            } else {
                return ZleData_Result.make(ErrorCode.REPEAT_SUBMIT, "repeat");
            }
        }
        return ZleData_Result.make(ErrorCode.Success, "success");

    }


    /**
     * 普通商城
     *
     * @param AppPrice    金额
     * @param orderID     订单号
     * @param RechargeNum 数量
     */
    private void ordinaryMall(int AppPrice, String orderID, int RechargeNum) {
        Family family = FamilyManager.getInstance().getFamily(this.player.getFamiliID());
        // 检查工会是否存在
        if (null != family) {
            // 计算总充值
            family.addTotalRMB(AppPrice);
        }
        this.player.getPlayerBO().saveTotalRecharge(this.player.getPlayerBO().getTotalRecharge() + AppPrice);
        this.player.getFeature(PlayerWallet.class).gainItemFlow(RechargeNum, ItemFlow.Recharge);
        this.player.getFeature(PlayerRebate.class).rebate(orderID, AppPrice);
    }

    /**
     * 亲友圈商城
     *
     * @param AppPrice    金额
     * @param orderID     订单号
     * @param RechargeNum 数量
     * @param clubID      俱乐部ID
     */
    private void clubMall(int AppPrice, String orderID, int RechargeNum, long clubID, long agentsID, int level) {
        // 检查充值圈卡是否代理或者总台
        Family family = FamilyManager.getInstance().getFamily(agentsID);
        // 检查工会是否存在
        if (null != family) {
            // 计算总充值
            family.addClubTotalRMB(AppPrice);
            this.player.getFeature(PlayerClub.class).onClubCardRecharge(agentsID, level, clubID, RechargeNum, RechargeType.DirectCharge, ItemFlow.ClubCardRecharge);

        }
        this.player.getPlayerBO().saveClubTotalRecharge(this.player.getPlayerBO().getClubTotalRecharge() + AppPrice);
    }

    /**
     * 充值
     *
     * @author Administrator
     */
    public enum RechargeMallEnum {
        // 普通商城
        OrdinaryMall,
        // 亲友圈商城
        ClubMall,
        ;
    }

}
