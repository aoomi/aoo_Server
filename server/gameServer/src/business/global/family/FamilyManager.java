package business.global.family;

import BaseCommon.CommLog;
import business.global.config.CurrencyKeyMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import core.db.entity.clarkGame.FamilyBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.FamilyBOService;
import core.ioc.ContainerMgr;

import java.util.List;
import java.util.Map;

public class FamilyManager {

    // 所有工会<familyID,family>
    public final Map<Long, Family> allFamilyMap = Maps.newConcurrentMap();

    // 最大下发排行榜数据
    public int RandCount = 20;

    private FamilyBOService familyBOService;

    private static FamilyManager instance = new FamilyManager();

    public static FamilyManager getInstance() {
        return instance;
    }

    private FamilyManager() {
        familyBOService = ContainerMgr.get().getComponent(FamilyBOService.class);
    }

    // 开服初始化
    public void init() {
        CommLogD.info("[FamilyManager.init] load family begin...]");
        List<FamilyBO> allFamilys = familyBOService.findAll(null, "");
        if (null == allFamilys || allFamilys.size() <= 0) {
            setFamily(Family.DefaultFamilyID, "默认代理", 0L, 0, 0, 0, 0, 0, 10, 0, "", 0, 0);
        } else {
            for (FamilyBO bo : allFamilys) {
                if (bo.getFamilyID() > Family.DefaultFamilyID) {
                    CurrencyKeyMgr.getInstance().clearKey((int) bo.getFamilyID());
                }
                this.regFamily(new Family(bo));
            }
            if (!this.allFamilyMap.containsKey(Family.DefaultFamilyID)) {
                setFamily(Family.DefaultFamilyID, "默认代理", 0L, 0, 0, 0, 0, 0, 10, 0, "", 0, 0);
            }
        }
        CommLogD.info("[FamilyManager.init] load family end]");
    }


    /**
     * 更新公会
     *
     * @param familyID
     * @return
     */
    public boolean updateFamily(long familyID) {
        FamilyBO FamilyBOs = familyBOService.findOne(Restrictions.eq("familyID", familyID), null);
        if (null == FamilyBOs) {
            CommLog.error("checkAddFamily not have find:{}", familyID);
            return false;
        }
        this.regFamily(new Family(FamilyBOs));
        return true;
    }


    // 新增工会对象
    private void regFamily(Family family) {
        FamilyBO familyBo = family.getFamilyBO();
        long familyID = familyBo.getFamilyID();
        this.allFamilyMap.put(familyID, family);
    }

//	/**
//	 * 是否存在指定工会
//	 *
//	 * @param pid
//	 * @return
//	 */
//	public boolean haveFamily(long familyID) {
//		Family family = allFamilyMap.get(familyID);
//		if (family == null) {
//			return false;
//		}
//		return true;
//	}
//

    /**
     * 获取工会信息
     *
     * @param familyID
     * @return
     */
    public Family getFamily(long familyID) {
        Family family = allFamilyMap.get(familyID);
        if (family == null) {
            CommLogD.warn("getFamily({}) not find family", familyID);
        }
        return family;
    }

//	public Family getFamilyDB(long familyID) {
//		FamilyBO familyBO = BM.getBM(FamilyBO.class).findOne("familyID", familyID);
//		if (familyBO != null) {
//			return new Family(familyBO);
//		}
//		return null;
//	}
//
//

    /**
     * 添加新公会
     *
     * @param familyID    公会ID
     * @param name        公会名称
     * @param ownerID     会长ID
     * @param fencheng    分成
     * @param recommend   推荐代理ID
     * @param minTixian   最小提现
     * @param roomcardNum 房卡数量
     * @param higherLevel 给上级代理分成比
     * @param lowerLevel  给下级代理分成比
     * @param clubLevel   给亲友圈代理分成比
     * @return
     */
    public FamilyBO setFamily(long familyID, String name, long ownerID, int fencheng, long recommend, int minTixian, int roomcardNum
            , int higherLevel, int lowerLevel, int clubLevel, String beizhu, int vip, int power) {
        Family family = this.allFamilyMap.get(familyID);
        FamilyBO familyBO = null;
        boolean isInsert = false;
        if (null == family) {
            familyBO = new FamilyBO();
            // 如果是默认公会
            familyBO.setFamilyID(Family.DefaultFamilyID == familyID ? Family.DefaultFamilyID : CurrencyKeyMgr.getInstance().getNewKey());
            familyBO.setCreateTime(CommTime.nowSecond());
            familyBO.setName(name);
            isInsert = true;
        } else {
            familyBO = family.getFamilyBO();
            isInsert = false;
        }
        // 检查会长ID
        if (familyBO.getOwnerID() != ownerID) {
            // 获取玩家信息
            Player player = PlayerMgr.getInstance().getPlayer(ownerID);
            if (null != player) {
                if (player.getFamiliID() == Family.DefaultFamilyID || player.getFamiliID() <= 0) {
//					player.getFeature(PlayerCurrency.class).roomCardRefererReward(GameConfig.BindingFamilyReward());
                }
                player.getPlayerBO().saveFamilyID_sync(familyBO.getFamilyID());
            }
            familyBO.setOwnerID(ownerID);
        }
        familyBO.setFencheng(fencheng);
        familyBO.setRecommend(recommend);
        familyBO.setMinTixian(minTixian);
        familyBO.setRoomcardNum(roomcardNum);
        familyBO.setHigherLevel(higherLevel);
        familyBO.setLowerLevel(lowerLevel);
        familyBO.setClubLevel(clubLevel);
        familyBO.setBeizhu(beizhu);
        familyBO.setVip(vip);
        familyBO.setPower(power);
        if (isInsert) {
            familyBO.getBaseService().saveOrUpDate(familyBO);
            this.regFamily(new Family(familyBO));
        } else {
            familyBO.getBaseService().saveOrUpDate(familyBO);
            family.setFamilyBO(familyBO);
        }

        return familyBO;
    }


    /**
     * 设置公会房卡
     *
     * @param familyID
     * @param roomcardNum
     * @return
     */
    public boolean setFamilyRoomCard(long familyID, int roomcardNum) {
        Family family = this.allFamilyMap.get(familyID);
        if (null != family) {
            family.setRoomCard(roomcardNum);
            return true;
        } else {
            return false;
        }
    }


    /**
     * 删除公会
     */
    public void deleteFamily(long familyID) {
        Family family = this.allFamilyMap.remove(familyID);
        if (null != family) {
            family.getFamilyBO().getBaseService().delete(family.getFamilyBO().getId());
            CurrencyKeyMgr.getInstance().giveBackKey((int) familyID);
        }
    }

//	/**
//	 * 操作代理圈卡
//	 * @param familyID 代理ID
//	 * @param clubCard 圈卡
//	 * @param type 1:充值,2:撤回
//	 */
//	public boolean onFamilyClubCard (long familyID,int clubCard,int type) {
//		Family family = this.allFamilyMap.get(familyID);
//		if (null == family) {
//			return false;
//		}
//		return family.onClubCard(clubCard, type);
//	}
//
//
//	/**
//	 * 所有以存储工会
//	 *
//	 * @return
//	 */
//	public List<Family> getAllFamilys() {
//		return Lists.newArrayList(allFamilyMap.values());
//	}
//
//
//	/**
//	 * 所有以存储工会ID列表
//	 *
//	 * @return
//	 */
//	public List<Long> getAllFamilyIDList() {
//		return Lists.newArrayList(allFamilyMap.keySet());
//	}

}
