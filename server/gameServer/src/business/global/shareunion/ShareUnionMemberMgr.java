package business.global.shareunion;

import BaseCommon.CommLog;
import business.global.union.UnionMember;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionMemberBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.union.UnionDefine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xsj
 * @date 2020/8/20 15:11
 * @description 共享赛事成员管理类
 */
public class ShareUnionMemberMgr {
    private static final String SHARE_UNION_MEMBER_KEY = "shareUnionMemberKey";
    private static final String SHARE_ONE_UNION_MEMBER_KEY = "shareOneUnionMemberKey";
    private static final String SHARE_ONE_CLUB_UNION_MEMBER_KEY = "shareOneClubUnionMemberKey";
    private static ShareUnionMemberMgr instance = new ShareUnionMemberMgr();

    // 获取单例
    public static ShareUnionMemberMgr getInstance() {
        return instance;
    }

    /**
     * 添加共享俱乐部成员
     *
     * @param unionMember
     */
    public void addClubMember(UnionMember unionMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_MEMBER_KEY);
        redisMap.putJson(String.valueOf(unionMember.getUnionMemberBO().getId()), unionMember);
        addOneUnionMember(unionMember);
        addOneClubUnionMember(unionMember);
    }

    /**
     * 添加一个赛事共享俱乐部成员
     *
     * @param unionMember
     */
    public void addOneUnionMember(UnionMember unionMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_MEMBER_KEY + unionMember.getUnionMemberBO().getUnionId());
        redisMap.putJson(String.valueOf(unionMember.getUnionMemberBO().getId()), unionMember);
    }

    /**
     * 添加一个赛事共享俱乐部成员
     *
     * @param unionMember
     */
    public void addOneClubUnionMember(UnionMember unionMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_UNION_MEMBER_KEY + unionMember.getUnionMemberBO().getClubId());
        redisMap.putJson(String.valueOf(unionMember.getUnionMemberBO().getId()), unionMember);
    }

    /**
     * 删除成员信息
     *
     * @param memberId
     */
    public void deleteClubMember(Long memberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_MEMBER_KEY);
        UnionMember unionMember = redisMap.getObject(String.valueOf(memberId), UnionMember.class);
        redisMap.removeObject(String.valueOf(memberId));
        if (unionMember != null) {
            deleteOneUnionMember(unionMember);
            deleteOneClubUnionMember(unionMember);
        }
    }

    /**
     * 删除一个赛事成员信息
     *
     * @param unionMember
     */
    public void deleteOneUnionMember(UnionMember unionMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_MEMBER_KEY + unionMember.getUnionMemberBO().getUnionId());
        redisMap.removeObject(String.valueOf(unionMember.getUnionMemberBO().getId()));
    }

    /**
     * 删除一个赛事成员信息
     *
     * @param unionMember
     */
    public void deleteOneClubUnionMember(UnionMember unionMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_UNION_MEMBER_KEY + unionMember.getUnionMemberBO().getClubId());
        redisMap.removeObject(String.valueOf(unionMember.getUnionMemberBO().getId()));
    }

    /**
     * 获取所有成员
     *
     * @return
     */
    public Map<Long, UnionMember> getAllUnionMember() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_MEMBER_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, UnionMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    UnionMember unionMember = gson.fromJson(map.getValue(), UnionMember.class);
                    clubMembers.put(Long.parseLong(map.getKey()), unionMember);
                } catch (Exception e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
            return clubMembers;
        } else {
            return new ConcurrentHashMap<>(1);
        }
    }

    /**
     * 获取一个赛事所有成员
     *
     * @return
     */
    public Map<Long, UnionMember> getAllOneUnionMember(Long unionId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_MEMBER_KEY + unionId);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, UnionMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    UnionMember unionMember = gson.fromJson(map.getValue(), UnionMember.class);
                    clubMembers.put(Long.parseLong(map.getKey()), unionMember);
                } catch (Exception e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
            return clubMembers;
        } else {
            return new ConcurrentHashMap<>(1);
        }
    }

    /**
     * 获取一个亲友圈赛事所有成员
     *
     * @return
     */
    public Map<Long, UnionMember> getAllOneClubUnionMember(Long clubId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_UNION_MEMBER_KEY + clubId);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, UnionMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    UnionMember unionMember = gson.fromJson(map.getValue(), UnionMember.class);
                    clubMembers.put(Long.parseLong(map.getKey()), unionMember);
                } catch (Exception e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
            return clubMembers;
        } else {
            return new ConcurrentHashMap<>(1);
        }
    }

//    /**
//     * 查询创建者亲友圈成员id
//     *
//     * @param unionId
//     * @return
//     */
//    public long findCreateClubMemberId(long unionId) {
//        return this.getAllUnionMember().values().stream()
//                .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getType() == UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubMemberId()).findAny().orElse(0L);
//    }

    /**
     * 判断成员是否存在
     *
     * @param unionMemberId
     */
    public boolean existUnionMember(Long unionMemberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_MEMBER_KEY);
        return redisMap.containsKey(String.valueOf(unionMemberId));
    }

    /**
     * 更新共享成员字段值
     *
     * @param unionMemberBO
     * @param fields
     * @return
     */
    public void updateField(UnionMemberBO unionMemberBO, String... fields) {
        UnionMember unionMember = this.getUnionMember(unionMemberBO.getId());
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(unionMemberBO, field);
            PropertiesUtil.invokeSet(unionMember.getUnionMemberBO(), field, value);
        }
        addClubMember(unionMember);
    }

    /**
     * 更新共享成员字段值
     *
     * @param unionMemberBO
     * @param mapFields
     * @return
     */
    public void updateField(UnionMemberBO unionMemberBO, Map<String, Object> mapFields) {
        UnionMember unionMember = this.getUnionMember(unionMemberBO.getId());
        mapFields.forEach((k, v) -> {
            Object value = PropertiesUtil.invokeGet(unionMemberBO, k);
            PropertiesUtil.invokeSet(unionMember.getUnionMemberBO(), k, value);
        });
        addClubMember(unionMember);
    }

    /**
     * 获取成员信息
     *
     * @param id
     * @return
     */
    public UnionMember getUnionMember(Long id) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_UNION_MEMBER_KEY);
        UnionMember unionMember = redisMap.getObject(String.valueOf(id), UnionMember.class);
        return unionMember;
    }

    /**
     * 获取赛事成员*
     *
     * @param unionId 赛事ID
     * @param status  状态
     * @return
     */
    public UnionMember find(long clubId, long unionId, UnionDefine.UNION_PLAYER_STATUS status) {
        return this.getAllOneUnionMember(unionId).values().stream()
                .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getStatus(status.value())).findAny()
                .orElse(null);
    }
}
