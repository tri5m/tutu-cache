package io.github.tri5m.tucache.core.cache.impl;

import io.github.tri5m.tucache.core.cache.AbstractTuCacheService;
import io.github.tri5m.tucache.core.localcache.CaffeineCache;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @title: LocalCacheService
 * @author: trifolium.wang
 * @date: 2023/9/19
 * @modified :
 */
@Slf4j
public class LocalCacheService extends AbstractTuCacheService {

    private final CaffeineCache caffeineCache;

    public LocalCacheService() {
        caffeineCache = new CaffeineCache();
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        caffeineCache.put(key, value, timeout < 0 ? null : timeUnit.toMillis(timeout));
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, CaffeineCache.NOT_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return objectConvertBean(caffeineCache.get(key), clazz);
    }

    @Override
    public <T> T get(String key, Class<T> clazz, long timeout, TimeUnit timeUnit) {
        return objectConvertBean(caffeineCache.getAndResetExpire(key, timeout, timeUnit), clazz);
    }

    @Override
    public void delete(String key) {
        caffeineCache.remove(key);
    }

    @Override
    public void deleteKeys(String key) {
        if (key == null) {
            return;
        }

        caffeineCache.removeKeys(key);
    }

}
