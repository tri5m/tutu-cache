package io.github.tri5m.tucache.core.aspect;

import io.github.tri5m.tucache.core.annotation.TuCache;
import io.github.tri5m.tucache.core.annotation.TuCacheClear;
import io.github.tri5m.tucache.core.bean.TuConditionProcess;
import io.github.tri5m.tucache.core.bean.TuKeyGenerate;
import io.github.tri5m.tucache.core.bean.impl.DefaultTuKeyGenerate;
import io.github.tri5m.tucache.core.cache.TuCacheService;
import io.github.tri5m.tucache.core.config.TuCacheProfiles;
import io.github.tri5m.tucache.core.pool.TucacheDefaultThreadPool;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * aop implementation of cache annotations
 *
 * @author wangxudong
 * @date 2020/08/28
 * @see TuCache,TuCacheClear
 */
@Slf4j
@Aspect
public class TuCacheAspect implements DisposableBean, InitializingBean, BeanFactoryAware {

    private BeanFactory beanFactory;

    /**
     * 需要 tuCacheService 如果没有注入则发生异常
     */
    @Setter
    private TuCacheService tuCacheService;

    /**
     * tuCache 的 key 生成器，如果没有注入，则使用默认的生成器
     */
    @Setter
    private TuKeyGenerate tuKeyGenerate;

    /**
     * 设置默认值
     */
    @Setter
    private TuCacheProfiles tuCacheProfiles = new TuCacheProfiles();

    /**
     * 异步处理的线程池  synchronous
     */
    @Setter
    private ExecutorService syncExecutorService;

    @Around("@annotation(io.github.tri5m.tucache.core.annotation.TuCache)")
    public Object cache(ProceedingJoinPoint pjp) throws Throwable {
        if (tuCacheService != null) {
            Object targetObj = pjp.getTarget();
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();

            return processTuCache(method.getAnnotation(TuCache.class), targetObj, method,
                    pjp.getArgs(), method.getReturnType(), pjp::proceed);
        }

        return pjp.proceed();
    }


    @Around("@annotation(io.github.tri5m.tucache.core.annotation.TuCacheClear)")
    public Object clear(ProceedingJoinPoint pjp) throws Throwable {

        if (tuCacheService != null) {
            Object targetObj = pjp.getTarget();
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();

            processTuCacheClear(method.getAnnotation(TuCacheClear.class), targetObj,
                    method, pjp.getArgs());
        }

        return pjp.proceed();
    }

    @Override
    public void afterPropertiesSet() {

        if (tuCacheService == null) {
            log.warn("TuCacheService at least one implementation, or closed tu-cache[tucache.enable=false]");
        }
        // 如果没有注入tuKeyGenerate 则使用默认的KeyGenerate
        if (this.tuKeyGenerate == null) {
            this.tuKeyGenerate = new DefaultTuKeyGenerate(beanFactory);
        }

        if (tuCacheService != null) {
            if (syncExecutorService == null) {
                syncExecutorService = TucacheDefaultThreadPool.getInstance(tuCacheProfiles).getPool();
            }

        }
    }

    @Override
    public void destroy() {
        if (syncExecutorService != null && !syncExecutorService.isShutdown()) {
            syncExecutorService.shutdownNow();
            log.info("tu-cache is destroyed.");
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

        this.beanFactory = beanFactory;
    }

    /**
     * 处理TuCache缓存
     */
    private Object processTuCache(TuCache tuCache, Object targetObj, Method method,
                                  Object[] args, Class<?> returnType, ResultSupplier<Object> resultSup)
            throws Throwable {

        if (tuCache != null && !returnType.equals(void.class)
                && new TuConditionProcess(this.beanFactory).accept(tuCache.condition(), targetObj, method, args)) {

            String spElKey = StringUtils.hasLength(tuCache.key()) ? tuCache.key() : tuCache.value();

            String cacheKey = tuKeyGenerate.generate(tuCacheProfiles, spElKey, targetObj, method, args);

            Object cacheResult;

            long timeout = tuCache.timeout();
            // 从缓存中获取数据，如果出错，则直接返回方法处理
            try {
                if (tuCache.resetExpire() && timeout >= 0) {
                    // Get data and reset the expiration time
                    cacheResult = tuCacheService.get(cacheKey, returnType, timeout, tuCache.timeUnit());
                } else {
                    cacheResult = tuCacheService.get(cacheKey, returnType);
                }

            } catch (Exception e) {

                log.warn("cache miss, read error. key:{}", cacheKey);
                log.error(e.getMessage(), e);

                return resultSup.get();
            }
            // 如果缓存中没有数据就放入，否则直接返回缓存的数据
            // 如果缓存中返回的是null，就认为没有缓存，直接运行方法获取最新数据
            if (cacheResult == null) {
                cacheResult = resultSup.get();
                try {
                    if (cacheResult != null) {
                        final Object finaCacheResult = cacheResult;
                        debugLog("tu-cache write to cache.");
                        if (timeout == -1) {
                            if (tuCache.async()) {
                                syncExecutorService.execute(() -> tuCacheService.set(cacheKey, finaCacheResult));
                            } else {
                                tuCacheService.set(cacheKey, cacheResult);
                            }
                        } else {
                            if (tuCache.async()) {
                                syncExecutorService.execute(() -> tuCacheService.set(cacheKey, finaCacheResult,
                                        timeout, tuCache.timeUnit()));
                            } else {
                                tuCacheService.set(cacheKey, cacheResult, timeout, tuCache.timeUnit());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("cache miss, write error. key:{}", cacheKey);
                    log.error(e.getMessage(), e);
                }
            }

            debugLog("tu-cache hit cache.");
            return cacheResult;
        }

        return resultSup.get();
    }

    /**
     * 处理TuCacheClear缓存
     */
    private void processTuCacheClear(TuCacheClear tuCacheClear, Object targetObj, Method method, Object[] args) {

        if (tuCacheClear != null && new TuConditionProcess(this.beanFactory)
                .accept(tuCacheClear.condition(), targetObj, method, args)) {

            String[] key = tuCacheClear.key().length == 0 ? tuCacheClear.value() : tuCacheClear.key();
            String[] keys = tuCacheClear.keys();

            try {
                debugLog("tu-cache remove cache.");
                for (String item : key) {
                    String cKey = tuKeyGenerate.generate(tuCacheProfiles, item, targetObj, method, args);
                    if (tuCacheClear.async()) {
                        syncExecutorService.execute(() -> tuCacheService.delete(cKey));
                    } else {
                        tuCacheService.delete(cKey);
                    }
                }
                for (String item : keys) {
                    String cKey = tuKeyGenerate.generate(tuCacheProfiles, item, targetObj, method, args);
                    if (tuCacheClear.async()) {
                        syncExecutorService.execute(() -> tuCacheService.deleteKeys(cKey));
                    } else {
                        tuCacheService.deleteKeys(cKey);
                    }
                }
            } catch (Exception e) {
                log.warn("failed to remove cache.");
                log.error(e.getMessage(), e);
            }
        }
    }

    private void debugLog(String msg) {

        if (tuCacheProfiles.isEnableDebugLog()) {
            log.debug(msg);
        }
    }
}
