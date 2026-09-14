package com.ddm.server.common.ehcache;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.ehcache.Cache;
import org.ehcache.config.CacheConfiguration;

import java.util.Iterator;

/**
 * 空缓存配置
 * 找不到缓存配置时，调用空配置，放在出现异常
 */
@Data
@NoArgsConstructor
@EhCacheConfiguration
public class NoneCacheConfiguration extends BaseCacheConfiguration {

    /**
     * 缓存配置
     *
     * @return
     */
    @Override
    public CacheConfiguration getConfiguration() {
        return null;
    }


    @Override
    public void delete(String key) {
        return;
    }

    @Override
    public void clear() {
        return;
    }

    @Override
    public boolean compareAndSet(String key, Object findValue, Object setValue) throws UnsupportedOperationException {
        return false;
    }

    @Override
    public void dispose() {
        return;
    }

    @Override
    public Iterator<Cache.Entry> entrys() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public boolean keyExists(String key) {
        return false;
    }

    @Override
    public void touch(String key) {
        return;
    }

    @Override
    public void put(String key, Object value) {

    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return null;
    }
}
