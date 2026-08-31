package com.ddm.server.common.ehcache;

import com.ddm.server.common.utils.KryoUtil;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;

import java.util.Iterator;
import java.util.Objects;

@Data
public abstract class BaseCacheConfiguration implements CacheAdapter {
    /**
     * 磁盘文件路径
     */
    // public static final String DISK_CACHE_DIR =System.getProperty("/common/bin/conf/ehcache") ;
    public static final String DISK_CACHE_DIR = System.getProperty("user.dir") + "/common/bin/conf/ehcache";
    /**
     * 缓存管理器
     */
    private CacheManager cacheManager = null;

    /**
     * 缓存对象
     */
    private Cache cache = null;

    @Override
    public void init(CacheManager cacheManager) {
    }


    /**
     * 缓存存值
     *
     * @param key   健
     * @param value 值
     */
    @Override
    public void put(String key, Object value) {
        if (ObjectUtils.allNotNull(key, value)) {
            getCache().put(key, KryoUtil.Serializer(value));
        }
    }

    /**
     * 获取缓存值
     *
     * @param key   健
     * @param clazz 转换类
     * @param <T>
     * @return
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        if (ObjectUtils.allNotNull(key, clazz)) {
            Object value = getCache().get(key);
            return Objects.nonNull(value) ? KryoUtil.Deserializer((byte[]) value) : null;
        }
        return null;
    }


    @Override
    public void delete(String key) {
        this.getCache().remove(key);
    }

    @Override
    public void clear() {
        this.getCache().clear();
    }

    @Override
    public boolean compareAndSet(String key, Object findValue, Object setValue) throws UnsupportedOperationException {
        return ObjectUtils.allNotNull(key, findValue, setValue) ? this.getCache().replace(key, findValue, setValue) : false;
    }


    @Override
    public Iterator<Cache.Entry> entrys() throws UnsupportedOperationException {
        return this.getCache().iterator();
    }

    @Override
    public boolean keyExists(String key) {
        return StringUtils.isNotEmpty(key) && this.getCache().containsKey(key);
    }

    @Override
    public void touch(String key) {
        if (StringUtils.isNotEmpty(key)) {
            this.getCache().get(key);
        }
    }
}
