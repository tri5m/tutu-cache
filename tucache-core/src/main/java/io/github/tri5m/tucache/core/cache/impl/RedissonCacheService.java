package io.github.tri5m.tucache.core.cache.impl;

import io.github.tri5m.tucache.core.cache.AbstractTuCacheService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存为redisson的实现
 *
 * @author: trifolium.wang
 * @date: 2025/1/3
 */
public class RedissonCacheService extends AbstractTuCacheService {

    private final RedissonClient redissonClient;

    public RedissonCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        redissonClient.getBucket(key).set(value, Duration.ofMillis(timeUnit.toMillis(timeout)));
    }

    @Override
    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public <T> T get(String key, Class<T> clazz, long timeout, TimeUnit timeUnit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        if (timeout >= 0) {
            bucket.expire(Duration.ofMillis(timeUnit.toMillis(timeout)));
        }
        return bucket.get();
    }

    @Override
    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public void deleteKeys(String key) {
        String k = key;
        if (!key.endsWith("*")) {
            k = key + "*";
        }
        redissonClient.getKeys().deleteByPattern(k);
    }
}
