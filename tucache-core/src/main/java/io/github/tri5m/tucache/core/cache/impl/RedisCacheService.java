package io.github.tri5m.tucache.core.cache.impl;

import io.github.tri5m.tucache.core.cache.AbstractTuCacheService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The default TuCacheService implementation class
 *
 * @author: wangxudong
 * @date: 2019/3/14
 */
public class RedisCacheService extends AbstractTuCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final long NOT_EXPIRE = -1;

    @Override
    public void set(String key, Object value, long expire, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value);
        if (expire != NOT_EXPIRE) {
            redisTemplate.expire(key, expire, timeUnit);
        }
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, NOT_EXPIRE, null);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void deleteKeys(String key) {
        String k = key;
        if (!key.endsWith("*")) {
            k = key + "*";
        }
        Set<String> keys = redisTemplate.keys(k);
        redisTemplate.delete(keys);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {

        return objectConvertBean(redisTemplate.opsForValue().get(key), clazz);
    }

    @Override
    public <T> T get(String key, Class<T> clazz, long expire, TimeUnit timeUnit) {

        Object value = redisTemplate.opsForValue().get(key);

        redisTemplate.expire(key, expire, timeUnit);

        return objectConvertBean(value, clazz);
    }

}

