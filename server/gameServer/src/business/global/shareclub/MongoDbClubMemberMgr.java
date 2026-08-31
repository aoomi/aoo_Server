//package business.global.shareclub;
//
//import business.global.club.ClubMember;
//import com.ddm.server.common.mongodb.MongoDBHelper;
//import com.mongodb.client.model.Filters;
//import core.ioc.ContainerMgr;
//import org.bson.conversions.Bson;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 亲友圈玩家缓存到mongodb
// */
//public class MongoDbClubMemberMgr {
//    //亲友圈玩家缓存Key
//    private static final String SHARE_CLUB_MEMBER_KEY = "shareClubMemberKey";
//
//    private MongoDbClubMemberMgr() {
//    }
//
//    public static MongoDbClubMemberMgr getInstance() {
//        return MongoDbClubMemberMgr.SingletonHolder.instance;
//    }
//
//    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
//    private static class SingletonHolder {
//        // 静态初始化器，由JVM来保证线程安全
//        private static MongoDbClubMemberMgr instance = new MongoDbClubMemberMgr();
//    }
//
//    /**
//     * 添加成员到mongodb
//     * @param clubMember
//     */
//    public void addClubMember(ClubMember clubMember){
//        clubMember.setId(clubMember.getClubMemberBO().getId());
//        clubMember.setClubID(clubMember.getClubMemberBO().getClubID());
//        clubMember.setPlayerID(clubMember.getClubMemberBO().getPlayerID());
//        clubMember.setIsminister(clubMember.getClubMemberBO().getIsminister());
//        clubMember.setStatus(clubMember.getClubMemberBO().getStatus());
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        if (!mongoDBHelper.existById(clubMember.getId(), SHARE_CLUB_MEMBER_KEY)) {
//            mongoDBHelper.insert(clubMember, SHARE_CLUB_MEMBER_KEY);
//        } else {
//            mongoDBHelper.updateById(clubMember.getId(), SHARE_CLUB_MEMBER_KEY, clubMember);
//        }
//    }
//
//    /**
//     * 删除成员
//     *
//     * @param id
//     */
//    public void removeClubMember(long id) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        mongoDBHelper.deleteById(id, SHARE_CLUB_MEMBER_KEY);
//    }
//
//
//    /**
//     * 获取所有亲友圈成员
//     *
//     * @return
//     */
//    public HashMap<Long, ClubMember> getAllClubMember() {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        List<ClubMember> list = mongoDBHelper.selectByQuery(SHARE_CLUB_MEMBER_KEY, null, null, ClubMember.class, null, null);
//        HashMap<Long, ClubMember> clubMembers = new HashMap<>(list.size());
//        list.forEach(data -> {
//            clubMembers.put(data.getId(), data);
//        });
//        return clubMembers;
//    }
//
//    /**
//     * 获取一个亲友圈成员
//     *
//     * @return
//     */
//    public Map<Long, ClubMember> getAllOneClubMember(Long clubId) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (clubId != null) {
//            filters = Filters.eq("clubID", clubId);
//        }
//        List<ClubMember> list = mongoDBHelper.selectByQuery(SHARE_CLUB_MEMBER_KEY, filters, null, ClubMember.class, null, null);
//        HashMap<Long, ClubMember> clubMembers = new HashMap<>(list.size());
//        list.forEach(data -> {
//            clubMembers.put(data.getId(), data);
//        });
//        return clubMembers;
//    }
//
//    /**
//     * 获取一个玩家的所有亲友圈成员信息
//     * @param playerId
//     * @return
//     */
//    public Map<Long, ClubMember> getAllOnePlayerClubMember(Long playerId) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (playerId != null) {
//            filters = Filters.eq("playerID", playerId);
//        }
//        List<ClubMember> list = mongoDBHelper.selectByQuery(SHARE_CLUB_MEMBER_KEY, filters, null, ClubMember.class, null, null);
//        HashMap<Long, ClubMember> clubMembers = new HashMap<>(list.size());
//        list.forEach(data -> {
//            clubMembers.put(data.getId(), data);
//        });
//        return clubMembers;
//    }
//
//    /**
//     * 获取几个亲友圈的玩家信息
//     * @param clubIdList
//     * @return
//     */
//    public List<Long> findClubIdAllClubMemberOnline(List<Long> clubIdList) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (clubIdList != null) {
//            List<Bson> filterList =  new ArrayList<>();
//            for(Long clubId:clubIdList){
//                filterList.add(Filters.eq("clubID", clubId));
//                filters = Filters.or(filterList);
//            }
//        }
//        List<ClubMember> list = mongoDBHelper.selectByQuery(SHARE_CLUB_MEMBER_KEY, filters, null, ClubMember.class, null, null);
//        List<Long> clubMemberIds = list.stream().map(k->k.getId()).collect(Collectors.toList());
//        return clubMemberIds;
//    }
//
//    /**
//     * 获取几个亲友圈的玩家信息
//     * @param clubIdList
//     * @return
//     */
//    public Map<Long, ClubMember> getAllClubMemberByClubIds(List<Long> clubIdList) {
//        MongoDBHelper mongoDBHelper = ContainerMgr.get().getMongoDb();
//        Bson filters = null;
//        if (clubIdList != null) {
//            List<Bson> filterList =  new ArrayList<>();
//            for(Long clubId:clubIdList){
//                filterList.add(Filters.eq("clubID", clubId));
//                filters = Filters.or(filterList);
//            }
//        }
//        List<ClubMember> list = mongoDBHelper.selectByQuery(SHARE_CLUB_MEMBER_KEY, filters, null, ClubMember.class, null, null);
//        HashMap<Long, ClubMember> clubMembers = new HashMap<>(list.size());
//        list.forEach(data -> {
//            clubMembers.put(data.getId(), data);
//        });
//        return clubMembers;
//    }
//}
