package io.github.tri5m.tucache.core.localcache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

/**
 * Local cache implementation based on Caffeine.
 *
 * @author trifolium.wang
 */
public class CaffeineCache {

    public static final long NOT_EXPIRE = -1;

    private final Cache<String, CacheValue> cache;

    public CaffeineCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfter(new TuCacheExpiry())
                .build();
    }

    public CaffeineCache(long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfter(new TuCacheExpiry())
                .build();
    }

    public void put(String key, Object value, Long timeoutMillis) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("add local cache key is null.");
        }

        // Store the ttl with the value so Caffeine can expire each key independently.
        cache.put(key, new CacheValue(value, toTtlNanos(timeoutMillis)));
    }

    public Object get(String key) {
        if (key == null) {
            return null;
        }

        CacheValue value = cache.getIfPresent(key);
        return value == null ? null : value.getObj();
    }

    public Object getAndResetExpire(String key, long timeout, TimeUnit timeUnit) {
        if (key == null) {
            return null;
        }

        AtomicReference<Object> result = new AtomicReference<>();
        // Reset only if the key still exists, avoiding a delete/get race that would recreate it.
        cache.asMap().computeIfPresent(key, (k, value) -> {
            result.set(value.getObj());
            return new CacheValue(value.getObj(), timeout < 0 ? Long.MAX_VALUE : timeUnit.toNanos(timeout));
        });
        return result.get();
    }

    public void remove(String key) {
        if (key == null) {
            return;
        }

        cache.invalidate(key);
    }

    /**
     * Remove by hierarchical prefix.
     * <p>
     * keyPrefix = "pre" removes only "pre".
     * keyPrefix = "pre:*" removes children matching "pre:*" and keeps "pre".
     */
    public void removeKeys(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isEmpty()) {
            return;
        }

        if (keyPrefix.endsWith("*")) {
            String prefix = trimTrailingWildcard(keyPrefix);
            cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
            return;
        }

        cache.invalidate(keyPrefix);
    }

    private long toTtlNanos(Long timeoutMillis) {
        if (timeoutMillis == null || timeoutMillis < 0) {
            // Caffeine treats Long.MAX_VALUE as practically never expire.
            return Long.MAX_VALUE;
        }

        return TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    }

    private String trimTrailingWildcard(String key) {
        String result = key;
        while (result.endsWith("*")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Getter
    @AllArgsConstructor
    static class CacheValue {

        private final Object obj;
        // Caffeine's Expiry API works in nanoseconds.
        private final long ttlNanos;
    }

    static class TuCacheExpiry implements Expiry<String, CacheValue> {

        @Override
        public long expireAfterCreate(String key, CacheValue value, long currentTime) {

            return value.getTtlNanos();
        }

        @Override
        public long expireAfterUpdate(String key, CacheValue value, long currentTime, long currentDuration) {

            return value.getTtlNanos();
        }

        @Override
        public long expireAfterRead(String key, CacheValue value, long currentTime, long currentDuration) {

            return currentDuration;
        }
    }
}
