package co.tunan.tucache.core.cache.impl;

import co.tunan.tucache.core.cache.AbstractTuCacheService;

import java.util.concurrent.TimeUnit;

/**
 * @title: CaffeineCacheService
 * @author: trifolium.wang
 * @date: 2023/9/19
 * @modified :
 */
public class CaffeineCacheService extends AbstractTuCacheService {
    @Override
    public void set(String key, Object value, long expire, TimeUnit timeUnit) {

    }

    @Override
    public void set(String key, Object value) {

    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T get(String key, Class<T> clazz, long expire, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public void delete(String key) {

    }

    @Override
    public void deleteKeys(String key) {

    }
}
