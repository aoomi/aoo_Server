//package business.shareplayer;
//
//import com.ddm.server.common.mongodb.MongoDBHelper;
//import com.mongodb.client.model.Filters;
//import core.ioc.ContainerMgr;
//import org.bson.conversions.Bson;
//
//import java.util.HashMap;
//import java.util.List;
//
//public class MongoDbPlayerMgr {
//    //在线玩家缓存key
//    private final String SHARE_ONLINE_PLAYER_KEY = "shareOnlinePlayerKey";
//    //所有玩家缓存key
//    private final String SHARE_ALL_PLAYER_KEY = "shareAllPlayerKey";
//    //在线玩家基础信息缓存key
//    private final String SHARE_ONLINE_PLAYER_SIMPLE_KEY = "ShareOnlinePlayerSimpleKey";
//
//    private MongoDbPlayerMgr() {
//    }
//
//    public static MongoDbPlayerMgr getInstance() {
//        return MongoDbPlayerMgr.SingletonHolder.instance;
//    }
//
//    public void addOnlinePlayer(SharePlayer sharePlayer) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        sharePlayer.setId(sharePlayer.getPlayerBO().getId());
//        sharePlayer.setRoomId(sharePlayer.getRoomInfo().getRoomId());
//        sharePlayer.setClubId(sharePlayer.getRoomInfo().getClubId());
//        sharePlayer.setUnionId(sharePlayer.getRoomInfo().getUnionId());
//        sharePlayer.setNodeIp(sharePlayer.getCurShareNode().getIp());
//        sharePlayer.setNodePort(sharePlayer.getCurShareNode().getPort());
//        if (!mongoDBHelper.existById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_KEY)) {
//            mongoDBHelper.insert(sharePlayer, SHARE_ONLINE_PLAYER_KEY);
//        } else {
//            mongoDBHelper.updateById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_KEY, sharePlayer);
//        }
//        //添加在线玩家基础信息
//        addOnlinePlayerSimple(sharePlayer);
//    }
//
//    public void updateOnlinePlayer(SharePlayer sharePlayer) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        sharePlayer.setId(sharePlayer.getPlayerBO().getId());
//        sharePlayer.setRoomId(sharePlayer.getRoomInfo().getRoomId());
//        sharePlayer.setClubId(sharePlayer.getRoomInfo().getClubId());
//        sharePlayer.setUnionId(sharePlayer.getRoomInfo().getUnionId());
//        sharePlayer.setNodeIp(sharePlayer.getCurShareNode().getIp());
//        sharePlayer.setNodePort(sharePlayer.getCurShareNode().getPort());
//        if (mongoDBHelper.existById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_KEY)) {
//            mongoDBHelper.updateById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_KEY, sharePlayer);
//        }
//        //修改在线玩家基础信息
//        updateOnlinePlayerSimple(sharePlayer);
//    }
//
//    public void addAllPlayer(SharePlayer sharePlayer) {
//        //修改在线玩家
//        this.addOnlinePlayer(sharePlayer);
//        //修改玩家信息
//        this.addPlayer(sharePlayer);
//    }
//
//    /**
//     * 删除在线玩家
//     *
//     * @param pid
//     */
//    public void removeOnlinePlayer(long pid) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        mongoDBHelper.deleteById(pid, SHARE_ONLINE_PLAYER_KEY);
//        //删除在线玩家基础信息
//        removeOnlinePlayerSimple(pid);
//
//
//    }
//
//    public void addPlayer(SharePlayer sharePlayer) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        sharePlayer.setId(sharePlayer.getPlayerBO().getId());
//        sharePlayer.setRoomId(sharePlayer.getRoomInfo().getRoomId());
//        sharePlayer.setClubId(sharePlayer.getRoomInfo().getClubId());
//        sharePlayer.setUnionId(sharePlayer.getRoomInfo().getUnionId());
//        sharePlayer.setNodeIp(sharePlayer.getCurShareNode().getIp());
//        sharePlayer.setNodePort(sharePlayer.getCurShareNode().getPort());
//        if (!mongoDBHelper.existById(sharePlayer.getId(), SHARE_ALL_PLAYER_KEY)) {
//            mongoDBHelper.insert(sharePlayer, SHARE_ALL_PLAYER_KEY);
//        } else {
//            mongoDBHelper.updateById(sharePlayer.getId(), SHARE_ALL_PLAYER_KEY, sharePlayer);
//        }
//    }
//
//    /**
//     * 获取所有在线玩家
//     *
//     * @return
//     */
//    public HashMap<Long, SharePlayer> onlineSharePlayers(Long clubId, Long unionId) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (clubId != null) {
//            filters = Filters.eq("clubId", clubId);
//        } else if (unionId != null) {
//            filters = Filters.eq("unionId", unionId);
//        }
//        List<SharePlayer> list = mongoDBHelper.selectByQuery(SHARE_ONLINE_PLAYER_KEY, filters, null, SharePlayer.class, null, null);
//        HashMap<Long, SharePlayer> players = new HashMap<>(list.size());
//        list.forEach(data -> {
//            players.put(data.getId(), data);
//        });
//        return players;
//    }
//
//    /**
//     * 获取所有在线玩家
//     *
//     * @return
//     */
//    public HashMap<Long, SharePlayer> onlineSharePlayers() {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        List<SharePlayer> list = mongoDBHelper.selectList(SHARE_ONLINE_PLAYER_KEY, SharePlayer.class, null, null);
//        HashMap<Long, SharePlayer> players = new HashMap<>(list.size());
//        list.forEach(data -> {
//            players.put(data.getId(), data);
//        });
//        return players;
//    }
//
//    /**
//     * 获取所有玩家
//     *
//     * @return
//     */
//    public HashMap<Long, SharePlayer> allSharePlayers() {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        List<SharePlayer> list = mongoDBHelper.selectList(SHARE_ALL_PLAYER_KEY, SharePlayer.class, null, null);
//        HashMap<Long, SharePlayer> players = new HashMap<>(list.size());
//        list.forEach(data -> {
//            players.put(data.getId(), data);
//        });
//        return players;
//    }
//
//    /**
//     * 根据房间Id获取所有玩家
//     *
//     * @return
//     */
//    public HashMap<Long, SharePlayer> allSharePlayersByRoomId(Long roomId) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        HashMap<Long, SharePlayer> players = new HashMap<>();
//        if (roomId != null && roomId > 0L) {
//            Bson filters = Filters.eq("roomId", roomId);
//            List<SharePlayer> list = mongoDBHelper.selectByQuery(SHARE_ALL_PLAYER_KEY, filters, null, SharePlayer.class, null, null);
//            list.forEach(data -> {
//                players.put(data.getId(), data);
//            });
//        }
//        return players;
//    }
//
//    /**
//     * 添加在线玩家基础信息
//     *
//     * @param sharePlayer
//     */
//    public void addOnlinePlayerSimple(SharePlayer sharePlayer) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        if (!mongoDBHelper.existById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_SIMPLE_KEY)) {
//            mongoDBHelper.insert(changeSimple(sharePlayer), SHARE_ONLINE_PLAYER_SIMPLE_KEY);
//        } else {
//            mongoDBHelper.updateById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_SIMPLE_KEY, changeSimple(sharePlayer));
//        }
//    }
//
//    /**
//     * 修改在线玩家基础信息
//     *
//     * @param sharePlayer
//     */
//    public void updateOnlinePlayerSimple(SharePlayer sharePlayer) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        if (mongoDBHelper.existById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_SIMPLE_KEY)) {
//            mongoDBHelper.updateById(sharePlayer.getId(), SHARE_ONLINE_PLAYER_SIMPLE_KEY, changeSimple(sharePlayer));
//        }
//    }
//
//    /**
//     * 删除在线玩家基础信息
//     *
//     * @param pid
//     */
//    public void removeOnlinePlayerSimple(long pid) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        mongoDBHelper.deleteById(pid, SHARE_ONLINE_PLAYER_SIMPLE_KEY);
//    }
//
//    /**
//     * 获取所有在线玩家
//     *
//     * @return
//     */
//    public HashMap<Long, ShareOnlinePlayerSimple> shareOnlinePlayerSimples() {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        //指定查询过滤器
//        List<ShareOnlinePlayerSimple> list = mongoDBHelper.selectByQuery(SHARE_ONLINE_PLAYER_SIMPLE_KEY, null, null, ShareOnlinePlayerSimple.class, null, null);
//        HashMap<Long, ShareOnlinePlayerSimple> players = new HashMap<>(list.size());
//        list.forEach(data -> {
//            players.put(data.getId(), data);
//        });
//        return players;
//    }
//
//    private ShareOnlinePlayerSimple changeSimple(SharePlayer sharePlayer) {
//        ShareOnlinePlayerSimple shareOnlinePlayerSimple = new ShareOnlinePlayerSimple();
//        shareOnlinePlayerSimple.setId(sharePlayer.getPlayerBO().getId());
//        shareOnlinePlayerSimple.setClubId(sharePlayer.getRoomInfo().getClubId());
//        shareOnlinePlayerSimple.setUnionId(sharePlayer.getRoomInfo().getUnionId());
//        shareOnlinePlayerSimple.setConfigId(sharePlayer.getRoomInfo().getConfigId());
//        shareOnlinePlayerSimple.setName(sharePlayer.getCurShareNode().getName());
//        shareOnlinePlayerSimple.setVipAddress(sharePlayer.getCurShareNode().getVipAddress());
//        shareOnlinePlayerSimple.setIp(sharePlayer.getCurShareNode().getIp());
//        shareOnlinePlayerSimple.setPort(sharePlayer.getCurShareNode().getPort());
//        shareOnlinePlayerSimple.setRoomId(sharePlayer.getRoomInfo().getRoomId());
//        shareOnlinePlayerSimple.setRoomKey(sharePlayer.getRoomInfo().getRoomKey());
//        return shareOnlinePlayerSimple;
//    }
//
//    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
//    private static class SingletonHolder {
//        // 静态初始化器，由JVM来保证线程安全
//        private static MongoDbPlayerMgr instance = new MongoDbPlayerMgr();
//    }
//}
