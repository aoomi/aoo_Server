package business.global.shareclub;

import BaseCommon.CommLog;
import business.global.club.Club;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.gson.Gson;
import core.db.entity.clarkGame.ClubListBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.ClubCreateGameSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xsj
 * @date 2020/8/20 15:11
 * @description 共享亲友圈管理类
 */
public class ShareClubListMgr {
    private static final String SHARE_CLUB_LIST_KEY = "shareClubListKey";
    private static ShareClubListMgr instance = new ShareClubListMgr();

    // 获取单例
    public static ShareClubListMgr getInstance() {
        return instance;
    }

    /**
     * 添加共享亲友圈
     *
     * @param club)
     */
    public void addClub(Club club) {
//        CommLogD.info("club修改内容[{}]", new Gson().toJson(club));
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_LIST_KEY);
        redisMap.putJson(String.valueOf(club.getClubListBO().getId()), club);
    }

    /**
     * 删除亲友圈信息
     *
     * @param memberId
     */
    public void deleteClub(Long memberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_LIST_KEY);
        redisMap.removeObject(String.valueOf(memberId));
    }

    /**
     * 获取所有亲友圈
     *
     * @return
     */
    public Map<Long, Club> getAllClub() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_LIST_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, Club> clubMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    Club club = gson.fromJson(map.getValue(), Club.class);
                    clubMembers.put(Long.parseLong(map.getKey()), club);
                } catch (Exception e) {
                    System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                    CommLog.error(e.getMessage(), e);
                }
            });
            return clubMembers;
        } else {
            return new ConcurrentHashMap<>(1);
        }
    }

    /**
     * 获取一个亲友圈
     *
     * @param clubId)
     */
    public Club getClub(Long clubId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_LIST_KEY);
        return redisMap.getObject(String.valueOf(clubId), Club.class);
    }

    /**
     * 添加房间数
     *
     * @param clubId
     * @param gameIndex
     */
    public void addRoomCount(Long clubId, Long gameIndex) {
        Club club = getClub(clubId);
        ClubCreateGameSet clubCreateGameSet = club.getMCreateGamesetMap().get(gameIndex);
        if (clubCreateGameSet != null) {
            clubCreateGameSet.addRoomCount();
            addClub(club);
        }
    }

    /**
     * 减少房间数
     *
     * @param clubId
     * @param gameIndex
     */
    public void subRoomCount(Long clubId, Long gameIndex) {
        Club club = getClub(clubId);
        ClubCreateGameSet clubCreateGameSet = club.getMCreateGamesetMap().get(gameIndex);
        if (clubCreateGameSet != null) {
            clubCreateGameSet.subRoomCount();
            addClub(club);
        }
    }

    /**
     * 更新共享亲友圈字段值
     *
     * @param club
     * @param fields
     * @return
     */
    public void updateField(Club club, String... fields) {
        Club clubOld = getClub(club.getClubListBO().getId());
        if (clubOld == null) {
            addClub(club);
        } else {
            for (String field : fields) {
                Object value = PropertiesUtil.invokeGet(club, field);
                PropertiesUtil.invokeSet(clubOld, field, value);
            }
            addClub(clubOld);
        }
    }

    /**
     * 更新共享亲友圈字段值
     *
     * @param clubListBO
     * @param fields
     * @return
     */
    public void updateClubListField(ClubListBO clubListBO, String... fields) {
        Club clubOld = getClub(clubListBO.getId());
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(clubListBO, field);
            PropertiesUtil.invokeSet(clubOld.getClubListBO(), field, value);
        }
        addClub(clubOld);

    }
}
