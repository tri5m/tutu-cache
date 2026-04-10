package io.github.tri5m.tucache.autoconfigure.configure;

import io.github.tri5m.tucache.core.cache.TuCacheService;
import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import io.github.tri5m.tucache.core.cache.impl.RedisCacheService;
import io.github.tri5m.tucache.core.cache.impl.RedissonCacheService;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TuCacheAutoConfigureTest {

    @Test
    public void shouldPreferRedissonInAutoMode() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("redissonTuCacheService", new RedissonCacheService(mockRedissonClient()));
        beanFactory.registerSingleton("redisTuCacheService", new RedisCacheService(new RedisTemplate<>()));
        beanFactory.registerSingleton("localTuCacheService", new LocalCacheService());

        TuCacheAutoConfigure autoConfigure = new TuCacheAutoConfigure();
        Method method = TuCacheAutoConfigure.class.getDeclaredMethod(
                "selectTuCacheService", org.springframework.beans.factory.ObjectProvider.class,
                TuCacheProfilesConfigure.CacheType.class);
        method.setAccessible(true);

        TuCacheService selected = (TuCacheService) method.invoke(
                autoConfigure, beanFactory.getBeanProvider(TuCacheService.class), TuCacheProfilesConfigure.CacheType.AUTO);

        Assert.assertTrue(selected instanceof RedissonCacheService);
    }

    private RedissonClient mockRedissonClient() {
        InvocationHandler handler = (proxy, method, args) -> {
            throw new UnsupportedOperationException("RedissonClient stub for auto-config selection tests only");
        };
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(), new Class[]{RedissonClient.class}, handler);
    }
}
