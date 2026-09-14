package business.global.sharegm;

import BaseCommon.CommLog;
import business.shareplayer.ShareNode;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisMap;
import com.google.gson.Gson;
import core.ioc.ContainerMgr;

import java.util.*;

/**
 * @author xsj
 * @date 2020/8/28 16:15
 * @description 共享服务器节点信息
 */
public class ShareNodeServerMgr {
    private static ShareNodeServerMgr instance = new ShareNodeServerMgr();
    //检测节点失效时间
    private final long CHECK_TIME_OUT = 60000 * 3 + 100;
    //服务器节点信息
    private final String SHARE_NODE_SERVER_KEY = "shareNodeServerKey";
    private final String nodeName;
    private final String nodeVipAddress;
    private final String nodeIp;
    private final Integer nodePort;

    private ShareNodeServerMgr() {
        this.nodeName = Config.nodeName();
        this.nodeVipAddress = Config.nodeVipAddress();
        this.nodePort = Config.nodePort();
        this.nodeIp = Config.nodeIp();
    }

    public static ShareNodeServerMgr getInstance() {
        return instance;
    }

    /**
     * 初始化节点
     */
    public void init() {
        addOrUpdate(System.currentTimeMillis());
    }

    /**
     * 添加或更新服务器节点
     */
    public void addOrUpdate() {
        addOrUpdate(null);
    }

    /**
     * 添加或更新服务器节点
     *
     * @param createTime
     */
    private void addOrUpdate(Long createTime) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_NODE_SERVER_KEY);
        String data = redisMap.get(this.nodeIp + this.nodePort);
        Gson gson = new Gson();
        if (data != null) {
            ShareNodeServer shareNodeServer = gson.fromJson(data, ShareNodeServer.class);
            shareNodeServer.setLastHeartTime(System.currentTimeMillis());
            if (shareNodeServer.getStatus() == -1 || (System.currentTimeMillis() - shareNodeServer.getLastHeartTime()) > CHECK_TIME_OUT) {
                shareNodeServer.setStartTime(System.currentTimeMillis());
                shareNodeServer.setStatus(0);
            }
            if (createTime != null) {
                shareNodeServer.setStartTime(createTime);
            }
            redisMap.put(this.nodeIp + this.nodePort, gson.toJson(shareNodeServer));
        } else {
            ShareNodeServer shareNodeServer = new ShareNodeServer();
            ShareNode shareNode = new ShareNode(nodeName, nodeVipAddress, nodeIp, nodePort);
            shareNodeServer.setShareNode(shareNode);
            shareNodeServer.setStartTime(System.currentTimeMillis());
            shareNodeServer.setLastHeartTime(System.currentTimeMillis());
            shareNodeServer.setStatus(0);
            redisMap.put(this.nodeIp + this.nodePort, gson.toJson(shareNodeServer));
        }
    }

    /**
     * 停止当前节点
     */
    public void stopNodeServer() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_NODE_SERVER_KEY);
        String data = redisMap.get(this.nodeIp + this.nodePort);
        Gson gson = new Gson();
        if (data != null) {
            ShareNodeServer shareNodeServer = gson.fromJson(data, ShareNodeServer.class);
            shareNodeServer.setStatus(-1);
            redisMap.put(this.nodeIp + this.nodePort, gson.toJson(shareNodeServer));
        }
    }

    /**
     * 检测当前节点是否存活
     *
     * @return
     */
    public boolean checkIsLiveCurrent() {
        return checkIsLive(this.nodeIp, this.nodePort);
    }

    /**
     * 根据ip检测存活
     *
     * @param nodeIp
     * @param nodePort
     * @return
     */
    public boolean checkIsLiveByIpPort(String nodeIp, Integer nodePort) {
        return checkIsLive(nodeIp, nodePort);
    }

    /**
     * 检查节点是否存活
     */
    private boolean checkIsLive(String nodeIp, Integer nodePort) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_NODE_SERVER_KEY);
        String data = redisMap.get(nodeIp + nodePort);
        Gson gson = new Gson();
        if (data != null) {
            ShareNodeServer shareNodeServer = gson.fromJson(data, ShareNodeServer.class);
            //这个时间一定时任务设置为准,目前定时任务设置1分钟,超过3次没有收到检测就证明节点断了
            if (shareNodeServer.getStatus() == 0 && (System.currentTimeMillis() - shareNodeServer.getLastHeartTime()) < CHECK_TIME_OUT) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 检查是否是当前节点
     *
     * @param nodeIp
     * @param nodePort
     * @return
     */
    public boolean checkCurrentNode(String nodeIp, Integer nodePort) {
        if (this.nodeIp.equals(nodeIp) && this.nodePort.equals(nodePort)) {
            return true;
        }
        return false;
    }

    /**
     * 获取所有共享节点信息
     *
     * @return
     */
    public List<ShareNodeServer> allShareNodes() {
        Gson gson = new Gson();
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_NODE_SERVER_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        List<ShareNodeServer> nodes = new ArrayList<>(allSet.size());
        allSet.forEach((k) -> {
            try {
                ShareNodeServer shareNodeServer = gson.fromJson(k.getValue(), ShareNodeServer.class);
                nodes.add(shareNodeServer);
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                CommLog.error(e.getMessage(), e);
            }
        });
        return nodes;
    }

    /**
     * 获取当前节点
     *
     * @return
     */
    public ShareNode getThisNode() {
        ShareNode shareNode = new ShareNode();
        shareNode.setIp(nodeIp);
        shareNode.setPort(nodePort);
        shareNode.setName(nodeName);
        shareNode.setVipAddress(nodeVipAddress);
        return shareNode;
    }

    /**
     * 检测当前节点是否是大厅主节点
     *
     * @return
     */
    public boolean checkIsMasterHall() {
        List<ShareNodeServer> allShareNodes = allShareNodes();
        Optional<ShareNode> shareNode = allShareNodes.stream()
                .filter(k -> k.getShareNode().getName().startsWith("hall") && checkIsLive(k.getShareNode().getIp(), k.getShareNode().getPort()))
                .map(ShareNodeServer::getShareNode).sorted(Comparator.comparing(ShareNode::getPort)).findFirst();
        return shareNode.map(node -> checkCurrentNode(node.getIp(), node.getPort())).orElse(false);
    }
}
