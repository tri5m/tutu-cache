package io.github.tri5m.tucache.autoconfigure.test;

import io.github.tri5m.tucache.autoconfigure.configure.TuCacheAutoConfigure;
import io.github.tri5m.tucache.core.aspect.TuCacheAspect;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TuCacheAutoConfigureExecutorServiceTest {

    @Test
    public void consecutiveContextsCreateFreshUsableExecutors() throws Exception {
        AnnotationConfigApplicationContext firstContext = createContext();
        ExecutorService firstExecutor = firstContext.getBean(ExecutorService.class);
        Assert.assertTrue(firstExecutor instanceof ThreadPoolExecutor);
        Assert.assertEquals("first", firstExecutor.submit(() -> "first").get(5, TimeUnit.SECONDS));

        firstContext.close();
        Assert.assertTrue(firstExecutor.isShutdown());

        AnnotationConfigApplicationContext secondContext = createContext();
        ExecutorService secondExecutor = secondContext.getBean(ExecutorService.class);

        try {
            Assert.assertNotSame(firstExecutor, secondExecutor);
            Assert.assertFalse(secondExecutor.isShutdown());
            Assert.assertEquals("second", secondExecutor.submit(() -> "second").get(5, TimeUnit.SECONDS));
        } finally {
            secondContext.close();
        }

        Assert.assertTrue(secondExecutor.isShutdown());
    }

    @Test
    public void customExecutorServiceIsUsedInsteadOfDefaultPool() throws Exception {
        AnnotationConfigApplicationContext context = createContext(UserExecutorConfig.class);
        ExecutorService customExecutor = context.getBean("customExecutorService", ExecutorService.class);

        try {
            Assert.assertSame(customExecutor, context.getBean(ExecutorService.class));
            Assert.assertSame(customExecutor, getExecutorService(context.getBean(TuCacheAspect.class)));

            Future<String> result = customExecutor.submit(() -> "custom");
            Assert.assertEquals("custom", result.get(5, TimeUnit.SECONDS));
        } finally {
            context.close();
        }

        Assert.assertTrue(customExecutor.isShutdown());
    }

    private AnnotationConfigApplicationContext createContext(Class<?>... extraConfigs) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testTuCache", properties()));
        for (Class<?> extraConfig : extraConfigs) {
            context.register(extraConfig);
        }
        context.register(TuCacheAutoConfigure.class);
        context.refresh();
        return context;
    }

    private Map<String, Object> properties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("tucache.enabled", "true");
        properties.put("tucache.cache-type", "LOCAL");
        properties.put("tucache.profiles.pool.core-thread-num", "1");
        properties.put("tucache.profiles.pool.max-thread-num", "2");
        properties.put("tucache.profiles.pool.keep-alive-time", "100");
        return properties;
    }

    private ExecutorService getExecutorService(TuCacheAspect aspect) throws Exception {
        Field field = TuCacheAspect.class.getDeclaredField("syncExecutorService");
        field.setAccessible(true);
        return (ExecutorService) field.get(aspect);
    }

    @Configuration
    static class UserExecutorConfig {
        @Bean(destroyMethod = "shutdownNow")
        public ExecutorService customExecutorService() {
            return Executors.newSingleThreadExecutor();
        }
    }
}
