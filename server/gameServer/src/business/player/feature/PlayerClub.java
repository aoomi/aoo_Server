package business.player.feature;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import business.shareplayer.SharePlayerMgr;
import cenum.ConstEnum.RechargeType;
import cenum.ConstEnum.ResOpType;
import cenum.ItemFlow;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import core.config.refdata.ref.RefRoomCost;
import core.db.entity.clarkGame.PlayerClubBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerClubBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.SPlayer_ChangeClubCard;

import java.util.HashMap;
import java.util.List;


public class PlayerClub extends Feature {
    private PlayerClubBOService playerClubBOService;
    private HashMap<Long, PlayerClubBO> pClubMap = new HashMap<>();
    // 圈卡之和
    private int sumClubCard = 0;

    public PlayerClub(Player player) {
        super(player);
        playerClubBOService = ContainerMgr.get().getComponent(PlayerClubBOService.class);
    }

    @Override
    public void loadDB() {
        List<PlayerClubBO> pClubBOs = playerClubBOService.findAll(Restrictions.eq("pid", this.getPid()), "");
        if (null == pClubBOs || pClubBOs.size() <= 0) {
            return;
        }
        for (PlayerClubBO pBo : pClubBOs) {
            // 求玩家身上圈卡总和
            this.sumClubCard += pBo.getClubRoomCard();
            this.pClubMap.put(pBo.getAgentsID(), pBo);
        }
    }

    /**
     * 求玩家身上总圈卡
     *
     * @return
     */
    public int sumClubCard() {
        // 检查是否有圈卡
        if (null == pClubMap || pClubMap.size() <= 0) {
            return 0;
        }
        // 求和圈卡数
        int sum = this.pClubMap.values().stream().mapToInt(PlayerClubBO::getClubRoomCard).sum();
        // 结果
        return sum;
    }

    /**
     * 获取玩家对应指定代理的圈卡
     *
     * @param agentsID 俱乐部代理ID
     * @param level    俱乐部代理等级
     * @return
     */
    public int getPlayerClubRoomCard(long agentsID, int level) {
        PlayerClubBO playerClubBO = this.getPlayerClubBO(agentsID, level);
        if (null == playerClubBO) {
            return 0;
        } else {
            return playerClubBO.getClubRoomCard();
        }
    }


    /**
     * 获取玩家圈卡数据
     *
     * @param agentsID 俱乐部代理ID
     * @param level    俱乐部代理等级
     * @return
     */
    public PlayerClubBO getPlayerClubBO(long agentsID, int level) {
        // 如果不是代理
        if ((agentsID == 0 && level == 0) || level == 3) {
            PlayerClubBO playerClubBO = this.pClubMap.get(agentsID);
            if (null == playerClubBO) {
                playerClubBO = new PlayerClubBO();
                playerClubBO.setPid(this.getPid());
                playerClubBO.setAgentsID(agentsID);
                playerClubBO.setLevel(level);
                playerClubBO.getBaseService().saveOrUpDate(playerClubBO);
                this.pClubMap.put(agentsID, playerClubBO);
            }
            return playerClubBO;
        } else {
            return null;
        }


    }

    /**
     * 检查房卡是否足够
     *
     * @param count    数量
     * @param clubID   俱乐部ID
     * @param gameType 游戏类型
     * @return
     */
    public boolean checkClubCard(int count, long clubID) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        // 检查俱乐部数据是否存在
        if (null == club) {
            CommLogD.error("checkAndConsume null == club clubID:{}", clubID);
            return false;
        }
        int clubCard = this.getPlayerClubRoomCard(club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel());
        // 玩家指定代理旗下的圈卡是否足够
        if (clubCard < count) {
            return false;
        }
        return true;
    }


    /**
     * 检查俱乐部圈卡并消耗
     *
     * @param bRoomConfigure 配置
     * @param reason         消耗类型
     * @return
     */
    @SuppressWarnings("rawtypes")
    public boolean checkClubCardAndConsume(BaseRoomConfigure bRoomConfigure, ItemFlow reason) {
        // 公共房间配置
        if (null == bRoomConfigure) {
            return false;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(bRoomConfigure.getBaseCreateRoom().getClubId());
        // 检查俱乐部数据是否存在
        if (null == club) {
            CommLogD.error("checkAndConsume null == club clubID:{}", bRoomConfigure.getBaseCreateRoom().getClubId());
            return false;
        }
        // 获取相应的房卡消耗值
        int value = bRoomConfigure.getBaseCreateRoom().getClubWinnerPayConsume();
        SData_Result result = RefRoomCost.GetCost(bRoomConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return false;
        }
        // 获取消耗
        int card = (int) result.getCustom();
        // 计算出的房卡<=0
        if (card <= 0) {
            CommLogD.error("checkAndConsume card <= 0 clubID:{}", bRoomConfigure.getBaseCreateRoom().getClubId());
            return false;
        }
        value = card;

        // 获取玩家的圈卡数
        int clubCard = this.getPlayerClubRoomCard(club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel());
        // 玩家指定代理旗下的圈卡是否足够
        if (clubCard < value) {
            return false;
        }
        // 圈卡临时消耗-游戏日志
        return this.consumeRoomCard(value, bRoomConfigure.getGameType().getId(), ResOpType.Lose, RechargeType.Not, ClubCardEnum.NOT_ROOM_ID.value(), club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel(), bRoomConfigure.getBaseCreateRoom().getClubId(), false, reason);
    }


    /**
     * 返回玩家圈卡
     *
     * @param consumeCard 返回圈卡数量
     * @param gameType    游戏类型
     * @param clubID      俱乐部ID
     * @param reason
     */
    public void clubCardReturnCradRoom(int consumeCard, GameType gameType, long clubID, ItemFlow reason) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        // 检查俱乐部数据是否存在
        if (null == club) {
            CommLogD.error("returnCradRoom null == club clubID:{}", clubID);
            return;
        }
        // 获得圈卡-游戏日志
        this.gainRoomCard(consumeCard, gameType.getId(), ResOpType.Fallback, RechargeType.Not, ClubCardEnum.NOT_ROOM_ID.value(), club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel(), clubID, false, reason);
    }

    /**
     * 返回玩家圈卡 - 大赢家付
     *
     * @param consumeCard 返回圈卡数量
     * @param gameType    游戏类型
     * @param clubID      俱乐部ID
     * @param several     分摊房卡人数
     */
    public void clubCardReturnCradRoom(int consumeCard, GameType gameType, long clubID, int several, long roomID, ItemFlow reason) {
        if (several == 0) {
            // 返回玩家房卡
            clubCardReturnCradRoom(consumeCard, gameType, clubID, reason);
            return;
        }
        int value = consumeCard - (int) Math.ceil(consumeCard * 1.0 / several);
        // 计算实际消耗房卡
        int finalValue = consumeCard - value;
        this.returnClubCardConsumeLog(value, gameType, clubID, finalValue, roomID, reason);
    }

    /**
     * 返回圈卡
     * 计算出圈卡实际消耗
     *
     * @param consumeCard 返回圈卡数量
     * @param gameType    游戏类型
     * @param clubID      俱乐部ID
     * @param value       消耗值
     * @param roomID      房间号
     * @param reason
     */
    public void returnClubCardConsumeLog(int consumeCard, GameType gameType, long clubID, int value, long roomID, ItemFlow reason) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        // 检查俱乐部数据是否存在
        if (null == club) {
            CommLogD.error("returnCradRoomConsume null == club clubID:{}", clubID);
            return;
        }
        // 获得圈卡
        this.gainRoomCard(consumeCard, gameType.getId(), ResOpType.Fallback, RechargeType.Not, ClubCardEnum.NOT_ROOM_ID.value(), club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel(), clubID, false, reason);
    }


    /**
     * 俱乐部奖励
     *
     * @param agentsID 俱乐部代理ID
     * @param level    俱乐部代理等级
     * @param clubID   俱乐部ID
     * @param value    奖励值
     */
    public void onClubReward(long agentsID, int level, long clubID, int value, ItemFlow reason) {
        // 获得圈卡
        this.gainRoomCard(value, ClubCardEnum.NOT_GAME_TYPE.value(), ResOpType.Gain, RechargeType.Reward, ClubCardEnum.NOT_ROOM_ID.value(), agentsID, level, clubID, true, reason);
    }

    /**
     * 俱乐部圈卡充值
     *
     * @param clubID       俱乐部ID
     * @param value        值
     * @param rechargeType 充值类型
     */
    public void onClubCardRecharge(long agentsID, int level, long clubID, int value, RechargeType rechargeType, ItemFlow reason) {
        // 获得圈卡
        this.gainRoomCard(value, ClubCardEnum.NOT_GAME_TYPE.value(), ResOpType.Gain, rechargeType, ClubCardEnum.NOT_ROOM_ID.value(), agentsID, level, clubID, true, reason);
    }

    /**
     * 管理员操作玩家圈卡
     *
     * @param value    值
     * @param familyID 代理ID
     * @param type     1:充值,2:撤回
     */
    public boolean onAdminClubCard(int value, long familyID, int type, RechargeType rechargeType, ItemFlow reason) {
        if (type == 1) {
            // 获得圈卡
            this.gainRoomCard(
                    value,
                    ClubCardEnum.NOT_GAME_TYPE.value(),
                    ResOpType.Gain,
                    rechargeType,
                    ClubCardEnum.NOT_ROOM_ID.value(),
                    familyID,
                    familyID == 0 ? 0 : ClubCardEnum.FAMILY_LEVEL.value(),
                    ClubCardEnum.NOT_CLUB_ID.value(), true, reason);
            return true;
        } else if (type == 2) {
            // 消耗圈卡
            this.consumeRoomCard(
                    value,
                    ClubCardEnum.NOT_GAME_TYPE.value(),
                    ResOpType.Lose,
                    rechargeType,
                    ClubCardEnum.NOT_ROOM_ID.value(),
                    familyID,
                    familyID == 0 ? 0 : ClubCardEnum.FAMILY_LEVEL.value(),
                    ClubCardEnum.NOT_CLUB_ID.value(), true, reason);
            return true;
        } else {
            return false;
        }
    }


    /**
     * 获得圈卡
     *
     * @param value        值
     * @param gameType     游戏类型
     * @param resOpType    操作类型
     * @param rechargeType 充值类型
     * @param roomID       房间ID
     * @param agentsID     俱乐部代理ID
     * @param level        俱乐部代理等级
     * @param clubID       俱乐部ID
     * @return
     */
    private int gainRoomCard(int value, int gameType, ResOpType resOpType, RechargeType rechargeType, long roomID, long agentsID, int level, long clubID, boolean isLog, ItemFlow reason) {
        if (value <= 0) {
            return 0;
        }
        // 获取玩家圈卡数据
        PlayerClubBO playerClubBO = this.getPlayerClubBO(agentsID, level);
        if (null == playerClubBO) {
            return 0;
        }
        this.lock();
        int before = 0;
        int finalValue = 0;
        before = playerClubBO.getClubRoomCard();
        finalValue = Math.min(1999999999, before + value);
        // 计算圈卡之和
        this.sumClubCard = Math.min(1999999999, this.sumClubCard + value);
        // 获得奖励房卡
        playerClubBO.saveClubRoomCard(finalValue);
        this.unlock();
        CommLogD.info("==获得圈卡== 玩家：{},消耗房卡数量：{},类型：{},充值方式：{}", player.getId(), value, gameType, rechargeType);
        this.player.pushProperties("clubCard", this.sumClubCard);
        this.player.pushProto(SPlayer_ChangeClubCard.make(this.getPid(), playerClubBO.getClubRoomCard(), agentsID, level, clubID));
        // 日志:获得房卡
        FlowLogger.playerClubCardLog(
                this.getPid(),
                value,
                finalValue,
                before,
                resOpType.ordinal(),
                gameType,
                rechargeType.ordinal(),
                roomID,
                agentsID,
                level,
                clubID,
                reason.value(),
                0);
        return finalValue - before;
    }

    /**
     * 消耗圈卡
     *
     * @param value        值
     * @param gameType     游戏类型
     * @param resOpType    操作类型
     * @param rechargeType 充值类型
     * @param roomID       房间ID
     * @param agentsID     俱乐部代理ID
     * @param level        俱乐部代理等级
     * @param clubID       俱乐部ID
     * @return
     */
    private boolean consumeRoomCard(int value, int gameType, ResOpType resOpType, RechargeType rechargeType, long roomID, long agentsID, int level, long clubID, boolean isLog, ItemFlow reason) {
        if (value <= 0) {
            return false;
        }
        // 获取玩家圈卡数据
        PlayerClubBO playerClubBO = this.getPlayerClubBO(agentsID, level);
        if (null == playerClubBO) {
            return false;
        }
        this.lock();
        int before = 0;
        int finalValue = 0;
        before = playerClubBO.getClubRoomCard();
        finalValue = Math.max(0, before - value);
        // 计算圈卡之和
        this.sumClubCard = Math.max(0, this.sumClubCard - value);
        // 消耗平台房卡
        playerClubBO.saveClubRoomCard(finalValue);
        this.unlock();
        CommLogD.info("==消耗圈卡== 玩家：{},消耗房卡数量：{},类型：{},充值方式：{}", player.getId(), value, gameType, rechargeType);
        this.player.pushProperties("clubCard", this.sumClubCard);
        this.player.pushProto(SPlayer_ChangeClubCard.make(this.getPid(), playerClubBO.getClubRoomCard(), agentsID, level, clubID));
        FlowLogger.playerClubCardLog(
                this.getPid(),
                -value,
                finalValue,
                before,
                resOpType.ordinal(),
                gameType,
                rechargeType.ordinal(),
                roomID,
                agentsID,
                level,
                clubID,
                reason.value(),
                0);
        // 日志:消耗房卡
        if (!isLog) {
            this.player.getRoomInfo().setConsumeCard(value);
            //共享数据
            if (Config.isShare()) {
                SharePlayerMgr.getInstance().updateField(this.getPlayer(), "roomInfo");
            }
        }
        return true;
    }

//	/**
//	 * 获取总圈卡数
//	 * @return
//	 */
//	public int getSumClubCard() {
//		return this.sumClubCard;
//	}
//

    /**
     * 圈卡枚举
     *
     * @author Administrator
     */
    enum ClubCardEnum {
        // 没有游戏类型
        NOT_GAME_TYPE(-1),
        // 没有房间ID
        NOT_ROOM_ID(0),
        // 代理等级
        FAMILY_LEVEL(3),
        // 没有俱乐部ID
        NOT_CLUB_ID(0),
        ;
        private int value;

        private ClubCardEnum(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }


}
