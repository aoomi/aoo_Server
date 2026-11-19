package com.ddm.server.common.ehcache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;

import java.util.Iterator;

public interface CacheAdapter {

    /**
     * 初始化配置
     */
    @EhcacheInit
    public void init(CacheManager cacheManager);


    /**
     * 获取缓存配置
     *
     * @return 缓存配置
     */
    public CacheConfiguration getConfiguration();

    /**
     * 添加缓存
     *
     * @param key
     * @param value
     */
    public void put(String key, Object value);

    /**
     * 获取缓存
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T get(String key, Class<T> clazz);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 清空当前缓存
     */
    void clear();

    /**
     * 判断缓存中的当前值，并存入一个新值。注意，某些缓存（如单线程的 Redis）不支持该操作。
     *
     * @param key       键
     * @param findValue 要比较的值。仅当缓存中的当前值与 findValue 相等时，才会放入新值
     * @param setValue  新值
     * @return 是否成功
     * @throws UnsupportedOperationException 如果缓存不支持该操作
     */
    boolean compareAndSet(String key, Object findValue, Object setValue) throws UnsupportedOperationException;

    /**
     * 关闭缓存客户端对象，释放相关资源
     */
    void dispose();

    /**
     * 列出所有 实体
     *
     * @return 所有 key
     * @throws UnsupportedOperationException 如果缓存不支持该操作
     */
    Iterator<Cache.Entry> entrys() throws UnsupportedOperationException;

    /**
     * 判断 key 是否存在。如果是为了取值，请直接把值取过来再判断是否为空。
     *
     * @param key 要查找的 key
     * @return 如果 key 存在则返回 true
     */
    boolean keyExists(String key);

    void touch(String key);


}
