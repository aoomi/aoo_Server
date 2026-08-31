package business.global.shareunion;

import BaseCommon.CommLog;
import business.global.union.Union;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.union.UnionCreateGameSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xsj
 * @date 2020/8/20 15:11
 * @description 共享赛事管理类
 */
public class ShareUnionListMgr {
    private static final String SHARE_UNION_LIST_KEY = "shareUnionListKey";
    private static ShareUnionListMgr instance = new ShareUnionListMgr();

    // 获取单例
    public static ShareUnionListMgr getInstance() {
        return instance;
    }

    /**
     * 添加共享赛事
     *
     * @param union)
     */
    public void addUnion(Union union) {
//        CommLogD.info("union修改内容[{}]", new Gson().toJson(union));
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_LIST_KEY);
        redisMap.putJson(String.valueOf(union.getUnionBO().getId()), union);
    }

    /**
     * 删除赛事信息
     *
     * @param memberId
     */
    public void deleteUnion(Long memberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_LIST_KEY);
        redisMap.removeObject(String.valueOf(memberId));
    }

    /**
     * 获取联盟数量
     *
     * @return
     */
    public int getAllUnionSize() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_LIST_KEY);
        return redisMap.size();
    }

    /**
     * 获取所有赛事
     *
     * @return
     */
    public Map<Long, Union> getAllUnion() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_LIST_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, Union> UnionMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    Union union = gson.fromJson(map.getValue(), Union.class);
                    UnionMembers.put(Long.parseLong(map.getKey()), union);
                } catch (Exception e) {
                    System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                    CommLog.error(e.getMessage(), e);
                }
            });
            return UnionMembers;
        } else {
            return new ConcurrentHashMap<>(1);
        }
    }

    /**
     * 获取一个赛事
     *
     * @param UnionId)
     */
    public Union getUnion(Long UnionId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_LIST_KEY);
        Union union = redisMap.getObject(String.valueOf(UnionId), Union.class);
        return union;
    }

    /**
     * 添加房间数
     *
     * @param unionId
     * @param gameIndex
     */
    public void addRoomCount(Long unionId, Long gameIndex) {
        Union union = getUnion(unionId);
        UnionCreateGameSet clubCreateGameSet = union.getRoomConfigBOMap().get(gameIndex);
        if (clubCreateGameSet != null) {
            clubCreateGameSet.addRoomCount();
            addUnion(union);
        }
    }

    /**
     * 减少房间数
     *
     * @param unionId
     * @param gameIndex
     */
    public void subRoomCount(Long unionId, Long gameIndex) {
        Union union = getUnion(unionId);
        UnionCreateGameSet clubCreateGameSet = union.getRoomConfigBOMap().get(gameIndex);
        if (clubCreateGameSet != null) {
            clubCreateGameSet.subRoomCount();
            addUnion(union);
        }
    }

    /**
     * 更新共享赛事字段值
     *
     * @param union
     * @param fields
     * @return
     */
    public void updateField(Union union, String... fields) {
        Union unionOld = getUnion(union.getUnionBO().getId());
        if (unionOld == null) {
            addUnion(union);
        } else {
            for (String field : fields) {
                Object value = PropertiesUtil.invokeGet(union, field);
                PropertiesUtil.invokeSet(unionOld, field, value);
            }
            addUnion(unionOld);
        }
    }

    /**
     * 更新共享赛事字段值
     *
     * @param unionBO
     * @param fields
     * @return
     */
    public void updateUnionBoField(UnionBO unionBO, String... fields) {
        Union unionOld = getUnion(unionBO.getId());
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(unionBO, field);
            PropertiesUtil.invokeSet(unionOld.getUnionBO(), field, value);
        }
        addUnion(unionOld);
    }

    /**
     * 更新共享赛事字段值
     *
     * @param unionBO
     * @param mapFields
     * @return
     */
    public void updateUnionBoMap(UnionBO unionBO, Map<String, Object> mapFields) {
        Union unionOld = getUnion(unionBO.getId());
        mapFields.forEach((k, v) -> {
            Object value = PropertiesUtil.invokeGet(unionBO, k);
            PropertiesUtil.invokeSet(unionOld.getUnionBO(), k, value);
        });
        addUnion(unionOld);
    }
}
