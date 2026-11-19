package business.shareplayer;

import BaseCommon.CommLog;
import business.player.Player;
import business.player.PlayerRoomInfo;
import business.rocketmq.bo.MqPlayerBo;
import business.rocketmq.bo.MqPlayerRemoveBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.redis.RedisSet;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.BeanUtils;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.gson.Gson;
import core.db.entity.clarkGame.PlayerBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.LocationInfo;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家管理类
 */
public class SharePlayerMgr {
    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static SharePlayerMgr instance = new SharePlayerMgr();
    }

    //在线玩家缓存key
    private final String SHARE_ONLINE_PLAYER_KEY = "shareOnlinePlayerKey";
    //所有玩家缓存key
    private final String SHARE_ALL_PLAYER_KEY = "shareAllPlayerKey";
    //在线玩家ID缓存key
    private final String SHARE_ONLINE_PLAYER_ID_KEY = "shareOnlinePlayerIdKey";
    //所有玩家ID缓存key
    private final String SHARE_ALL_PLAYER_ID_KEY = "shareAllPlayerIdKey";
    private final String nodeName = Config.nodeName();
    private final String nodeVipAddress = Config.nodeVipAddress();
    private final String nodeIp = Config.nodeIp();
    private final Integer nodePort = Config.nodePort();

    private SharePlayerMgr() {
    }

    public static SharePlayerMgr getInstance() {
        return SharePlayerMgr.SingletonHolder.instance;
    }

    public void init() {
//        //只初始化一次
//        if(!ContainerMgr.get().getRedis().exists(SHARE_ALL_PLAYER_ID_KEY)){
//            RedisSet redisSetPlayerIdAll = ContainerMgr.get().getRedis().getSet(SHARE_ALL_PLAYER_ID_KEY);
//            HashMap<Long, SharePlayer> sharePlayers = allSharePlayers();
//            sharePlayers.forEach((k,v)->redisSetPlayerIdAll.add(String.valueOf(k)));
//        }
        //清除节点在线玩家,重启的情况
        Map<Long, SharePlayer> sharePlayers = SharePlayerMgr.getInstance().onlineSharePlayersRedis();
        cleanNodePlayer(sharePlayers);
        if (Config.isShareLocal()) {
            LocalPlayerMgr.getInstance().initOnlinePlayer(sharePlayers);
        }

    }

    /**
     * 添加在线共享玩家信息
     *
     * @param player
     * @return
     */
    public SharePlayer addOnlineSharePlayer(Player player) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        RedisMap redisMapAll = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        RedisSet redisSetPlayerId = ContainerMgr.get().getRedis().getSet(SHARE_ONLINE_PLAYER_ID_KEY);
        RedisSet redisSetPlayerIdAll = ContainerMgr.get().getRedis().getSet(SHARE_ALL_PLAYER_ID_KEY);
        SharePlayer sharePlayer = getSharePlayer(player.getPid());
        if (sharePlayer == null) {
            sharePlayer = createSharePlayer(player);
        }
        //设置节点信息
        sharePlayer.setCurShareNode(new ShareNode(this.nodeName, this.nodeVipAddress, this.nodeIp, this.nodePort));
        String sharePlayerString = new Gson().toJson(sharePlayer);
        redisMap.put(String.valueOf(player.getPid()), sharePlayerString);
        redisMapAll.put(String.valueOf(player.getPid()), sharePlayerString);
        redisSetPlayerId.add(String.valueOf(player.getPid()));
        redisSetPlayerIdAll.add(String.valueOf(player.getPid()));
        if (Config.isShareLocal()) {
            //本地缓存更新通知
            sharePlayer.setUpdateTime(System.nanoTime());
            MqProducerMgr.get().send(MqTopic.LOCAL_ONLINE_PLAYER_ADD, new MqPlayerBo(sharePlayer));
        }
        return sharePlayer;
    }

    /**
     * 移除在线共享玩家
     *
     * @param player
     */
    public void removeOnlineSharePlayer(Player player) {
        removeOnlineSharePlayer(player.getPid());
    }

    /**
     * 移除在线共享玩家
     *
     * @param pid
     */
    /**
     * 移除在线共享玩家
     *
     * @param pid
     */
    public void removeOnlineSharePlayer(Long pid) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        RedisSet redisSet = ContainerMgr.get().getRedis().getSet(SHARE_ONLINE_PLAYER_ID_KEY);
        if (redisMap.containsKey(String.valueOf(pid))) {
            String result = redisMap.get(pid);
            SharePlayer sharePlayer = new Gson().fromJson(result, SharePlayer.class);
            if (sharePlayer != null && sharePlayer.getCurShareNode() != null) {
                //当前节点才移除
                if (checkCurNodePlayer(sharePlayer.getCurShareNode())) {
                    redisMap.remove(String.valueOf(pid));
                    redisSet.remove(String.valueOf(pid));
                    if (Config.isShareLocal()) {
                        //删除本地缓存更新通知
                        MqProducerMgr.get().send(MqTopic.LOCAL_ONLINE_PLAYER_REMOVE, new MqPlayerRemoveBo(pid));
                    }
                }
            }
        }
    }

    /**
     * 获取在线共享玩家
     *
     * @param pid
     * @return
     */
    public SharePlayer getSharePlayerByOnline(Long pid) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        if (redisMap.containsKey(String.valueOf(pid))) {
            String result = redisMap.get(pid);
            return new Gson().fromJson(result, SharePlayer.class);
        }
        return null;
    }

    /**
     * 获取在线共享玩家
     *
     * @param player
     * @return
     */
    public SharePlayer getSharePlayerByOnline(Player player) {
        return getSharePlayerByOnline(player.getPid());
    }

    /**
     * 检测共享玩家是否在线
     *
     * @param pid
     * @return
     */
    public boolean checkSharePlayerByOnline(Long pid) {
        if (Config.isShareLocal()) {
            return LocalPlayerMgr.getInstance().checkSharePlayerByOnline(pid);
        } else {
            RedisSet redisSet = ContainerMgr.get().getRedis().getSet(SHARE_ONLINE_PLAYER_ID_KEY);
            return redisSet.contains(String.valueOf(pid));
        }
    }

    /**
     * 获取所有玩家ID
     *
     * @return
     */
    public Set<String> allShareAllPlayerIdKey() {
        RedisSet redisSet = ContainerMgr.get().getRedis().getSet(SHARE_ALL_PLAYER_ID_KEY);
        return redisSet.getSet();
    }

    /**
     * 获取在线共享玩家数量
     *
     * @return
     */
    public int onlineSharePlayerSize() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        return redisMap.size();
    }

    /**
     * 获取所有在线玩家
     *
     * @return
     */
    public Map<Long, SharePlayer> onlineSharePlayers() {
        if (Config.isShareLocal()) {
            return LocalPlayerMgr.getInstance().onlineSharePlayers();
        } else {
            return onlineSharePlayersRedis();
        }
    }

    /**
     * 获取所有在线玩家
     *
     * @return
     */
    public Map<Long, SharePlayer> onlineSharePlayersRedis() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        HashMap<Long, SharePlayer> players = new HashMap<>(allSet.size());
        allSet.forEach(data -> {
            players.put(Long.parseLong(data.getKey()), new Gson().fromJson(data.getValue(), SharePlayer.class));
        });
        return players;
    }

    /**
     * 获取所有在线玩家
     *
     * @return
     */
    public Set<String> onlineSharePlayerIds() {
        RedisSet redisSet = ContainerMgr.get().getRedis().getSet(SHARE_ONLINE_PLAYER_ID_KEY);
        Set<String> set = redisSet.getSet();
        return set;
    }

    /**
     * 获取所有玩家
     *
     * @return
     */
    public HashMap<Long, SharePlayer> allSharePlayers() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        HashMap<Long, SharePlayer> players = new HashMap<>(allSet.size());
        allSet.forEach(data -> {
            players.put(Long.parseLong(data.getKey()), new Gson().fromJson(data.getValue(), SharePlayer.class));
        });
        return players;
    }

    /**
     * 所有玩家获取共享玩家
     *
     * @param player
     * @return
     */
    public SharePlayer getSharePlayer(Player player) {
        return getSharePlayer(player.getPid());
    }

    /**
     * 获取共享的玩家信息返回player对象
     *
     * @param player
     * @return
     */
    public Player getPlayer(Player player) {
        try {
            SharePlayer sharePlayer = getSharePlayer(player.getPid());
            if (sharePlayer != null) {
                PlayerBO playerBO = player.getPlayerBO();
                BeanUtils.copyProperties(playerBO, sharePlayer.getPlayerBO());
                LocationInfo locationInfo = player.getLocationInfo();
                BeanUtils.copyProperties(locationInfo, sharePlayer.getLocationInfo());
                PlayerRoomInfo playerRoomInfo = player.getRoomInfo();
                int consumeCard = playerRoomInfo.getConsumeCard();
                BeanUtils.copyProperties(playerRoomInfo, sharePlayer.getRoomInfo());
                //消耗房卡不替换
                playerRoomInfo.setConsumeCard(consumeCard);
                player.setDiamondsAttentionAll(sharePlayer.isDiamondsAttentionAll());
                player.setDiamondsAttentionMinister(sharePlayer.isDiamondsAttentionMinister());
                player.setSignEnumClubID(sharePlayer.getSignEnumClubID());
                player.setSignEnum(sharePlayer.getSignEnum());
                player.setUnionDiamondsAttentionAll(sharePlayer.isUnionDiamondsAttentionAll());
                player.setUnionDiamondsAttentionMinister(sharePlayer.isUnionDiamondsAttentionMinister());
                player.setIp(sharePlayer.getIp());
                player.setHourTime(sharePlayer.getHourTime());
                player.setLastTime(sharePlayer.getLastTime());
                player.setIsMobile(sharePlayer.getIsMobile());
            } else {
                addAllSharePlayer(player);
            }
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
        return player;

    }

    /**
     * 获取共享玩家从所有玩家中获取
     *
     * @param pid
     * @return
     */
    public SharePlayer getSharePlayer(Long pid) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        if (redisMap.containsKey(String.valueOf(pid))) {
            String result = redisMap.get(pid);
            Gson gson = new Gson();
            return gson.fromJson(result, SharePlayer.class);
        }
        return null;
    }

    /**
     * 初始化添加所有玩家里面的玩家信息
     *
     * @param player
     * @return
     */
    public SharePlayer addAllSharePlayer(Player player) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        RedisSet redisSetPlayerIdAll = ContainerMgr.get().getRedis().getSet(SHARE_ALL_PLAYER_ID_KEY);
        //判定玩家是否存在
        if (!redisMap.containsKey(String.valueOf(player.getPid()))) {
            SharePlayer sharePlayer = createSharePlayer(player);
            redisMap.put(String.valueOf(player.getPid()), new Gson().toJson(sharePlayer));
            redisSetPlayerIdAll.add(String.valueOf(player.getPid()));
            if (Config.isShareLocal()) {
                //本地缓存更新通知
                sharePlayer.setUpdateTime(System.nanoTime());
                MqProducerMgr.get().send(MqTopic.LOCAL_PLAYER_ADD, new MqPlayerBo(sharePlayer));
            }
            return sharePlayer;
        }
        return null;
    }

    /**
     * 更新所有玩家里面的玩家信息
     *
     * @param player
     * @return
     */
    public SharePlayer updateAllSharePlayer(Player player) {
        //修改所有玩家列表的玩家信息
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        SharePlayer sharePlayer = createSharePlayer(player);
        redisMap.put(String.valueOf(player.getPid()), new Gson().toJson(sharePlayer));

        //修改在线玩家列表的玩家信息
        RedisMap onlineRedisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        if (onlineRedisMap.containsKey(String.valueOf(player.getPid()))) {
            onlineRedisMap.put(String.valueOf(player.getPid()), new Gson().toJson(sharePlayer));
        }
        return sharePlayer;
    }

    /**
     * 将玩家信息转换成共享玩家信息
     *
     * @param player
     * @return
     */
    private SharePlayer createSharePlayer(Player player) {
        SharePlayer sharePlayer = getSharePlayer(player.getPid());
        if (sharePlayer == null) {
            sharePlayer = new SharePlayer();
        }
        sharePlayer.setDiamondsAttentionAll(player.isDiamondsAttentionAll());
        sharePlayer.setDiamondsAttentionMinister(player.isDiamondsAttentionMinister());
        sharePlayer.setSignEnumClubID(player.getSignEnumClubID());
        sharePlayer.setSignEnum(player.getSignEnum());
        sharePlayer.setUnionDiamondsAttentionAll(player.isUnionDiamondsAttentionAll());
        sharePlayer.setUnionDiamondsAttentionMinister(player.isUnionDiamondsAttentionMinister());
        sharePlayer.setIp(player.getIp());
        sharePlayer.setHourTime(player.getHourTime());
        sharePlayer.setLastTime(player.getLastTime());
        sharePlayer.setIsMobile(player.getIsMobile());
        SharePlayerBO sharePlayerBO = new SharePlayerBO();
        BeanUtils.copyProperties(sharePlayerBO, player.getPlayerBO());
        sharePlayer.setPlayerBO(sharePlayerBO);
        ShareLocationInfo shareLocationInfo = new ShareLocationInfo();
        BeanUtils.copyProperties(shareLocationInfo, player.getLocationInfo());
        sharePlayer.setLocationInfo(shareLocationInfo);
        SharePlayerRoomInfo sharePlayerRoomInfo = new SharePlayerRoomInfo();
        BeanUtils.copyProperties(sharePlayerRoomInfo, player.getRoomInfo());
        sharePlayer.setRoomInfo(sharePlayerRoomInfo);
        //设置节点信息
        sharePlayer.setCurShareNode(new ShareNode(this.nodeName, this.nodeVipAddress, this.nodeIp, this.nodePort));
        return sharePlayer;
    }


    /**
     * 更新共享玩家邀请字段
     *
     * @param playerId
     * @return
     */
    public void updateInviteFlag(Long playerId) {
        SharePlayer sharePlayer = getSharePlayer(playerId);
        sharePlayer.setInviteFlag(true);
        updateSharePlayer(sharePlayer);
    }

    /**
     * 更新共享玩家字段值
     *
     * @param playerBO
     * @param fields
     * @return
     */
    public SharePlayer updateField(PlayerBO playerBO, String... fields) {
        SharePlayer sharePlayer = getSharePlayer(playerBO.getId());
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(playerBO, field);
            Field shareField = PropertiesUtil.getField(sharePlayer.getPlayerBO(), field);
            if (shareField == null) {
                CommLog.error("{}设置字段不存在", field);
            }
            Gson gson = new Gson();
            Object newValue = null;
            if (value != null) {
                try {
                    newValue = gson.fromJson(gson.toJson(value), shareField.getGenericType());
                } catch (Exception e) {
                    CommLog.error("{}字段转换异常{}", field);
                }
            }
            PropertiesUtil.invokeSet(sharePlayer.getPlayerBO(), field, newValue);
        }
        updateSharePlayer(sharePlayer);
        return sharePlayer;
    }

    /**
     * 更新共享玩家字段值
     *
     * @param player
     * @param fields
     * @return
     */
    public SharePlayer updateField(Player player, String... fields) {
        SharePlayer sharePlayer = getSharePlayer(player.getPid());
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(player, field);
            Field shareField = PropertiesUtil.getField(sharePlayer, field);
            if (shareField == null) {
                CommLog.error("{}设置字段不存在", field);
            }
            Gson gson = new Gson();
            Object newValue = null;
            if (value != null) {
                try {
                    newValue = gson.fromJson(gson.toJson(value), shareField.getGenericType());
                } catch (Exception e) {
                    CommLog.error("{}字段转换异常{}", field);
                }
            }
            PropertiesUtil.invokeSet(sharePlayer, field, newValue);
        }
        updateSharePlayer(sharePlayer);
        return sharePlayer;
    }

    /**
     * 检查玩家当前节点
     *
     * @param shareNode
     * @return
     */
    public boolean checkCurNodePlayer(ShareNode shareNode) {
//        if (StringUtils.isEmpty(nodeIp)) {
//            nodeIp = IpUtils.getLanIp();
//        }
        if (shareNode.getName().equals(nodeName) && shareNode.getVipAddress().equals(nodeVipAddress) && shareNode.getIp().equals(nodeIp) && shareNode.getPort().compareTo(nodePort) == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 清理节点的玩家
     */
    public void cleanNodePlayer(Map<Long, SharePlayer> sharePlayers) {
        sharePlayers.forEach((k, sharePlayer) -> {
            if (sharePlayer != null) {
                ShareNode shareNode = sharePlayer.getCurShareNode();
                if (checkCurNodePlayer(shareNode)) {
                    removeOnlineSharePlayer(k);
                }
            }
        });
    }

    /**
     * 清理掉玩家所在房间信息,当游戏节点重启的时候需要操作
     *
     * @param roomId
     */
    public void cleanRoomId(Long roomId, Map<Long, SharePlayer> sharePlayers) {
        sharePlayers.forEach((k, sharePlayer) -> {
            if (sharePlayer != null) {
                if (sharePlayer.getRoomInfo().getRoomId() == roomId) {
                    sharePlayer.setRoomInfo(new SharePlayerRoomInfo());
                    updateSharePlayer(sharePlayer);
                }
            }
        });
    }

    /**
     * 更新玩家信息
     *
     * @param sharePlayer
     */
    public void updateSharePlayer(SharePlayer sharePlayer) {
        Gson gson = new Gson();
        //修改所有玩家列表的玩家信息
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ALL_PLAYER_KEY);
        redisMap.put(String.valueOf(sharePlayer.getPlayerBO().getId()), gson.toJson(sharePlayer));
        if (Config.isShareLocal()) {
            //本地缓存更新通知
            sharePlayer.setUpdateTime(System.nanoTime());
            MqProducerMgr.get().send(MqTopic.LOCAL_PLAYER_ADD, new MqPlayerBo(sharePlayer));
        }
        //修改在线玩家列表的玩家信息
        RedisMap onlineRedisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
        if (onlineRedisMap.containsKey(String.valueOf(sharePlayer.getPlayerBO().getId()))) {
            onlineRedisMap.put(String.valueOf(sharePlayer.getPlayerBO().getId()), gson.toJson(sharePlayer));
            if (Config.isShareLocal()) {
                //本地缓存更新通知
                sharePlayer.setUpdateTime(System.nanoTime());
                MqProducerMgr.get().send(MqTopic.LOCAL_ONLINE_PLAYER_ADD, new MqPlayerBo(sharePlayer));
            }
        }
    }

    /**
     * 获取所有节点的在线玩家数量
     *
     * @return
     */
    public List<ShareNodePlayerSize> onlinePlayerSizeGroup() {
        Map<Long, SharePlayer> onlineSharePlayers = onlineSharePlayers();
        Map<ShareNode, Long> shareNodeLongMap = onlineSharePlayers.values().stream().collect(Collectors.groupingBy(sharePlayer -> sharePlayer.getCurShareNode(), Collectors.counting()));
        List<ShareNodePlayerSize> list = new ArrayList<>();
        shareNodeLongMap.forEach((k, v) -> {
            list.add(getShareNodePlayerSize(k, v));
        });
        return list;
    }

    /**
     * 获取联盟当前在线人数
     *
     * @return
     */
    public int onlinePlayerSizeUnionId(Long unionId) {
        Map<Long, SharePlayer> onlineSharePlayers = onlineSharePlayers();
        Long count = onlineSharePlayers.values().stream().filter(k -> k.getRoomInfo().getUnionId() == unionId).count();
        return count.intValue();
    }

    private ShareNodePlayerSize getShareNodePlayerSize(ShareNode shareNode, Long playerSize) {
        ShareNodePlayerSize shareNodePlayerSize = BeanUtils.copyObject(shareNode, ShareNodePlayerSize.class);
        shareNodePlayerSize.setPlayerSize(playerSize);
        return shareNodePlayerSize;
    }

    /**
     * 设置共享玩家在线
     *
     * @param pid
     */
    public void setSharePlayerToOnline(Long pid) {
        SharePlayer sharePlayer = getSharePlayer(pid);
        if (sharePlayer != null) {
            RedisMap onlineRedisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONLINE_PLAYER_KEY);
            onlineRedisMap.put(String.valueOf(sharePlayer.getPlayerBO().getId()), new Gson().toJson(sharePlayer));
            RedisSet redisSet = ContainerMgr.get().getRedis().getSet(SHARE_ONLINE_PLAYER_ID_KEY);
            redisSet.add(String.valueOf(sharePlayer.getPlayerBO().getId()));
        } else {
            CommLogD.error("共享玩家不存在[{}]", pid);
        }
    }

    /**
     * 重置每个玩家的邀请状态
     */
    public void clearInviteInfo() {
        Long startTime = System.currentTimeMillis();
        Set<String> playerIdSet = allShareAllPlayerIdKey();
        playerIdSet.forEach(k -> updateInviteFlag(Long.parseLong(k)));
        CommLogD.info("修改邀请状态耗时[{}]", System.currentTimeMillis() - startTime);
    }

}
