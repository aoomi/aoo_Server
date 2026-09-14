package server.aoo.dao.config.redis;

import lombok.Data;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.redisson")
public class RedissonProperties {
    /**
     * 单节点配置
     */
    private SingleServerConfig singleServerConfig;
    /** Redis credentials are global in Redisson 4.x. */
    private String password;
    /**
     * 线程池数量,默认值: 当前处理核数量 * 2
     */
    private int threads = 16;
    /**
     * Netty线程池数量,默认值: 当前处理核数量 * 2
     */
    private int nettyThreads = 32;
    /**
     * 传输模式
     */
    private TransportMode transportMode;

}
