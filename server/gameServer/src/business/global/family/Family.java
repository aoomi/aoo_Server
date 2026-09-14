package business.global.family;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerFamily;
import cenum.ConstEnum.RechargeType;
import cenum.ConstEnum.ResOpType;
import cenum.ItemFlow;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Maps;
import core.db.entity.clarkGame.FamilyBO;
import core.db.entity.clarkGame.ZleRechargeBO;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import lombok.Data;

import java.util.Objects;

@Data
public class Family {
    public static final long DefaultFamilyID = 10001L;

    private FamilyBO familyBO;

    public Family(FamilyBO familyBO) {
        this.familyBO = familyBO;
    }

    public void setFamilyBO(FamilyBO familyBO) {
        this.familyBO = familyBO;
    }

    public FamilyBO getFamilyBO() {
        return this.familyBO;
    }

    /**
     * 获取游戏列表
     *
     * @return
     */
    public String getHaveYouXi() {
        return this.familyBO.getHaveYouxi();
    }

    /**
     * 获取工会ID
     *
     * @return
     */
    public long getFamilyID() {
        return this.familyBO.getFamilyID();
    }

    /**
     * 获取名称
     *
     * @return
     */
    public String getName() {
        return this.familyBO.getName();
    }

    /**
     * 会长ID
     *
     * @return
     */
    public long getOwnerID() {
        return this.familyBO.getOwnerID();
    }

    /**
     * 获取RMB
     *
     * @return
     */
    public long getTotalRMB() {
        return this.familyBO.getTotalRMB();
    }

    /**
     * 计算人民币
     *
     * @param price
     */
    public void addTotalRMB(int price) {
        long totalRMB = this.familyBO.getTotalRMB();
        this.familyBO.saveTotalRMB_Sync(totalRMB + price);
    }

    /**
     * 俱乐部计算人民币
     *
     * @param price
     */
    public void addClubTotalRMB(int price) {
        long totalRMB = this.familyBO.getClubTotalRMB();
        this.familyBO.saveClubTotalRMB_Sync(totalRMB + price);
    }


    /**
     * 添加房卡
     */
    public void addRoomCard(int count) {
        int num = this.familyBO.getRoomcardNum() + count;
        this.familyBO.saveRoomcardNum_Sync(num);
    }

    /**
     * 获取房卡
     */
    public int getRoomCard() {
        return this.familyBO.getRoomcardNum();
    }

    /**
     * 设置房卡
     *
     * @param roomCard
     */
    public void setRoomCard(int roomCard) {
        int roomcardNum = this.familyBO.getRoomcardNum() + roomCard;
        if (roomcardNum <= 0) {
            this.familyBO.saveRoomcardNum_Sync(0);
        } else {
            this.familyBO.saveRoomcardNum_Sync(roomcardNum);
        }
    }

    /**
     * 代理对玩家操作圈卡
     *
     * @param player 玩家
     * @param value  钻石
     * @return
     */
    public SData_Result onFamilyCardToPlayer(Player player, int value, String beizhu) {
        // 检查玩家
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        SData_Result result = player.getFeature(PlayerFamily.class).getFamilyRecommend();
        if (ErrorCode.Success.equals(result.getCode())) {
            if (getFamilyID() != result.getCustom()) {
                return SData_Result.make(ErrorCode.Not_Family_Member, "getFamilyRecommend this.player.getPid({%d}) != family.getOwnerID({%d})", player.getPid(), getOwnerID());
            }
        } else if (player.getFamiliID() != getFamilyID()) {
            return SData_Result.make(ErrorCode.Not_Family_Member, "Not_Family_Member");
        }
        // 会长
        Player playerFamily = PlayerMgr.getInstance().getPlayer(this.getFamilyBO().getOwnerID());
        if (Objects.isNull(playerFamily)) {
            return SData_Result.make(ErrorCode.Player_PidError, "playerFamily error pid:{%d}", this.getFamilyBO().getOwnerID());
        }
        if (playerFamily.getFeature(PlayerWallet.class).checkAndConsumeItemFlow(value, ItemFlow.FamilyCardToPlayer)) {
            player.getFeature(PlayerWallet.class).gainItemFlow(value, ItemFlow.FamilyCardToPlayer);
            ZleRechargeBO zleRechargeBO = new ZleRechargeBO();
            zleRechargeBO.setToId(player.getPid());
            zleRechargeBO.setRoomCard(value);
            zleRechargeBO.setType(1);
            zleRechargeBO.setCreateTime(CommTime.nowSecond());
            zleRechargeBO.setClassificationRegionId(0);
            zleRechargeBO.setKeyId(getOwnerID());
            zleRechargeBO.setGive_roomcard_num(0);
            zleRechargeBO.setBeizhu(beizhu);
            zleRechargeBO.getBaseService().save(zleRechargeBO);
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.NotEnough_Currency, "playerFamily pid:{%d},value:{%d},cardValue:{%d}", playerFamily.getPid(), value, playerFamily.getFeature(PlayerWallet.class).balance());
        }
    }



}
