package business.global.sharegm;

import BaseCommon.CommLog;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.ShareNodePlayerSize;
import cenum.DefaultEnum;
import cenum.node.NodeStateEnum;
import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.redis.RedisSetTuple;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CollectionUtils;
import com.ddm.server.common.utils.GsonUtils;
import com.google.common.collect.Lists;
import jsproto.c2s.cclass.share.ShareNodeItem;
import jsproto.c2s.cclass.share.ShareNodeServerInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 新的节点管理信息
 */
@Data
public class ShareNewNodeServerMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static ShareNewNodeServerMgr instance = new ShareNewNodeServerMgr();
    }

    // 私有化构造方法
    private ShareNewNodeServerMgr() {
    }

    // 获取单例
    public static ShareNewNodeServerMgr getInstance() {
        return ShareNewNodeServerMgr.SingletonHolder.instance;
    }



    /**
     * 检测节点失效时间
     */
    private final long CHECK_TIME_OUT = 60000 * 3 + 100;

    /**
     * 最小人数
     */
    private final static int MIN_NUMBER = 0;

    /**
     * 最大人数
     */
    private final static int MAX_NUMBER = 99999999;

    /**
     * 取人数
     */
    private final static int MOD = 100000000;

    /**
     * 当前节点Id
     */
    private String curNodeId;


    /**
     * redis排行
     */
    private final RedisSetTuple redisSetTuple = RedisSource.getRedisSetTuple("SHARE_NODE_2_PLAYER_SIZE_SET");

    /**
     * 共享节点
     */
    private final RedisMap shareNodeMap = RedisSource.getMaps(RedisBydrKeyEnum.SHARE_NODE_MAP.getKey());

    /**
     * 当前节点状态（1:异常）
     */
    private int curNodeState = NodeStateEnum.ABNORMAL.ordinal();

    /**
     * 初始化节点
     */
    public void init() {
        // 获取当前节点id
        this.getCurNodeId();
        // 添加或更新服务器节点
        this.addOrUpdate(true);
    }

    /**
     * 添加或更新服务器节点
     *
     */
    public void addOrUpdate(boolean isInit) {
        if (StringUtils.isEmpty(this.getCurNodeId())) {
            CommLog.error("ShareNodeServerMgr not init" );
            return;
        }
        ShareNodeServerInfo dbNodeServer  = this.getShareNodeMap().getBean(this.getCurNodeId(), ShareNodeServerInfo.class);
        if (Objects.nonNull(dbNodeServer)) {
            // 更新节点数据
            dbNodeServer.setId(this.getCurNodeId());
            dbNodeServer.setShareNode(this.getThisNode());
            if (isInit){
                // 初始化节点后为正常状态
                dbNodeServer.setStartTime(System.currentTimeMillis());
                dbNodeServer.setStatus(NodeStateEnum.NORMAL.ordinal());
            }
            dbNodeServer.setLastHeartTime(System.currentTimeMillis());
        } else {
            // 新增节点数据
            dbNodeServer = ShareNodeServerInfo.builder()
                    .id(this.getCurNodeId())
                    .shareNode(this.getThisNode())
                    .startTime(System.currentTimeMillis())
                    .lastHeartTime(System.currentTimeMillis())
                    .status(NodeStateEnum.NORMAL.ordinal()).build();
        }
        this.curUpdate(dbNodeServer,dbNodeServer.getStatus());
    }


    /**
     * 停止当前节点
     */
    public void stopCurNodeServer() {
        if(StringUtils.isEmpty(this.getCurNodeId())) {
            CommLog.error("stopCurNodeServer error" );
            return;
        }
        ShareNodeServerInfo nodeServer = this.getShareNodeMap().getBean(this.getCurNodeId(), ShareNodeServerInfo.class);
        if (Objects.nonNull(nodeServer)) {
            nodeServer.setStatus(NodeStateEnum.ABNORMAL.ordinal());
            this.curUpdate(nodeServer,nodeServer.getStatus());
        }
    }

    /**
     * 当前更新节点服务信息
     * @param nodeServer 节点服务
     * @param curNodeState 节点状态
     */
    public void curUpdate (ShareNodeServerInfo nodeServer,int curNodeState) {
        // 更新当前节点状态
        this.setCurNodeState(curNodeState);
        // 玩家人数
        nodeServer.setPlayerSize(PlayerMgr.getInstance().getOnlinePlayerSizeReal());
        // 每个节点的人数
        this.setCurShareNode2PlayerSize(nodeServer.getPlayerSize());
        // 更新节点信息
        this.getShareNodeMap().putIf(nodeServer.getId(), GsonUtils.toJsonString(nodeServer));
    }

    /**
     * 获取当前节点id
     * @return
     */
    public String getCurNodeId() {
        if (StringUtils.isEmpty(this.curNodeId)) {
            this.curNodeId = String.format("%s:%d", Config.nodeIp(),Config.nodePort());
        }
        return curNodeId;
    }

    /**
     * 获取当前节点
     * @return
     */
    public ShareNodeItem getThisNode(){
        return new ShareNodeItem(Config.nodeName(),Config.nodeVipAddress(),Config.nodeIp(),Config.nodePort());
    }

    /**
     * 设置当前节点人数
     * @param onlinePlayerSize 在线玩家人数
     */
    public void setCurShareNode2PlayerSize(int onlinePlayerSize) {
        if (StringUtils.isNotEmpty(this.getCurNodeId())) {
            this.redisSetTuple.add(new Tuple(this.getCurNodeId(), this.getNodePlayerSizeScore(this.getSignId(), onlinePlayerSize)));
        }
    }


    /**
     * 获取所有共享节点信息
     * @return
     */
    public List<ShareNodeServerInfo> allShareNodes(){
        if (StringUtils.isEmpty(this.getCurNodeId()))  {
            return Collections.emptyList();
        }
        return this.getShareNodeMap().values().stream().map(value->GsonUtils.stringToBean(value.toString(),ShareNodeServerInfo.class)).collect(Collectors.toList());
    }


    /**
     * 获取正常的标记id
     * @param isHall 是否大厅（T:大厅,F:游戏）
     * @return
     */
    public int getNormalSignId(boolean isHall) {
        return isHall ? DefaultEnum.HALL_SERVER_SIGN_ID.value() : DefaultEnum.GAME_SERVER_SIGN_ID.value();
    }

    /**
     * 获取标记id
     * @return
     */
    public int getSignId() {
        return Config.ServerID() >= DefaultEnum.GAME_SERVER_SIGN_ID.value() ? DefaultEnum.GAME_SERVER_SIGN_ID.value() + this.getCurNodeState() : DefaultEnum.HALL_SERVER_SIGN_ID.value() + this.getCurNodeState();
    }

    /**
     * 获取节点人数分数
     * @param signId 服务id
     * @param number 人数
     * @return
     */
    private final double getNodePlayerSizeScore(int signId,int number) {
        return Double.parseDouble(String.format("%d%08d",signId,number));
    }

    /**
     * 存在当前节点异常
     * @return
     */
    public boolean existCurrNodeAbnormal() {
        return this.getCurNodeState() == NodeStateEnum.ABNORMAL.ordinal();
    }

    /**
     * 检查节点是否存活
     * @param nodeIp 节点IP
     * @param nodePort 节点端口
     * @return
     */
    private boolean checkIsLive(String nodeIp, Integer nodePort) {
        return this.checkIsLive(String.format("%s:%d",nodeIp,nodePort));
    }

    /**
     * 检查节点是否存活
     * @param nodeId 节点Id
     * @return
     */
    private boolean checkIsLive(String nodeId) {
        return Objects.nonNull(this.getNode(nodeId,true));
    }

    /**
     * 检查是否是当前节点
     *
     * @param nodeId 节点Id
     * @return
     */
    public boolean checkCurrentNode(String nodeId) {
        return this.getCurNodeId().equals(nodeId);
    }

    /**
     * 检查是否是当前节点
     * @param nodeIp 节点Ip
     * @param nodePort 节点端口
     * @return
     */
    public boolean checkCurrentNode(String nodeIp, Integer nodePort) {
        return Config.nodeIp().equals(nodeIp) && Config.nodePort().equals(nodePort);
    }

    /**
     *  获取节点
     * @param nodeId 节点Id
     * @param isLive 是否存活(T:活，F:所有)
     * @return
     */
    public ShareNodeItem getNode(String nodeId,boolean isLive) {
        if (nodeId.equals(this.getCurNodeId()) && this.getCurNodeState() == NodeStateEnum.NORMAL.ordinal()) {
            // 是我当前节点并状态正常
            return this.getThisNode();
        }
        ShareNodeServerInfo nodeServer = this.getShareNodeMap().getBean(nodeId,ShareNodeServerInfo.class);
        if(Objects.nonNull(nodeServer)) {
            if (isLive) {
                // 获取存活的节点
                if ((nodeServer.getStatus() == NodeStateEnum.NORMAL.ordinal() && (System.currentTimeMillis() - nodeServer.getLastHeartTime()) < CHECK_TIME_OUT)) {
                    // 这个时间一定时任务设置为准,目前定时任务设置1分钟,超过3次没有收到检测就证明节点断了
                    return nodeServer.getShareNode();
                } else {
                    // 标记节点出现异常，并停止
                    MqProducerMgr.get().send(MqTopic.STOP_SHARE_NODE, new MqUrgentMaintainServerBo(nodeServer.getShareNode().getIp(),nodeServer.getShareNode().getPort()));
                }
            } else {
                // 不管节点是否存活
                return nodeServer.getShareNode();
            }
        }
        // 节点有问题
        return null;
    }

    /**
     * 检查所有节点存活状况
     */
    public void checkAllNodeLive() {
        Set<Tuple> sectionObjectSet = this.redisSetTuple.rangeWithScoresAll();
        if (CollectionUtils.isNotEmpty(sectionObjectSet)) {
            for (Tuple tuple:sectionObjectSet) {
                ShareNodeItem shareNodeItem = this.getNode(tuple.getElement(),true );
                int abnormalValue = (int) ((tuple.getScore() / MOD) % 100);
                if (Objects.isNull(shareNodeItem) && abnormalValue <= 0) {
                    // 异常并处于排行列表的节点
                    int playerSize = tuple.getScore() >= MOD ? (int) (tuple.getScore() % MOD) : 0;
                    int newSignId = (int) ((tuple.getScore()/MOD) + NodeStateEnum.ABNORMAL.ordinal());
                    this.redisSetTuple.add(new Tuple(tuple.getElement(), this.getNodePlayerSizeScore(newSignId, playerSize)));
                }
            }
        }
    }



    /**
     * 获取每个节点对应人数列表
     * @return
     */
    public List<ShareNodePlayerSize> allNodeOnlinePlayerSizeList() {
        if (StringUtils.isEmpty(this.getCurNodeId()))  {
            return Collections.emptyList();
        }
        List<ShareNodePlayerSize> allNodeOnlinePlayerSizeList = Lists.newArrayList();
        // 获取节点信息
        Set<Tuple> sectionObjectSet = this.redisSetTuple.rangeWithScoresAll();
        if (CollectionUtils.isEmpty(sectionObjectSet)) {
            // 无数据
            return allNodeOnlinePlayerSizeList;
        }
        for (Tuple typedTuple : sectionObjectSet) {
            ShareNodeItem node = this.getNode(typedTuple.getElement(),false);
            if(Objects.nonNull(node)) {
                // 获取玩家人数
                long playerSize = typedTuple.getScore() >= MOD ? (long) (typedTuple.getScore() % MOD) : 0L;
                int abnormalValue = (int) ((typedTuple.getScore() / MOD) % 100);
                allNodeOnlinePlayerSizeList.add(new ShareNodePlayerSize(node.getName(),node.getVipAddress(),node.getIp(),node.getPort(),abnormalValue <= 0 ? playerSize : -playerSize));
            }
        }
        return allNodeOnlinePlayerSizeList;
    }

    /**
     * 获取游戏id对应的节点列表
     * @return
     */
    public String getGameIdToNodeId(int gameId) {
        Set<String> nodeSet = RedisSource.getSetV(RedisBydrKeyEnum.SHARE_GAME_NODE_SET.getKey(gameId)).value();
        if (CollectionUtils.isEmpty(nodeSet)) {
            CommLog.error("gameIdToNodeId error gameId:{}",gameId);
            return null;
        }
        List<Tuple> tupleList = Lists.newArrayList();
        for (String nodeId : nodeSet) {
            double value = this.redisSetTuple.zscore(nodeId);
            if (value <= MOD) {
                continue;
            }
            int abnormalValue = (int) ((value / MOD) % 100);
            if (abnormalValue <=0) {
                tupleList.add(new Tuple(nodeId,value));
            }
        }
        Tuple tuple = tupleList.stream().sorted(Comparator.comparing(Tuple::getScore)).findFirst().orElse(null);
        return Objects.isNull(tuple) ? null:tuple.getElement();
    }

}
