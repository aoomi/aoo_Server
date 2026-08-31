package business.global.GM;

import BaseThread.BaseMutexManager;
import business.global.sharegm.ShareMaintainGameMgr;
import business.player.Player;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.MaintainGameBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.MaintainGameBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护游戏管理器
 *
 * @author xushaojun
 */
@Data
public class MaintainGameMgr {
    private static MaintainGameMgr instance = new MaintainGameMgr();
    private final BaseMutexManager _lock = new BaseMutexManager();
    private final Map<String, Long> notifyPlayerMap = new ConcurrentHashMap<>();


    public static MaintainGameMgr getInstance() {
        return instance;
    }

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * 设置游戏维护内容
     * 后台设置
     *
     * @param maintainGameBO
     */
    public void saveMaintainGame(MaintainGameBO maintainGameBO) {
        MaintainGameBO gameBO = ShareMaintainGameMgr.getInstance().getMaintainGame(maintainGameBO.getGameTypeId());
        if (gameBO != null) {
            maintainGameBO.setId(gameBO.getId());
        }
        maintainGameBO.getBaseService().saveOrUpDate(maintainGameBO);
        ShareMaintainGameMgr.getInstance().addMaintainGame(maintainGameBO);
    }

    /**
     * 获取游戏维护内容
     *
     * @param gameTypeId
     * @return
     */
    public String getMaintainGameContent(Integer gameTypeId) {
        MaintainGameBO gameBO = ShareMaintainGameMgr.getInstance().getMaintainGame(gameTypeId);
        if (gameBO == null) {
            gameBO = ContainerMgr.get().getComponent(MaintainGameBOService.class).findOne(Restrictions.eq("gameTypeId", gameTypeId), null);
            if (gameBO != null) {
                ShareMaintainGameMgr.getInstance().addMaintainGame(gameBO);
            }
        }
        return gameBO != null ? gameBO.getContent() : "";
    }

    /**
     * 获取游戏维护对象
     *
     * @param gameTypeId
     * @return
     */
    public MaintainGameBO getMaintainGame(Integer gameTypeId) {
        MaintainGameBO gameBO = ShareMaintainGameMgr.getInstance().getMaintainGame(gameTypeId);
        if (gameBO == null) {
            gameBO = ContainerMgr.get().getComponent(MaintainGameBOService.class).findOne(Restrictions.eq("gameTypeId", gameTypeId), null);
            if (gameBO != null) {
                ShareMaintainGameMgr.getInstance().addMaintainGame(gameBO);
            }
        }
        return gameBO;
    }

    /**
     * 获取维护信息通知
     *
     * @param gameTypeId
     * @return
     */
    public MaintainGameBO getMaintainGameToNotify(Integer gameTypeId, Long pid) {
        MaintainGameBO bo = getMaintainGame(gameTypeId);
        if (checkMaintainGameStatus(bo)) {
            if (notifyPlayerMap.get(bo.getGameTypeId() + "_" + pid) == null) {
                return bo;
            }
        }
        return null;
    }

    /**
     * 获取游戏维护对象
     *
     * @param gameTypeId
     * @return
     */
    public void notifyFinish(Integer gameTypeId, Long pid) {
        MaintainGameBO bo = getMaintainGame(gameTypeId);
        if (checkMaintainGameStatus(bo)) {
            notifyPlayerMap.put(gameTypeId + "_" + pid, pid);
        }
    }

    /**
     * 检查是否维护中
     * 如果维护中，只有Gm 用户可以进入游戏，进行游戏测试。
     *
     * @param player 玩家
     * @return
     */
    public SData_Result checkMaintainGame(Integer gameTypeId, Player player) {
        MaintainGameBO maintainGameBO = getMaintainGame(gameTypeId);
        //在维护时间内
        if (Objects.nonNull(maintainGameBO) && CommTime.checkTimeIntervale(maintainGameBO.getStartTime(), maintainGameBO.getEndTime())) {
            //检查玩家是否有权限进入游戏
            if (Objects.isNull(player) && player.isGmLevel()) {
                return SData_Result.make(ErrorCode.Success);
            } else {
                switch (maintainGameBO.getStatus()) {
                    //没有维护
                    case 0:
                        return SData_Result.make(ErrorCode.Success);
                    //维护中
                    case 1:
                        return SData_Result.make(ErrorCode.Game_Maintain, maintainGameBO);
                }
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 检查能不能继续游戏
     *
     * @param gameTypeId
     * @return
     */
    public Boolean checkContinueGame(Integer gameTypeId) {
        MaintainGameBO maintainGameBO = getMaintainGame(gameTypeId);
        //在维护时间内
        if (Objects.nonNull(maintainGameBO) && CommTime.checkTimeIntervale(maintainGameBO.getStartTime(), maintainGameBO.getEndTime())) {
            switch (maintainGameBO.getStatus()) {
                //没有维护
                case 0:
                    return true;
                //有维护
                case 1:
                    return false;
            }
        }
        return true;
    }

    /**
     * 获取维护列表
     *
     * @return
     */
    public List<MaintainGameBO> listMaintainGameBO() {
        return ShareMaintainGameMgr.getInstance().allMaintainGames();
    }

    /**
     * 通知游戏维护的所有在线玩家
     */
    public void notifyThisGameAll() {
        List<MaintainGameBO> list = listMaintainGameBO();
        for (MaintainGameBO bo : list) {
            //维护消息一个小时开始通知
            if (checkMaintainGameStatus(bo)) {
//                Gson gson = new Gson();
//                PlayerMgr.getInstance().getOnlinePlayers().parallelStream().forEach(k -> {
//                    if (notifyPlayerMap.get(bo.getGameTypeId() + "_" + k.getPid()) == null) {
//                        notifyPlayerMap.put(bo.getGameTypeId() + "_" + k.getPid(), k.getPid());
//                        k.pushProto(SGame_MaintainNotice.make(gson.fromJson(gson.toJson(bo), MaintainGameInfo.class)));
//                    }
//                });
            } else {
                notifyPlayerMap.forEach((k, v) -> {
                    if (k.startsWith(bo.getGameTypeId() + "_")) {
                        notifyPlayerMap.remove(k);
                    }
                });
            }
        }
    }

    /**
     * 检查游戏维护状态提前1小时通知维护消息
     *
     * @param bo
     * @return
     */
    private boolean checkMaintainGameStatus(MaintainGameBO bo) {
        if (bo != null && bo.getStatus() == 1 && bo.getStartTime() <= (CommTime.nowSecond() + 3600) && bo.getStartTime() > CommTime.nowSecond()) {
            return true;
        } else {
            return false;
        }
    }

}
