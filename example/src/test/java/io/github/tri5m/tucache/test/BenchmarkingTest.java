package io.github.tri5m.tucache.test;

/**
 * BenchmarkingTest
 *
 * @author: trifolium.wang
 * @date: 2025/4/9
 */

import io.github.tri5m.tucache.core.cache.TuCacheService;
import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import io.github.tri5m.tucache.core.config.TuCacheProfiles;
import io.github.tri5m.tucache.core.pool.TucacheDefaultThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BenchmarkingTest {

    @Test
    public void benchmarkingTest() {
        // 基准测试(单线程)，测试平台，m1pro 8c 默认线程池配置
        // 测试50万数据的[写入]，[查询]，[随机查询]，[删除]。
        TuCacheService tuCacheService = new LocalCacheService();
        String[] art = new String[]{"a", "b", "c", "d", "e", "f", "g", ":", ":"};
        int dataCount = 500_000;
        String[] keys = new String[dataCount];

        log.info("准备测试数据.");

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < dataCount; i++) {
            String key = Stream.generate(() -> art[random.nextInt(9)]).limit(50).collect(Collectors.joining());
            while (key.startsWith(":")) {
                key = key.substring(1);
            }
            keys[i] = key;
        }

        log.info("测试插入数据");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < dataCount; i++) {
            tuCacheService.set(keys[i], keys[i], 7, TimeUnit.SECONDS);
        }
        log.info("耗时0：{} ms", System.currentTimeMillis() - startTime);

        log.info("测试查询数据");
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataCount; i++) {
            tuCacheService.get(keys[i], String.class);
        }
        log.info("耗时3：{} ms", System.currentTimeMillis() - startTime);

        log.info("测试删除数据2");
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataCount; i++) {
            tuCacheService.delete(keys[i]);
        }
        log.info("耗时1：{} ms", System.currentTimeMillis() - startTime);
    }

    @Test
    public void concurrentBenchmarkTest() {
        // 并发测试
        TuCacheService tuCacheService = new LocalCacheService();
        ThreadPoolExecutor pool = TucacheDefaultThreadPool.getInstance(new TuCacheProfiles()).getPool();

        String[] art = new String[]{"a", "b", "c", "d", "e", "f", "g", ":", ":"};
        long startTime = System.currentTimeMillis();

        // 每次500个[写入，查询，删除]任务
        int taskNum = 500;

        // 每个任务1000个数据
        int multiple = 1000;

        // 数据矩阵
        String[][] keys = new String[taskNum][multiple];
        for (int i = 0; i < taskNum; i++) {
            int finalI = i;
            // 写入数据
            pool.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < multiple; j++) {
                    String key = Stream.generate(() -> art[random.nextInt(9)]).limit(50).collect(Collectors.joining());
                    while (key.startsWith(":")) {
                        key = key.substring(1);
                    }
                    keys[finalI][j] = key;
                    tuCacheService.set(key, random.nextInt(), random.nextInt(100), TimeUnit.SECONDS);
                }
            });

            // 查询存在的数据
            pool.submit(() -> {
                for (int j = 0; j < multiple; j++) {
                    tuCacheService.get(keys[finalI][j], int.class);
                }
            });

            // 随机查询包含不存在的数据
            pool.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < multiple; j++) {
                    String key = keys[finalI][j];
                    if (random.nextBoolean()) {
                        key = key + "_";
                    }
                    tuCacheService.get(key, int.class);
                }
            });

            // 删除数据
            pool.submit(() -> {
                for (int j = 0; j < multiple; j++) {
                    tuCacheService.delete(keys[finalI][j]);
                }
            });
        }

        log.info("耗时2：{} ms", System.currentTimeMillis() - startTime);
    }
}
