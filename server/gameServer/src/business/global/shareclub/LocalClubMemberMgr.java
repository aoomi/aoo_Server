package business.global.shareclub;

import business.global.club.ClubMember;
import com.ddm.server.common.CommLogD;
import com.google.common.collect.Maps;
import jsproto.c2s.cclass.club.Club_define;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地缓存成员信息
 */
public class LocalClubMemberMgr {
    // 本地俱乐部成员管理map
    private final Map<Long, Map<Long, ClubMember>> localOneClubMemberMap = new ConcurrentHashMap<>();
    // 玩家ID对应成员ID
    private final Map<Long, Map<Long, Long>> onePlayerClubMemberMap = new ConcurrentHashMap<>();

    private LocalClubMemberMgr() {
    }

    public static LocalClubMemberMgr getInstance() {
        return LocalClubMemberMgr.SingletonHolder.instance;
    }

    public Map<Long, Map<Long, Long>> getOnePlayerClubMemberMap() {
        return onePlayerClubMemberMap;
    }

    public Map<Long, Map<Long, ClubMember>> getLocalOneClubMemberMap() {
        return localOneClubMemberMap;
    }

    /**
     * 初始化数据
     *
     * @param clubMemberMap
     */
    public void initClubMember(Map<Long, ClubMember> clubMemberMap) {
        clubMemberMap.values().stream().forEach(k -> addClubMember(k));
    }

    /**
     * 添加成员到本地缓存
     *
     * @param clubMember
     */
    public void addClubMember(ClubMember clubMember) {
        Map<Long, ClubMember> clubMemberMap = this.getLocalOneClubMemberMap().get(clubMember.getClubMemberBO().getClubID());
        if (clubMemberMap == null) {
            Map<Long, ClubMember> map = addOnClubMap(clubMember.getClubMemberBO().getClubID());
            ClubMember clubMemberOld = map.get(clubMember.getClubMemberBO().getId());
            if (clubMemberOld != null) {
                if (clubMemberOld.getUpdateTime() < clubMember.getUpdateTime()) {
                    map.put(clubMember.getClubMemberBO().getId(), clubMember);
                    addOnePlayerClubMember(clubMember);
                }
            } else {
                map.put(clubMember.getClubMemberBO().getId(), clubMember);
                addOnePlayerClubMember(clubMember);
            }
        } else {
            ClubMember clubMemberOld = clubMemberMap.get(clubMember.getClubMemberBO().getId());
            if (clubMemberOld != null) {
                if (clubMemberOld.getUpdateTime() < clubMember.getUpdateTime()) {
                    clubMemberMap.put(clubMember.getClubMemberBO().getId(), clubMember);
                    addOnePlayerClubMember(clubMember);
                }
            } else {
                clubMemberMap.put(clubMember.getClubMemberBO().getId(), clubMember);
                addOnePlayerClubMember(clubMember);
            }
        }
    }

    /**
     * 添加玩家ID和成员ID关系
     *
     * @param clubMember
     */
    public void addOnePlayerClubMember(ClubMember clubMember) {
        Map<Long, Long> clubMemberMap = this.getOnePlayerClubMemberMap().get(clubMember.getClubMemberBO().getPlayerID());
        if (clubMemberMap == null) {
            clubMemberMap = this.getOnePlayerClubMemberMap().computeIfAbsent(
                    clubMember.getClubMemberBO().getPlayerID(), ignored -> new ConcurrentHashMap<>());
        }
        try {
            clubMemberMap.put(clubMember.getClubMemberBO().getId(), clubMember.getClubMemberBO().getClubID());
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            CommLogD.error(e.getMessage(), e);
        }
    }


    /**
     * 添加玩家ID和成员ID关系
     *
     * @param clubMember
     */
    private void removeOnePlayerClubMember(ClubMember clubMember) {
        Map<Long, Long> clubMemberMap = this.getOnePlayerClubMemberMap().get(clubMember.getClubMemberBO().getPlayerID());
        if (clubMemberMap != null) {
            clubMemberMap.remove(clubMember.getClubMemberBO().getId());
        }

    }

    /**
     * 这里会有线程安全问题所有需要同步块
     * 添加一个亲友圈成本Map用来存储
     *
     * @param clubId
     */
    public Map<Long, ClubMember> addOnClubMap(Long clubId) {
        return this.getLocalOneClubMemberMap().computeIfAbsent(clubId, ignored -> new ConcurrentHashMap<>());
    }

    /**
     * 删除成员从本地缓存
     *
     * @param clubMember
     */
    public void removeClubMember(ClubMember clubMember) {
        Map<Long, ClubMember> clubMemberMap = this.getLocalOneClubMemberMap().get(clubMember.getClubMemberBO().getClubID());
        clubMemberMap.remove(clubMember.getClubMemberBO().getId());
        //删除玩家ID和成员关系
        removeOnePlayerClubMember(clubMember);
    }

    /**
     * 获取所有亲友圈成员
     *
     * @return
     */
    public HashMap<Long, ClubMember> getAllClubMember() {
        HashMap<Long, ClubMember> allClubMember = Maps.newHashMap();
        this.getLocalOneClubMemberMap().forEach((k, v) -> allClubMember.putAll(v));
        return allClubMember;
    }

    /**
     * 获取一个亲友圈成员
     *
     * @return
     */
    public Map<Long, ClubMember> getAllOneClubMember(Long clubId) {
        return this.getLocalOneClubMemberMap().get(clubId);
    }

    /**
     * 获取一个玩家的所有亲友圈成员信息
     *
     * @param playerId
     * @return
     */
    public Map<Long, ClubMember> getAllOnePlayerClubMember(Long playerId) {
        Map<Long, ClubMember> allClubMember = Maps.newHashMap();
        Map<Long, Long> map = this.getOnePlayerClubMemberMap().get(playerId);
        if (map != null) {
            map.forEach((k, v) -> {
                allClubMember.put(k, this.getLocalOneClubMemberMap().get(v).get(k));
            });
        }
        return allClubMember;
    }

    /**
     * 获取几个亲友圈的玩家信息
     *
     * @param clubIdList
     * @return
     */
    public List<Long> findClubIdAllClubMemberOnline(List<Long> clubIdList) {
        Map<Long, ClubMember> allClubMember = Maps.newHashMap();
        for (Long clubId : clubIdList) {
            allClubMember.putAll(this.getLocalOneClubMemberMap().get(clubId));
        }
        return allClubMember.values().stream().filter(k -> k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && !(k.isBanGame() || k.isUnionBanGame())).map(k -> k.getClubMemberBO().getPlayerID()).distinct().collect(Collectors.toList());
    }

    /**
     * 获取几个亲友圈的玩家信息
     *
     * @param clubIdList
     * @return
     */
    public Map<Long, ClubMember> getAllClubMemberByClubIds(List<Long> clubIdList) {
        Map<Long, ClubMember> allClubMember = Maps.newHashMap();
        for (Long clubId : clubIdList) {
            Map<Long, ClubMember> clubMemberMap = this.getLocalOneClubMemberMap().get(clubId);
            if (clubMemberMap != null) {
                allClubMember.putAll(clubMemberMap);
            }
        }
        return allClubMember;
    }

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static LocalClubMemberMgr instance = new LocalClubMemberMgr();
    }


}
