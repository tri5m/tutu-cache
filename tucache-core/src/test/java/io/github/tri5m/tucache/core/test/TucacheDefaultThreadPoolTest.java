package io.github.tri5m.tucache.core.test;

import io.github.tri5m.tucache.core.config.TuCacheProfiles;
import io.github.tri5m.tucache.core.pool.TucacheDefaultThreadPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TucacheDefaultThreadPoolTest {

    @Test
    public void createPoolReturnsIndependentPools() {
        ThreadPoolExecutor first = TucacheDefaultThreadPool.createPool(profiles(1, 2, 100));
        ThreadPoolExecutor second = TucacheDefaultThreadPool.createPool(profiles(1, 2, 100));

        try {
            Assert.assertNotSame(first, second);
            first.shutdownNow();

            Assert.assertTrue(first.isShutdown());
            Assert.assertFalse(second.isShutdown());
        } finally {
            first.shutdownNow();
            second.shutdownNow();
        }
    }

    @Test
    public void createPoolUsesProfileThreadCounts() {
        ThreadPoolExecutor pool = TucacheDefaultThreadPool.createPool(profiles(2, 4, 123));

        try {
            Assert.assertEquals(2, pool.getCorePoolSize());
            Assert.assertEquals(4, pool.getMaximumPoolSize());
            Assert.assertEquals(123, pool.getKeepAliveTime(TimeUnit.MILLISECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void invalidThreadCountsAreNormalized() {
        ThreadPoolExecutor pool = TucacheDefaultThreadPool.createPool(profiles(0, -1, 100));

        try {
            Assert.assertEquals(1, pool.getCorePoolSize());
            Assert.assertEquals(1, pool.getMaximumPoolSize());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void repeatedCreateShutdownAndSubmitDoesNotReuseShutdownPool() throws InterruptedException {
        for (int round = 0; round < 50; round++) {
            ThreadPoolExecutor pool = TucacheDefaultThreadPool.createPool(profiles(1, 3, 100));
            AtomicInteger counter = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(30);

            try {
                for (int i = 0; i < 30; i++) {
                    pool.execute(() -> {
                        counter.incrementAndGet();
                        done.countDown();
                    });
                }

                Assert.assertTrue("round " + round + " timed out", done.await(5, TimeUnit.SECONDS));
                Assert.assertEquals(30, counter.get());
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Test
    public void concurrentPoolCreationReturnsUsableIndependentPools() throws InterruptedException {
        int poolCount = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(poolCount);
        List<ThreadPoolExecutor> pools = new ArrayList<>();

        for (int i = 0; i < poolCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    ThreadPoolExecutor pool = TucacheDefaultThreadPool.createPool(profiles(1, 2, 100));
                    synchronized (pools) {
                        pools.add(pool);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }

        start.countDown();
        Assert.assertTrue(done.await(5, TimeUnit.SECONDS));

        CountDownLatch taskDone = new CountDownLatch(poolCount);
        try {
            for (ThreadPoolExecutor pool : pools) {
                Assert.assertFalse(pool.isShutdown());
                pool.execute(taskDone::countDown);
            }
            Assert.assertTrue(taskDone.await(5, TimeUnit.SECONDS));
        } finally {
            for (ThreadPoolExecutor pool : pools) {
                pool.shutdownNow();
            }
        }
    }

    private TuCacheProfiles profiles(int core, int max, long keepAliveMillis) {
        TuCacheProfiles profiles = new TuCacheProfiles();
        profiles.getPool().setCoreThreadNum(core);
        profiles.getPool().setMaxThreadNum(max);
        profiles.getPool().setKeepAliveTime(keepAliveMillis);
        return profiles;
    }
}
