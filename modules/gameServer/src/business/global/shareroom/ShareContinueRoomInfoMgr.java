package business.global.shareroom;

import com.ddm.server.common.redis.RedisMap;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.room.ContinueRoomInfo;

/**
 * @author xsj
 * @date 2020/11/5 16:59
 * @description 共享继续房间管理类
 */
public class ShareContinueRoomInfoMgr {
    //房间存储KEy
    private static final String SHARE_CONTINUE_ROOM_INFO_KEY = "shareContinueRoomInfoKey";

    private static ShareContinueRoomInfoMgr instance = new ShareContinueRoomInfoMgr();

    // 获取单例
    public static ShareContinueRoomInfoMgr getInstance() {
        return instance;
    }


    /**
     * 增加共享继续房间
     *
     * @param continueRoomInfo
     */
    public void addShareContinueRoom(ContinueRoomInfo continueRoomInfo) {
        if (continueRoomInfo != null) {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CONTINUE_ROOM_INFO_KEY);
            redisMap.putJson(String.valueOf(continueRoomInfo.getRoomID()), continueRoomInfo);
        }
    }


    /**
     * 获取共享房间信息
     *
     * @param roomId
     * @return
     */
    public ContinueRoomInfo getShareContinueRoom(Long roomId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CONTINUE_ROOM_INFO_KEY);
        ContinueRoomInfo continueRoomInfo = redisMap.getObject(String.valueOf(roomId), ContinueRoomInfo.class);
        return continueRoomInfo;
    }

    /**
     * 删除共享房间
     *
     * @param roomId
     */
    public void removeShareContinueRoom(Long roomId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CONTINUE_ROOM_INFO_KEY);
        redisMap.remove(String.valueOf(roomId));
    }

}
