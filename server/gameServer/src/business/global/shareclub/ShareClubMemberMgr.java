package business.global.shareclub;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.rocketmq.bo.MqClubMemberBo;
import business.rocketmq.constant.MqTopic;
import business.utils.ClubMemberUtils;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.DistributedRedisLock;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.GetStackUtils;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import core.db.entity.clarkGame.ClubMemberBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.Club_define;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author xsj
 * @date 2020/8/20 15:11
 * @description 共享亲友圈成员管理类
 */
public class ShareClubMemberMgr {
    private static final String SHARE_CLUB_MEMBER_KEY = "shareClubMemberKey";
    private static final String SHARE_ONE_CLUB_MEMBER_KEY = "shareOneClubMemberKey";
    private static final String SHARE_ONE_PLAYER_CLUB_MEMBER_KEY = "shareOnePlayerClubMemberKey";
    private static final String SHARE_ONE_CLUB_MEMBER_ARRAY_KEY = "shareOneClubMemberArrayKey";

    private final Map<String, Integer> clubMemberIndexMap = Maps.newConcurrentMap();
    //test
    private static final String COUNT_METHOD = "COUNTMETHODKEY";
    //test
    private static ShareClubMemberMgr instance = new ShareClubMemberMgr();

    // 获取单例
    public static ShareClubMemberMgr getInstance() {
        return instance;
    }

    /**
     * 添加共享俱乐部成员
     *
     * @param clubMember
     */
    public void addClubMember(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        redisMap.putJson(String.valueOf(clubMember.getClubMemberBO().getId()), clubMember);
        addOneClubMember(clubMember);
        addOnePlayerClubMember(clubMember);
        addOneClubMemberArray(clubMember);
        if (Config.isShareLocal()) {
            //mq通知所有节点同步数据
            clubMember.setUpdateTime(System.nanoTime());
            MqProducerMgr.get().send(MqTopic.LOCAL_CLUB_MEMBER_ADD, new MqClubMemberBo(clubMember));
        }
    }

    /**
     * 添加一个亲友圈的玩家
     *
     * @param clubMember
     */
    private void addOneClubMember(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_KEY + clubMember.getClubID());
        redisMap.putJson(String.valueOf(clubMember.getClubMemberBO().getPlayerID()), clubMember);
    }

    /**
     * 添加一个亲友圈的玩家
     *
     * @param clubMember
     */
    private void addOneClubMemberArray(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_ARRAY_KEY + clubMember.getClubID());
        redisMap.put(String.valueOf(clubMember.getClubMemberBO().getPlayerID()), PropertiesUtil.getObjectToString(clubMember.getClubMemberBO()));
    }

    /**
     * 添加一个亲友圈的玩家
     *
     * @param clubMember
     */
    private void addOnePlayerClubMember(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_PLAYER_CLUB_MEMBER_KEY + clubMember.getClubMemberBO().getPlayerID());
        redisMap.putJson(String.valueOf(clubMember.getClubMemberBO().getId()), clubMember);
    }

    /**
     * 更新共享俱乐部信息
     *
     * @param clubMemberBO
     */
    public void updateClubMemberBo(ClubMemberBO clubMemberBO) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        ClubMember clubMember = getClubMember(clubMemberBO.getId());
        clubMember.setClubMemberBO(clubMemberBO);
        redisMap.putJson(String.valueOf(clubMemberBO.getId()), clubMember);
    }

    /**
     * 删除成员信息
     *
     * @param memberId
     */
    public void deleteClubMember(Long memberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        ClubMember clubMember = redisMap.getObject(memberId, ClubMember.class);
        redisMap.removeObject(String.valueOf(memberId));
        if (clubMember != null) {
            deleteOneClubMember(clubMember);
            deleteOnePlayerMember(clubMember);
            deleteOneClubMemberArray(clubMember);
        }
        if (Config.isShareLocal()) {
            //mq通知所有节点同步数据
            MqProducerMgr.get().send(MqTopic.LOCAL_CLUB_MEMBER_REMOVE, new MqClubMemberBo(clubMember));
        }
    }

    /**
     * 删除一个亲友圈的成员信息
     *
     * @param clubMember
     */
    private void deleteOneClubMember(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_KEY + clubMember.getClubID());
        redisMap.removeObject(String.valueOf(clubMember.getClubMemberBO().getPlayerID()));
    }

    /**
     * 删除一个亲友圈的成员信息
     *
     * @param clubMember
     */
    private void deleteOneClubMemberArray(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_ARRAY_KEY + clubMember.getClubID());
        redisMap.removeObject(String.valueOf(clubMember.getClubMemberBO().getPlayerID()));
    }

    /**
     * 删除一个玩家亲友圈的成员信息
     *
     * @param clubMember
     */
    private void deleteOnePlayerMember(ClubMember clubMember) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_PLAYER_CLUB_MEMBER_KEY + clubMember.getClubMemberBO().getPlayerID());
        redisMap.removeObject(String.valueOf(clubMember.getClubMemberBO().getId()));
    }

    /**
     * 获取所有成员
     *
     * @return
     */
    public Map<Long, ClubMember> getAllClubMember() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, ClubMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    ClubMember clubMember = gson.fromJson(map.getValue(), ClubMember.class);
                    clubMembers.put(Long.parseLong(map.getKey()), clubMember);
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
     * 获取所有成员
     *
     * @return
     */
    public Map<Long, ClubMember> getAllOneClubMember(Long clubId) {
        if (Config.isShareLocal()) {
            return LocalClubMemberMgr.getInstance().getAllOneClubMember(clubId);
        } else {
            //test
            String getStack = GetStackUtils.getGetStackSimple();
            RedisMap redisMap1 = ContainerMgr.get().getRedis().getMap(COUNT_METHOD);
            redisMap1.putJson(getStack, redisMap1.get(getStack) == null ? 1 : Integer.parseInt(redisMap1.get(getStack)) + 1);
            //test
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_KEY + clubId);
            Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
            if (allSet != null) {
                Map<Long, ClubMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
                Gson gson = new Gson();
                allSet.forEach(map -> {
                    try {
                        ClubMember clubMember = gson.fromJson(map.getValue(), ClubMember.class);
                        clubMembers.put(Long.parseLong(map.getKey()), clubMember);
                    } catch (Exception e) {
                        CommLog.error(e.getMessage(), e);
                    }
                });
                return clubMembers;
            } else {
                return new ConcurrentHashMap<>(1);
            }
        }
    }

    /**
     * 获取所有成员
     *
     * @return
     */
    public Map<String, String> getAllOneClubMemberArray(Long clubId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_ARRAY_KEY + clubId);
        Map<String, String> map = redisMap.entryMapString();
        if (map == null) {
            return new ConcurrentHashMap<>(1);
        }
        return map;
    }


    /**
     * 获取所有成员
     *
     * @return
     */
    public Map<Long, ClubMember> getAllOnePlayerClubMember(Long playerId) {
        if (Config.isShareLocal()) {
            return LocalClubMemberMgr.getInstance().getAllOnePlayerClubMember(playerId);
        } else {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_PLAYER_CLUB_MEMBER_KEY + playerId);
            Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
            if (allSet != null) {
                Map<Long, ClubMember> clubMembers = new ConcurrentHashMap<>(allSet.size());
                Gson gson = new Gson();
                allSet.forEach(map -> {
                    try {
                        ClubMember clubMember = gson.fromJson(map.getValue(), ClubMember.class);
                        clubMembers.put(Long.parseLong(map.getKey()), clubMember);
                    } catch (Exception e) {
                        CommLog.error(e.getMessage(), e);
                    }
                });
                return clubMembers;
            } else {
                return new ConcurrentHashMap<>(1);
            }
        }
    }

    /**
     * 获取成员信息
     *
     * @param id
     * @return
     */
    public ClubMember getClubMember(Long id) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        ClubMember clubMember = redisMap.getObject(String.valueOf(id), ClubMember.class);
        return clubMember;
    }

    /**
     * 获取一个亲友圈成员信息
     *
     * @param clubId
     * @param pid
     * @return
     */
    private ClubMember getOneClubMember(Long clubId, Long pid) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_MEMBER_KEY + clubId);
        ClubMember clubMember = redisMap.getObject(String.valueOf(pid), ClubMember.class);
        return clubMember;
    }

    /**
     * 查询亲友圈指定玩家的信息
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家PID
     * @return
     */
    public ClubMember getClubMember(long clubID, long pid) {
//        Map<Long, ClubMember> allClubMemberMap = getAllClubMember();
//        // 检查亲友圈成员数据是否存在。
//        if (MapUtils.isEmpty(allClubMemberMap)) {
//            return null;
//        }
//        // or like this
//        ClubMember result = allClubMemberMap.values().stream().filter(x -> {
//            if (x.getClubMemberBO().getPlayerID() == pid && x.getClubID() == clubID
//                    && x.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value())) {
//                return true;
//            }
//            return false;
//        }).findAny().orElse(null);
//        return result;
        return getOneClubMember(clubID, pid);
    }

    /**
     * 更新共享成员字段值
     * 更新时候家成员分布式锁防止并发
     *
     * @param clubMemberBO
     * @param fields
     * @return
     */
    public void updateField(ClubMemberBO clubMemberBO, String... fields) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + clubMemberBO.getId(), uuid);
            ClubMember clubMember = getClubMember(clubMemberBO.getId());
            for (String field : fields) {
                Object value = PropertiesUtil.invokeGet(clubMemberBO, field);
                PropertiesUtil.invokeSet(clubMember.getClubMemberBO(), field, value);
            }
            addClubMember(clubMember);
        } catch (Exception e) {
            CommLogD.error(e.getMessage(), e);
        } finally {
            DistributedRedisLock.release("sportsPoint" + clubMemberBO.getId(), uuid);
        }
    }

    /**
     * 更新共享成员字段值
     * 更新时候家成员分布式锁防止并发
     *
     * @return
     */
    public void updateUpLevelId(Long id, Long upLevelId, int level) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + id, uuid);
            ClubMember clubMember = getClubMember(id);
            clubMember.getClubMemberBO().setUpLevelId(upLevelId);
            clubMember.getClubMemberBO().setLevel(level);
            addClubMember(clubMember);
        } catch (Exception e) {
            CommLogD.error(e.getMessage(), e);
        } finally {
            DistributedRedisLock.release("sportsPoint" + id, uuid);
        }
    }

    /**
     * 更新共享成员字段值
     *
     * @param clubMemberBO
     * @param mapFields
     * @return
     */
    public void updateField(ClubMemberBO clubMemberBO, Map<String, Object> mapFields) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            DistributedRedisLock.acquire("sportsPoint" + clubMemberBO.getId(), uuid);
            ClubMember clubMember = getClubMember(clubMemberBO.getId());
            mapFields.forEach((k, v) -> {
                Object value = PropertiesUtil.invokeGet(clubMemberBO, k);
                PropertiesUtil.invokeSet(clubMember.getClubMemberBO(), k, value);
            });
            addClubMember(clubMember);
        } catch (Exception e) {
            CommLogD.error(e.getMessage(), e);
        } finally {
            DistributedRedisLock.release("sportsPoint" + clubMemberBO.getId(), uuid);
        }
    }

    public ClubMember findCreate(long clubId) {
        Club club = ShareClubListMgr.getInstance().getClub(clubId);
        if (Objects.isNull(club)) {
            CommLogD.error("findCreate clubId:{}", clubId);
            return null;
        }
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, club.getClubListBO().getOwnerID());
        return Objects.nonNull(clubMember) && clubMember.isClubCreate() ? clubMember : null;
    }

    /**
     * 检查指定赛事成员是否存在
     *
     * @param clubIdList
     * @param pid
     * @return
     */
    public boolean checkExistByPidMember(final List<Long> clubIdList, long pid) {
        for (Long clubId : clubIdList) {
            ClubMember clubMember = this.getClubMember(clubId, pid);
            if (clubMember != null && clubMember.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取亲友圈成员-亲友圈列表中最大竞技点的亲友圈Id
     *
     * @param clubIdList 亲友圈列表
     * @param pid        玩家pid
     * @return
     */
    public long getMemberMaxSportsPointClubId(final List<Long> clubIdList, long pid) {
        Map<Long, ClubMember> clubMemberMap = new ConcurrentHashMap<>();
        for (Long clubId : clubIdList) {
            ClubMember clubMember = this.getClubMember(clubId, pid);
            if (clubMember != null && clubMember.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && clubMember.getClubMemberBO().getPlayerID() == pid && !(clubMember.isBanGame() || clubMember.isUnionBanGame())) {
                clubMemberMap.put(clubMember.getId(), clubMember);
            }
        }
        return clubMemberMap.values().stream().sorted(Comparator.comparing(ClubMember::getSportsPointLong).reversed()).map(k -> k.getClubMemberBO().getClubID()).findFirst().orElse(0L);

    }

    /**
     * 是否赛事成员
     * 并且没有被禁止游戏
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    public boolean anyMatchNotBanGame(long clubId, long pid) {
        ClubMember clubMember = this.getClubMember(clubId, pid);
        if (clubMember != null && clubMember.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && !clubMember.isBanGame()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 查询指定亲友圈ID玩家
     * 只显示在线
     *
     * @param clubIdList 亲友圈列表
     * @return
     */
    public List<Long> findClubIdAllClubMemberOnline(final List<Long> clubIdList) {
        if (Config.isShareLocal()) {
            return LocalClubMemberMgr.getInstance().findClubIdAllClubMemberOnline(clubIdList);
        } else {
            List<Long> list = new ArrayList<>();
            for (Long clubId : clubIdList) {
                List<Long> oneClubList = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                        .filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_define.Club_Player_Status.PLAYER_JIARU.value()) && !(ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0 || ClubMemberUtils.getArrayValueInteger(k, "unionBanGame") > 0)).map(k -> ClubMemberUtils.getArrayValueLong(k, "playerID")).distinct().collect(Collectors.toList());
                list.addAll(oneClubList);
            }
//        for (Long clubId : clubIdList) {
//            List<Long> oneClubList = this.getAllOneClubMember(clubId).values().stream().filter(k -> k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && !(k.isBanGame() || k.isUnionBanGame())).map(k -> k.getClubMemberBO().getPlayerID()).distinct().collect(Collectors.toList());
//            list.addAll(oneClubList);
//        }
//        CommLogD.info("Invitation clubIdList[{}]",new Gson().toJson(clubIdList));
//        CommLogD.info("Invitation clubMemberList[{}]",new Gson().toJson(list));
            return list.stream().distinct().collect(Collectors.toList());
        }
    }

    /**
     * 判断成员是否存在
     *
     * @param clubMemberId
     */
    public boolean existClubMember(Long clubMemberId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_CLUB_MEMBER_KEY);
        return redisMap.containsKey(String.valueOf(clubMemberId));
    }

    /**
     * 获取属性名的位置
     *
     * @param fileName
     * @return
     */
    public int getClubMemberIndexByName(String fileName) {
        if (clubMemberIndexMap.isEmpty()) {
            synchronized (this) {
                clubMemberIndexMap.putAll(PropertiesUtil.getObjectToMapIndex(ClubMemberBO.class));
                return clubMemberIndexMap.get(fileName);
            }
        } else {
            return clubMemberIndexMap.get(fileName);
        }
    }

    public Map<String, Integer> getClubMemberIndexMap() {
        return clubMemberIndexMap;
    }

    /**
     * 初始化ShareOneClubMemberArrayKey，是需要使用一次
     *
     * @param clubMemberMap
     */
    public synchronized void initShareOneClubMemberArrayKey(Map<Long, ClubMember> clubMemberMap) {
        if (!ContainerMgr.get().getRedis().exists(SHARE_ONE_CLUB_MEMBER_ARRAY_KEY)) {
            ContainerMgr.get().getRedis().put(SHARE_ONE_CLUB_MEMBER_ARRAY_KEY, "1");
            for (ClubMember clubMember : clubMemberMap.values()) {
                addOneClubMemberArray(clubMember);
            }
        }
    }
}
