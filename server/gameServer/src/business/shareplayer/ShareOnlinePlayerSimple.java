package business.shareplayer;

import lombok.Data;

/**
 * 在线玩家基础信息类
 */
@Data
//@MongoDbs({@MongoDb(doc = @Document(collection = "ShareOnlinePlayerSimpleKey"), indexes = @CompoundIndexes({@CompoundIndex(name = "pid_state",def = "{'pid':1,'state':1}"),@CompoundIndex(name = "clubId_1",def = "{'clubId':1}")}))})
public class ShareOnlinePlayerSimple {
    //玩家ID
    private long id;
    //玩家亲友圈ID
    private long clubId;
    //玩家联盟ID
    private long unionId;
    //节点名称
    private String name;
    //节点地址
    private String vipAddress;
    //节点ip
    private String ip;
    //节点端口
    private Integer port;
    //房间Id
    private long roomId;
    //房间号
    private String roomKey;
    //配置Id
    private long configId;
}
