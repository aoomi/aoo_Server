package com.ddm.server.common.ehcache;

import BaseCommon.CommLog;
import com.ddm.server.common.utils.ClassScanner;
import lombok.Data;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Data
public class EhCacheFactory {
    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static EhCacheFactory instance = new EhCacheFactory();
    }

    // 私有化构造方法
    private EhCacheFactory() {
    }

    private static volatile String gameId = "";

    public static void setGameId(String value) {
        gameId = value == null ? "" : value;
    }

    // 获取单例
    public static EhCacheFactory getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 配置管理器
     */
    private final Map<String, CacheAdapter> cacheConfigurationMap = new ConcurrentHashMap<>();

    /**
     * 默认空配置
     */
    private final static NoneCacheConfiguration NONE_CACHE_CONFIGURATION = new NoneCacheConfiguration();

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(cacheDirectory()))
            //持久化硬盘路径
            .withCache("preConfigured", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                    .heap(40, MemoryUnit.MB) //堆内，单位可选KB、MB、GB，也可设缓存的单位个数
                                    .disk(10, MemoryUnit.GB, true))
                    .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(1)))) //硬盘，第三个boolean参数设置是否持久化
            .build(true);//创建并初始化

    private static String cacheDirectory() {
        String nodeId = gameId.isBlank() ? System.getProperty("game_sid", "default") : gameId;
        return BaseCacheConfiguration.DISK_CACHE_DIR + File.separator + nodeId.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * 路径配置
     */
    private final String SCAN_PATH = "com.ddm.server.common.ehcache.configuration";

    public void initalize() {
        Set<Class<?>> ehCacheConfigurations = ClassScanner.listClassesWithAnnotation(SCAN_PATH, EhCacheConfiguration.class);
        for (Class<?> ehCacheConfiguration : ehCacheConfigurations) {
            try {
                CacheAdapter handler = (CacheAdapter) ehCacheConfiguration.getDeclaredConstructor().newInstance();
                Method[] methods = ehCacheConfiguration.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(EhcacheInit.class)) {
                        // 设置方法为可执行的
                        method.setAccessible(true);
                        // 带注解的调用运行
                        method.invoke(handler, this.getCacheManager());
                        this.getCacheConfigurationMap().put(handler.getClass().getName(), handler);
                        break;
                    }
                }
            } catch (Exception e) {
                CommLog.error("EhCacheFactory initalize Exception Message:{}", e.getMessage(), e);
            }
        }
    }


    /**
     * 获取缓存Api操作接口
     *
     * @param clazz 配置类
     * @return
     */
    public static CacheAdapter getCacheApi(Class clazz) {
        return getInstance().getCacheAdapter(clazz);
    }


    /**
     * 获取配置
     *
     * @param clazz 配置类
     * @return
     */
    public CacheAdapter getCacheAdapter(Class clazz) {
        // 获取配置接口
        CacheAdapter cacheAdapter = getCacheConfigurationMap().get(clazz.getName());
        if (Objects.isNull(cacheAdapter)) {
            // 是否默认空配置
            CommLog.error("error CacheConfiguration class name:{}", clazz.getName());
            return NONE_CACHE_CONFIGURATION;
        }
        return cacheAdapter;
    }


}
