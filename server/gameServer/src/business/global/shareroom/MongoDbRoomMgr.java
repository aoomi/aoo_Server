//package business.global.shareroom;
//
//import com.ddm.server.common.mongodb.MongoDBHelper;
//import com.mongodb.client.model.Filters;
//import core.ioc.ContainerMgr;
//import org.bson.conversions.Bson;
//
//import java.util.HashMap;
//import java.util.List;
//
//public class MongoDbRoomMgr {
//    //房间存储KEy
//    private static final String SHARE_ROOM_KEY = "shareRoomKey";
//    private MongoDbRoomMgr() {
//    }
//
//    public static MongoDbRoomMgr getInstance() {
//        return MongoDbRoomMgr.SingletonHolder.instance;
//    }
//    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
//    private static class SingletonHolder {
//        // 静态初始化器，由JVM来保证线程安全
//        private static MongoDbRoomMgr instance = new MongoDbRoomMgr();
//    }
//
//    /**
//     * 添加房间mongodb
//     * @param shareRoom
//     */
//    public void addShareRoom(ShareRoom shareRoom){
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        shareRoom.setNodeIp(shareRoom.getCurShareNode().getIp());
//        shareRoom.setNodePort(shareRoom.getCurShareNode().getPort());
//        if (!mongoDBHelper.existById(shareRoom.getId(), SHARE_ROOM_KEY)) {
//            mongoDBHelper.insert(shareRoom, SHARE_ROOM_KEY);
//        } else {
//            mongoDBHelper.updateById(shareRoom.getId(), SHARE_ROOM_KEY, shareRoom);
//        }
//    }
//
//    /**
//     * 移除房间
//     *
//     * @param id
//     */
//    public void removeShareRoom(String id) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        mongoDBHelper.deleteById(id, SHARE_ROOM_KEY);
//    }
//
//    /**
//     * 亲友圈房间
//     * @param clubId
//     * @return
//     */
//    public HashMap<String, ShareRoom> allShareRoomsByClubId(Long clubId){
//        return allShareRooms(clubId, null, null, null);
//    }
//
//    /**
//     * 赛事房间
//     * @param unionId
//     * @return
//     */
//    public HashMap<String, ShareRoom> allShareRoomsByUnionId(Long unionId){
//        return allShareRooms(unionId, null, null, null);
//    }
//
//    /**
//     * 端口下的房间
//     * @param nodeIp
//     * @param port
//     * @return
//     */
//    public HashMap<String, ShareRoom> allShareRoomsByIpPort(String nodeIp, Integer port){
//        return allShareRooms(null, null, nodeIp, port);
//    }
//
//    /**
//     * 获取所有房间
//     * @return
//     */
//    public HashMap<String, ShareRoom> allShareRooms(){
//        return allShareRooms(null, null, null, null);
//    }
//
//    /**
//     * 获取所有房间数据
//     *
//     * @return
//     */
//    public HashMap<String, ShareRoom> allShareRooms(Long clubId, Long unionId, String nodeIp, Integer nodePort) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (clubId != null) {
//            filters = Filters.eq("clubId", clubId);
//        } else if (unionId != null) {
//            filters = Filters.eq("unionId", unionId);
//        } else if (nodeIp != null && nodePort!=null){
//            filters = Filters.and(Filters.eq("nodeIp", nodeIp),Filters.eq("nodePort", nodePort));
//        }
//        List<ShareRoom> list = mongoDBHelper.selectByQuery(SHARE_ROOM_KEY, filters, null, ShareRoom.class, null, null);
//        HashMap<String, ShareRoom> shareRooms = new HashMap<>(list.size());
//        list.forEach(data -> {
//            shareRooms.put(data.getId(), data);
//        });
//        return shareRooms;
//    }
//
//
//}
