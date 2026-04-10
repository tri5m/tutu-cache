package io.github.tri5m.tucache.core.test;

import io.github.tri5m.tucache.core.annotation.TuCache;
import io.github.tri5m.tucache.core.aspect.TuCacheAspect;
import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class TuCacheAspectTest {

    @Test
    public void shouldProceedWhenConditionIsFalse() {
        TuCacheAspect aspect = new TuCacheAspect();
        aspect.setBeanFactory(new DefaultListableBeanFactory());
        aspect.setTuCacheService(new LocalCacheService());
        aspect.afterPropertiesSet();

        ConditionFalseService target = new ConditionFalseService();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        ConditionFalseService proxy = proxyFactory.getProxy();
        String first = proxy.load("demo");
        String second = proxy.load("demo");

        Assert.assertEquals("value-1", first);
        Assert.assertEquals("value-2", second);
        Assert.assertEquals(2, target.getInvokeCount());
    }

    static class ConditionFalseService {
        private int invokeCount;

        @TuCache(key = "condition:false:#{#name}", condition = "false")
        public String load(String name) {
            invokeCount++;
            return "value-" + invokeCount;
        }

        public int getInvokeCount() {
            return invokeCount;
        }
    }
}
