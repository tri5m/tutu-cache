package io.github.tri5m.tucache.core.test;

import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import io.github.tri5m.tucache.core.localcache.TuTreeCache;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheTest {

    private static final long SHORT_TTL_MILLIS = 100;
    private static final long LONG_TTL_MILLIS = 800;

    @Test
    public void deleteKeysWithoutWildcardRemovesOnlyExactKey() {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("pre", "root");
        localCacheService.set("pre:a", "child");
        localCacheService.set("pre:a:b", "grandchild");
        localCacheService.set("prefix", "other");

        localCacheService.deleteKeys("pre");

        Assert.assertNull(localCacheService.get("pre", String.class));
        Assert.assertEquals("child", localCacheService.get("pre:a", String.class));
        Assert.assertEquals("grandchild", localCacheService.get("pre:a:b", String.class));
        Assert.assertEquals("other", localCacheService.get("prefix", String.class));
    }

    @Test
    public void deleteKeysWithHierarchicalWildcardRemovesChildrenOnly() {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("pre", "root");
        localCacheService.set("pre:a", "child");
        localCacheService.set("pre:a:b", "grandchild");
        localCacheService.set("prefix", "other");

        localCacheService.deleteKeys("pre:*");

        Assert.assertEquals("root", localCacheService.get("pre", String.class));
        Assert.assertNull(localCacheService.get("pre:a", String.class));
        Assert.assertNull(localCacheService.get("pre:a:b", String.class));
        Assert.assertEquals("other", localCacheService.get("prefix", String.class));
    }

    @Test
    public void deleteKeysWithTrailingDelimiterWithoutWildcardDoesNotRemoveChildren() {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("pre", "root");
        localCacheService.set("pre:a", "child");

        localCacheService.deleteKeys("pre:");

        Assert.assertEquals("root", localCacheService.get("pre", String.class));
        Assert.assertEquals("child", localCacheService.get("pre:a", String.class));
    }

    @Test
    public void deprecatedTuTreeCacheUsesSameWildcardDeleteSemantics() {
        TuTreeCache tuTreeCache = new TuTreeCache();
        tuTreeCache.putNode("pre", "root", null);
        tuTreeCache.putNode("pre:a", "child", null);
        tuTreeCache.putNode("pre:a:b", "grandchild", null);
        tuTreeCache.putNode("prefix", "other", null);

        tuTreeCache.removeKeys("pre");

        Assert.assertNull(tuTreeCache.searchNode("pre").getObj());
        Assert.assertEquals("child", tuTreeCache.searchNode("pre:a").getObj());
        Assert.assertEquals("grandchild", tuTreeCache.searchNode("pre:a:b").getObj());
        Assert.assertEquals("other", tuTreeCache.searchNode("prefix").getObj());

        tuTreeCache.removeKeys("pre:*");

        Assert.assertNull(tuTreeCache.searchNode("pre:a"));
        Assert.assertNull(tuTreeCache.searchNode("pre:a:b"));
        Assert.assertEquals("other", tuTreeCache.searchNode("prefix").getObj());
    }

    @Test
    public void eachKeyCanHaveDifferentExpireTime() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("short", "short", SHORT_TTL_MILLIS, TimeUnit.MILLISECONDS);
        localCacheService.set("long", "long", LONG_TTL_MILLIS, TimeUnit.MILLISECONDS);

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertNull(localCacheService.get("short", String.class));
        Assert.assertEquals("long", localCacheService.get("long", String.class));
    }

    @Test
    public void defaultSetDoesNotExpire() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("permanent", "value");

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertEquals("value", localCacheService.get("permanent", String.class));
    }

    @Test
    public void negativeExpireDoesNotExpire() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("permanent", "value", -1, TimeUnit.MILLISECONDS);

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertEquals("value", localCacheService.get("permanent", String.class));
    }

    @Test
    public void zeroExpireExpiresImmediately() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("zero", "value", 0, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(20);

        Assert.assertNull(localCacheService.get("zero", String.class));
    }

    @Test
    public void setExistingKeyReplacesValueAndExpireTime() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("key", "old", SHORT_TTL_MILLIS, TimeUnit.MILLISECONDS);
        localCacheService.set("key", "new", LONG_TTL_MILLIS, TimeUnit.MILLISECONDS);

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertEquals("new", localCacheService.get("key", String.class));
    }

    @Test
    public void getWithExpireResetsExpireTime() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("key", "value", SHORT_TTL_MILLIS, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(SHORT_TTL_MILLIS / 2);
        Assert.assertEquals("value", localCacheService.get("key", String.class, LONG_TTL_MILLIS,
                TimeUnit.MILLISECONDS));

        sleepPast(SHORT_TTL_MILLIS);
        Assert.assertEquals("value", localCacheService.get("key", String.class));
    }

    @Test
    public void resetExpireEventuallyExpiresWithNewTimeout() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("key", "value", LONG_TTL_MILLIS, TimeUnit.MILLISECONDS);

        Assert.assertEquals("value", localCacheService.get("key", String.class, SHORT_TTL_MILLIS,
                TimeUnit.MILLISECONDS));

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertNull(localCacheService.get("key", String.class));
    }

    @Test
    public void resetExpireCanMakeKeyPermanent() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("key", "value", SHORT_TTL_MILLIS, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(SHORT_TTL_MILLIS / 2);
        Assert.assertEquals("value", localCacheService.get("key", String.class, -1, TimeUnit.MILLISECONDS));

        sleepPast(SHORT_TTL_MILLIS);

        Assert.assertEquals("value", localCacheService.get("key", String.class));
    }

    @Test
    public void resetExpireDoesNotCreateMissingKey() {
        LocalCacheService localCacheService = new LocalCacheService();

        Assert.assertNull(localCacheService.get("missing", String.class, LONG_TTL_MILLIS, TimeUnit.MILLISECONDS));
        Assert.assertNull(localCacheService.get("missing", String.class));
    }

    @Test
    public void deletedKeyDoesNotComeBackAfterResetExpireStyleGet() {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("key", "value", LONG_TTL_MILLIS, TimeUnit.MILLISECONDS);
        localCacheService.delete("key");

        Assert.assertNull(localCacheService.get("key", String.class, LONG_TTL_MILLIS, TimeUnit.MILLISECONDS));
        Assert.assertNull(localCacheService.get("key", String.class));
    }

    @Test
    public void concurrentWritesDoNotLoseSiblingKeys() throws InterruptedException {
        LocalCacheService localCacheService = new LocalCacheService();
        int taskCount = 64;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            int index = i;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    localCacheService.set("root:key:" + index, index, 1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finish.countDown();
                }
            });
            thread.start();
        }

        start.countDown();
        Assert.assertTrue(finish.await(5, TimeUnit.SECONDS));

        for (int i = 0; i < taskCount; i++) {
            Assert.assertEquals(Integer.valueOf(i), localCacheService.get("root:key:" + i, Integer.class));
        }
    }

    private void sleepPast(long ttlMillis) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(ttlMillis + 80);
    }
}
