package business.global.sharegm;

import BaseCommon.CommLog;
import com.ddm.server.common.redis.RedisMap;
import com.google.gson.Gson;
import core.db.entity.clarkGame.MaintainGameBO;
import core.ioc.ContainerMgr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xsj
 * @date 2020/8/28 16:15
 * @description 共享游戏维护信息
 */
public class ShareMaintainGameMgr {
    private static ShareMaintainGameMgr instance = new ShareMaintainGameMgr();

    //游戏维护KEy
    private static final String SHARE_MAINTAIN_GAME_KEY = "shareMaintainGameKey";

    public static ShareMaintainGameMgr getInstance() {
        return instance;
    }

    /**
     * 添加维护游戏
     *
     * @param maintainGameBO
     */
    public void addMaintainGame(MaintainGameBO maintainGameBO) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_MAINTAIN_GAME_KEY);
        redisMap.put(String.valueOf(maintainGameBO.getGameTypeId()), new Gson().toJson(maintainGameBO));
    }

    /**
     * 获取维护游戏
     *
     * @param gameTypeId
     * @return
     */
    public MaintainGameBO getMaintainGame(Integer gameTypeId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_MAINTAIN_GAME_KEY);
        if (redisMap.containsKey(String.valueOf(gameTypeId))) {
            String result = redisMap.get(String.valueOf(gameTypeId));
            return new Gson().fromJson(result, MaintainGameBO.class);
        }
        return null;
    }

    /**
     * 获取所有维护的游戏信息
     *
     * @return
     */
    public List<MaintainGameBO> allMaintainGames() {
        Gson gson = new Gson();
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_MAINTAIN_GAME_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        List<MaintainGameBO> maintainGames = new ArrayList<>(allSet.size());
        allSet.forEach((k) -> {
            try {
                MaintainGameBO maintainGameBO = gson.fromJson(k.getValue(), MaintainGameBO.class);
                maintainGames.add(maintainGameBO);
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                CommLog.error(e.getMessage(), e);
            }
        });
        return maintainGames;
    }

}
