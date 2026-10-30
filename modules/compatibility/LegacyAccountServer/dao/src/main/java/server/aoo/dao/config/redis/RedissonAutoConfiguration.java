package server.aoo.dao.config.redis;

import com.ddm.server.common.utils.CommLog;
import com.ddm.server.redis.jedis.RedisClient;
import com.ddm.server.redis.jedis.RedisUtil;
import com.ddm.server.redis.jedis.impl.RedisClientImpl;
import com.ddm.server.redis.redission.DistributedLocker;
import com.ddm.server.redis.redission.RedissLockUtil;
import com.ddm.server.redis.redission.impl.RedissonDistributedLocker;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;

@Configuration
@ConditionalOnClass(Config.class)
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonAutoConfiguration extends CachingConfigurerSupport {

    @Autowired
    private RedissonProperties redissonProperties;

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redissonConnectionFactory){
        RedisTemplate<String,Object> template = new RedisTemplate <>();
        template.setConnectionFactory(redissonConnectionFactory);

        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(new tools.jackson.databind.ObjectMapper());

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jsonSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }


    /**
     * redisson客户端
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();
        SingleServerConfig source = this.redissonProperties.getSingleServerConfig();
        config.useSingleServer()
                .setAddress(source.getAddress())
                .setDatabase(source.getDatabase())
                .setConnectionPoolSize(source.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(source.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(source.getSubscriptionConnectionPoolSize())
                .setSubscriptionConnectionMinimumIdleSize(source.getSubscriptionConnectionMinimumIdleSize());
        config.setPassword(this.redissonProperties.getPassword());
        config.setUsername(source.getUsername());
        config.setThreads(this.redissonProperties.getThreads());
        config.setNettyThreads(this.redissonProperties.getNettyThreads());
        if (this.redissonProperties.getTransportMode() != null) {
            config.setTransportMode(this.redissonProperties.getTransportMode());
        }
        return Redisson.create(config);
    }

    /**
     * 装配locker类，并将实例注入到RedissLockUtil中
     * @return
     */
    @Bean
    DistributedLocker distributedLocker(RedissonClient redissonClient) {
        DistributedLocker locker = new RedissonDistributedLocker();
        ((RedissonDistributedLocker) locker).setRedissonClient(redissonClient);
        RedissLockUtil.setLocker(locker);
        return locker;
    }

    @Bean
    public RedisClient redisClient(RedisTemplate<String,Object> redisTemplate) {
        RedisClient redisClient = new RedisClientImpl();
        ((RedisClientImpl) redisClient).setJedisPool(redisTemplate);
        RedisUtil.setRedisClient(redisClient);
        return redisClient;
    }

}
