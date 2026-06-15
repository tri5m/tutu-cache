package io.github.tri5m.tucache.core.test;

import io.github.tri5m.tucache.core.bean.impl.DefaultTuKeyGenerate;
import io.github.tri5m.tucache.core.config.TuCacheProfiles;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.lang.reflect.Method;

public class DefaultTuKeyGenerateTest {

    @Test
    public void defaultKeyIncludesCachePrefix() throws Exception {
        TuCacheProfiles profiles = new TuCacheProfiles();
        profiles.setCachePrefix("prefix:");
        Method method = TestTarget.class.getMethod("query", String.class, int.class);

        String key = new DefaultTuKeyGenerate(null).generate(profiles, "", new TestTarget(), method,
                new Object[]{"name", 7});

        Assert.assertEquals("prefix:" + TestTarget.class.getName() + ":query:name:7", key);
    }

    @Test
    public void explicitKeyIncludesCachePrefix() throws Exception {
        TuCacheProfiles profiles = new TuCacheProfiles();
        profiles.setCachePrefix("prefix:");
        Method method = TestTarget.class.getMethod("query", String.class, int.class);

        String key = new DefaultTuKeyGenerate(new StaticListableBeanFactory()).generate(profiles,
                "user:#{#name}:#{#page}",
                new TestTarget(), method, new Object[]{"name", 7});

        Assert.assertEquals("prefix:user:name:7", key);
    }

    @Test
    public void nullCachePrefixDoesNotAddTextToDefaultKey() throws Exception {
        TuCacheProfiles profiles = new TuCacheProfiles();
        profiles.setCachePrefix(null);
        Method method = TestTarget.class.getMethod("query", String.class, int.class);

        String key = new DefaultTuKeyGenerate(null).generate(profiles, null, new TestTarget(), method,
                new Object[]{"name", 7});

        Assert.assertEquals(TestTarget.class.getName() + ":query:name:7", key);
    }

    public static class TestTarget {

        public String query(String name, int page) {
            return name + page;
        }
    }
}
