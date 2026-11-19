package business.player.feature;

import business.global.family.Family;
import business.global.family.FamilyManager;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.FamilyEnum.BindingFamilyEnum;
import cenum.ItemFlow;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.PlayerBindingFamilyBO;
import core.db.entity.clarkGame.ZleRechargeBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.FamilyItem;
import jsproto.c2s.cclass.family.PlayerBindingFamily;
import jsproto.c2s.iclass.S2119_InitPlayerFamily;
import jsproto.c2s.iclass.family.SPlayer_BindingFamily;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class PlayerFamily extends Feature {
    public static final int FAMILY = 2;

    public PlayerFamily(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }

    // --------封包数据获取--------
    public S2119_InitPlayerFamily getPlayerFamilyInfo() {
        return S2119_InitPlayerFamily.make(this.getPlayer().getFamiliID());
    }

    /**
     * 绑定工会*
     *
     * @param id          工会Id
     * @param bFamilyEnum 选择绑定状态
     */
    public SData_Result family(String id, BindingFamilyEnum bFamilyEnum) {
        if (BindingFamilyEnum.Select.equals(bFamilyEnum)) {
            return selectFamily(bFamilyEnum, this.getPlayer().getFamiliID());
        } else if (BindingFamilyEnum.Check.equals(bFamilyEnum)) {
            return selectFamily(bFamilyEnum, TypeUtils.StringTypeLong(id));
        } else if (BindingFamilyEnum.BinDing.equals(bFamilyEnum)) {
            return bindingFamily(bFamilyEnum, TypeUtils.StringTypeLong(id));
        }
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
    }

    /**
     * 查询工会
     *
     * @param bFamilyEnum //查看
     *                    Select(1),
     *                    //检查认证
     *                    Check(2),
     *                    //绑定工会
     *                    BinDing(3)
     * @param id          公会Id
     * @return
     */
    private SData_Result selectFamily(BindingFamilyEnum bFamilyEnum, long id) {
        // 检查玩家是否拥有工会
        if (id == Family.DefaultFamilyID) {
            return SData_Result.make(ErrorCode.NotAllow, "not DefaultFamilyID:{%d}", this.getPlayer().getFamiliID());
        }
        // 检查玩家工会是否存在
        Family family = FamilyManager.getInstance().getFamily(id);
        if (null == family) {
            return SData_Result.make(ErrorCode.NotAllow, "not Family:{%d}", this.getPlayer().getFamiliID());
        }
        // 获取玩家工会信息
        return SData_Result.make(ErrorCode.Success, SPlayer_BindingFamily.make(this.getPlayer().getPid(), bFamilyEnum, new PlayerBindingFamily(family.getFamilyID(), family.getName())));
    }

    /**
     * 绑定公会
     *
     * @param bFamilyEnum //查看
     *                    Select(1),
     *                    //检查认证
     *                    Check(2),
     *                    //绑定工会
     *                    BinDing(3)
     * @param id          公会Id
     * @return
     */
    private SData_Result bindingFamily(BindingFamilyEnum bFamilyEnum, long id) {
        if (this.getPlayer().getFamiliID() > Family.DefaultFamilyID) {
            return SData_Result.make(ErrorCode.NotAllow, "bindingFamily exist :{%d}", this.getPlayer().getFamiliID());
        }
        // 检查玩家是否拥有工会
        if (id == Family.DefaultFamilyID) {
            return SData_Result.make(ErrorCode.NotAllow, "not DefaultFamilyID:{%d}", this.getPlayer().getFamiliID());
        }
        // 检查玩家工会是否存在
        Family family = FamilyManager.getInstance().getFamily(id);
        if (Objects.isNull(family)) {
            return SData_Result.make(ErrorCode.NotAllow, "not Family:{%d}", this.getPlayer().getFamiliID());
        }
        Player ownerPlayer = PlayerMgr.getInstance().getPlayer(family.getFamilyBO().getOwnerID());
        if (Objects.isNull(ownerPlayer)) {
            // 公会会长不存在.
            return SData_Result.make(ErrorCode.NotAllow, "Not Family ownerPlayer null ");
        }
        this.getPlayer().getPlayerBO().saveFamilyID(family.getFamilyBO().getFamilyID());
        roomCardReward();
        insert(String.valueOf(id), FAMILY);
        return SData_Result.make(ErrorCode.Success, SPlayer_BindingFamily.make(this.getPlayer().getPid(), bFamilyEnum, new PlayerBindingFamily(family.getFamilyID(), family.getName())));
    }

    /**
     * 赠送房卡
     */
    public void roomCardReward() {
        this.getPlayer().getFeature(PlayerWallet.class).gainItemFlow(GameConfig.BindingFamilyReward(), ItemFlow.FamilyID);
    }

    /**
     * 添加一个绑定记录
     *
     * @param familyidStr
     * @param type
     */
    public void insert(String familyidStr, int type) {
        PlayerBindingFamilyBO pFamilyBO = new PlayerBindingFamilyBO();
        pFamilyBO.setPid(this.getPlayer().getPid());
        pFamilyBO.setType(type);
        pFamilyBO.setValue(familyidStr);
        pFamilyBO.setTime(CommTime.nowSecond());
        pFamilyBO.getBaseService().save(pFamilyBO, new AsyncInfo(pFamilyBO.getId()));

    }


    /**
     * 检查是否公会代理
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkFamilyOwner() {
        if (Config.DE_DEBUG() || Config.DE_DEBUG_ROOM()) {
            return SData_Result.make(ErrorCode.Success, new FamilyItem(1, Collections.emptyList(), 1));
        }
        if (this.getPlayer().getFamiliID() == Family.DefaultFamilyID || this.getPlayer().getFamiliID() <= 0) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        Family family = FamilyManager.getInstance().getFamily(player.getFamiliID());
        if (Objects.isNull(family)) {
            return SData_Result.make(ErrorCode.NotExist_Family, "NotExist_Family player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        // 检查公会会长是否本玩家
        if (this.getPlayer().getPid() == family.getOwnerID()) {
            return SData_Result.make(ErrorCode.Success,
                    new FamilyItem(family.getFamilyBO().getVip(), Collections.emptyList(), family.getFamilyBO().getPower()));
        } else {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner error player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
    }

    /**
     * 获取玩家的上级代理
     */
    public SData_Result<?> getFamilyRecommend() {
        if (this.getPlayer().getFamiliID() == Family.DefaultFamilyID || getPlayer().getFamiliID() <= 0) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        Family family = FamilyManager.getInstance().getFamily(getPlayer().getFamiliID());
        if (null == family) {
            return SData_Result.make(ErrorCode.NotExist_Family, "NotExist_Family player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        // 检查公会会长是否本玩家
        if (this.getPlayer().getPid() != family.getOwnerID()) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "this.player.getPid({%d}) != family.getOwnerID({%d})", this.getPlayer().getPid(), family.getOwnerID());
        } else {
            return SData_Result.make(ErrorCode.Success, (int) family.getFamilyBO().getRecommend());
        }
    }


    public SData_Result<?> agentGiveRoomCard(long toPid, int roomCard) {
        Player toPlayer = PlayerMgr.getInstance().getPlayer(toPid);
        if (this.getPlayer().getFamiliID() == Family.DefaultFamilyID || getPlayer().getFamiliID() <= 0) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        Family family = FamilyManager.getInstance().getFamily(getPlayer().getFamiliID());
        if (Objects.isNull(family)) {
            return SData_Result.make(ErrorCode.NotExist_Family, "NotExist_Family player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        if (this.getPlayer().getPid() != family.getOwnerID()) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "this.player.getPid({%d}) != family.getOwnerID({%d})", this.getPlayer().getPid(), family.getOwnerID());
        }
        if (null == toPlayer) {
            return SData_Result.make(ErrorCode.Player_PidError, "agentPresentingRoomCard null == toPlayer toPid:{%d}", toPid);
        }
        if (toPlayer.getFamiliID() != family.getFamilyID()) {
            @SuppressWarnings("rawtypes")
            SData_Result result = toPlayer.getFeature(PlayerFamily.class).getFamilyRecommend();
            if (!ErrorCode.Success.equals(result.getCode()) || family.getFamilyID() != result.getCustom()) {
                return SData_Result.make(ErrorCode.Not_Family_Member, "Not_Family_Member");
            }
        }
        if (this.getPlayer().getFeature(PlayerWallet.class).checkAndConsumeItemFlow(roomCard, ItemFlow.AgentGiveRoomCard)) {
            toPlayer.getFeature(PlayerWallet.class).gainItemFlow(roomCard, ItemFlow.AgentGiveRoomCard);
            ZleRechargeBO zleRechargeBO = new ZleRechargeBO();
            zleRechargeBO.setToId(toPlayer.getPid());
            zleRechargeBO.setRoomCard(roomCard);
            zleRechargeBO.setType(1);
            zleRechargeBO.setCreateTime(CommTime.nowSecond());
            zleRechargeBO.setClassificationRegionId(0);
            zleRechargeBO.setKeyId(getPid());
            zleRechargeBO.setGive_roomcard_num(0);
            zleRechargeBO.setBeizhu("赠送");
            zleRechargeBO.getBaseService().save(zleRechargeBO);
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.NotEnough_RoomCard, "NotEnough_RoomCard");
        }
    }

    /**
     * 检查是否公会代理
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkFamilyOwnerPhone() {
        if (this.getPlayer().getFamiliID() == Family.DefaultFamilyID || this.getPlayer().getFamiliID() <= 0) {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        Family family = FamilyManager.getInstance().getFamily(player.getFamiliID());
        if (Objects.isNull(family)) {
            return SData_Result.make(ErrorCode.NotExist_Family, "NotExist_Family player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
        // 检查公会会长是否本玩家
        if (this.getPlayer().getPid() == family.getOwnerID()) {
            if (this.getPlayer().getPlayerBO().getPhone() <= 0L) {
                return SData_Result.make(ErrorCode.Success);
            }
            return SData_Result.make(ErrorCode.NotAllow, "NotAllow error player.getFamiliID():{%d}", getPlayer().getFamiliID());
        } else {
            return SData_Result.make(ErrorCode.Not_Family_Owner, "Not_Family_Owner error player.getFamiliID():{%d}", getPlayer().getFamiliID());
        }
    }

    /**
     * 获取玩家的上级代理
     */
    public List<Long> getFamilyIdList() {
        if (this.getPlayer().getFamiliID() == Family.DefaultFamilyID || getPlayer().getFamiliID() <= 0L) {
            return Collections.emptyList();
        }
        Family family = FamilyManager.getInstance().getFamily(getPlayer().getFamiliID());
        if (Objects.isNull(family)) {
            return Collections.emptyList();
        }
        List<Long> familyIdList = Lists.newArrayList(getPlayer().getFamiliID());
        // 检查公会会长是否本玩家
        if (this.getPlayer().getPid() != family.getOwnerID()) {
            return familyIdList;
        } else {
            if (family.getFamilyBO().getRecommend() > 0L) {
                familyIdList.add(family.getFamilyBO().getRecommend());
            }
            return familyIdList;
        }
    }

}
