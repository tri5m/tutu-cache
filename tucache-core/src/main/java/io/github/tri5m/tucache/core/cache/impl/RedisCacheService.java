package io.github.tri5m.tucache.core.cache.impl;

import io.github.tri5m.tucache.core.cache.AbstractTuCacheService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The default TuCacheService implementation class
 *
 * @author: wangxudong
 * @date: 2019/3/14
 */
public class RedisCacheService extends AbstractTuCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int SCAN_BATCH_SIZE = 1000;

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
        if (key == null) {
            return;
        }

        if (!key.contains("*")) {
            redisTemplate.delete(key);
            return;
        }

        deleteByScan(key);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {

        return objectConvertBean(redisTemplate.opsForValue().get(key), clazz);
    }

    @Override
    public <T> T get(String key, Class<T> clazz, long expire, TimeUnit timeUnit) {

        Object value = redisTemplate.opsForValue().get(key);

        if (expire != NOT_EXPIRE) {
            redisTemplate.expire(key, expire, timeUnit);
        }

        return objectConvertBean(value, clazz);
    }

    private void deleteByScan(String pattern) {
        redisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(SCAN_BATCH_SIZE)
                    .build();
            List<byte[]> keys = new ArrayList<>(SCAN_BATCH_SIZE);
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                    if (keys.size() >= SCAN_BATCH_SIZE) {
                        deleteBatch(connection, keys);
                    }
                }
            }
            deleteBatch(connection, keys);
            return null;
        });
    }

    private void deleteBatch(RedisConnection connection, List<byte[]> keys) {
        if (!keys.isEmpty()) {
            connection.keyCommands().del(keys.toArray(new byte[0][]));
            keys.clear();
        }
    }

}
