package business.global.config;

import BaseCommon.CommLog;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.common.redis.RedisSet;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NetIpMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static NetIpMgr instance = new NetIpMgr();
    }

    // 私有化构造方法
    private NetIpMgr() {
    }

    // 获取单例
    public static NetIpMgr getInstance() {
        return NetIpMgr.SingletonHolder.instance;
    }

    /**
     * 服务节点Ip
     */
    private final Set<String> serverNodeIpSet = new HashSet<>();

    private final RedisSet redisSet = RedisSource.getSetV(RedisBydrKeyEnum.IP_SERVER_NODE_SET.getKey());

    private Searcher searcher;

    public void init(String configPath){
        if (this.redisSet.size() > 0 ) {
            serverNodeIpSet.addAll(this.redisSet.getSet());
        }
        this.initSearcher(configPath);
    }

    public void initSearcher(String configPath) {
        String dbPath = String.format("%s/ip2region.xdb", configPath);
        // 1、从 dbPath 加载整个 xdb 到内存。
        LongByteArray cBuff;
        try {
            cBuff = Searcher.loadContentFromFile(dbPath);
        } catch (Exception e) {
            CommLog.error("failed to load content from dbPath:{}",dbPath,e);
            return;
        }

        // 2、使用上述的 cBuff 创建一个完全基于内存的查询对象。
        try {
            this.searcher = Searcher.newWithBuffer(Version.IPv4, cBuff);
        } catch (Exception e) {
            CommLog.error("failed to create content cached searcher dbPath:{}",dbPath,e);
            return;
        }
    }

    /**
     * 国外ip地址查询
     * @param ip
     * @return
     */
    public String abroadRegion(String ip) {
        if (Objects.isNull(this.searcher)){
            return null;
        }

        // 3、查询
        try {
            String region = searcher.search(ip);
            if (StringUtils.isNotEmpty(region)) {
                String[] splitRegion= region.split("\\|");
                String name = splitRegion[0];
                if ("中国".equals(name) || "0".equals(name)) {
                    // 不对国内Ip或者局域网ip进行处理
                    return null;
                }
                int size = splitRegion.length -1;
                StringBuilder stringBuilder = new StringBuilder();
                for (int i =0;i< size;i++) {
                    String value = splitRegion[i];
                    if ("0".equals(value)) {
                        continue;
                    }
                    stringBuilder.append(value);
                }
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            CommLog.error("failed to search ip:{}",ip,e);
        }
        return null;
    }


    /**
     * 查询ip
     * @param ip
     * @return
     */
    public boolean existIp(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        return this.serverNodeIpSet.contains(ip);
    }

    /**
     * 添加ip
     * @param ip
     */
    public void add(String ip) {
        if(StringUtils.isEmpty(ip)) {
            return;
        }
        if (StringUtil.isboolIp(ip)) {
            this.redisSet.add(ip);
            this.serverNodeIpSet.add(ip);
        }
    }

    /**
     * 添加Ip通知
     * @param ip
     */
    public void addNotify(String ip) {
        if(StringUtils.isEmpty(ip)) {
            return;
        }
        if (StringUtil.isboolIp(ip)) {
            MqProducerMgr.get().send(MqTopic.IP_SERVER_NODE_SET_NOTIFY, new MqUrgentMaintainServerBo(ip));
        }
    }




}
