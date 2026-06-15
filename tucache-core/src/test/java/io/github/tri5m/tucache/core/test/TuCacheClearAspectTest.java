package io.github.tri5m.tucache.core.test;

import io.github.tri5m.tucache.core.annotation.TuCacheClear;
import io.github.tri5m.tucache.core.aspect.TuCacheAspect;
import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class TuCacheClearAspectTest {

    @Test
    public void clearRunsAfterSuccessfulInvocationByDefault() throws Throwable {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("clear:key", "value");
        TuCacheAspect aspect = aspect(localCacheService);
        TestTarget target = new TestTarget();
        ProceedingJoinPoint joinPoint = joinPoint(target, method("afterSuccess"), () -> {
            Assert.assertEquals("value", localCacheService.get("clear:key", String.class));
            return "ok";
        });

        Object result = aspect.clear(joinPoint);

        Assert.assertEquals("ok", result);
        Assert.assertNull(localCacheService.get("clear:key", String.class));
    }

    @Test
    public void clearDoesNotRunWhenDefaultAfterInvocationFails() throws Throwable {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("clear:key", "value");
        TuCacheAspect aspect = aspect(localCacheService);
        TestTarget target = new TestTarget();
        ProceedingJoinPoint joinPoint = joinPoint(target, method("afterFailure"), () -> {
            throw new IllegalStateException("boom");
        });

        try {
            aspect.clear(joinPoint);
            Assert.fail("method exception should be propagated");
        } catch (IllegalStateException e) {
            Assert.assertEquals("boom", e.getMessage());
        }

        Assert.assertEquals("value", localCacheService.get("clear:key", String.class));
    }

    @Test
    public void clearRunsBeforeInvocationWhenConfigured() throws Throwable {
        LocalCacheService localCacheService = new LocalCacheService();
        localCacheService.set("clear:key", "value");
        TuCacheAspect aspect = aspect(localCacheService);
        TestTarget target = new TestTarget();
        ProceedingJoinPoint joinPoint = joinPoint(target, method("beforeInvocation"), () -> {
            Assert.assertNull(localCacheService.get("clear:key", String.class));
            return "ok";
        });

        Object result = aspect.clear(joinPoint);

        Assert.assertEquals("ok", result);
        Assert.assertNull(localCacheService.get("clear:key", String.class));
    }

    private TuCacheAspect aspect(LocalCacheService localCacheService) throws Exception {
        TuCacheAspect aspect = new TuCacheAspect();
        aspect.setTuCacheService(localCacheService);
        aspect.setTuKeyGenerate((profiles, originKey, rootObject, method, arguments) -> originKey);
        aspect.setSyncExecutorService(new DirectExecutorService());
        aspect.afterPropertiesSet();
        return aspect;
    }

    static class DirectExecutorService extends AbstractExecutorService {

        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private Method method(String name) throws NoSuchMethodException {
        return TestTarget.class.getMethod(name);
    }

    private ProceedingJoinPoint joinPoint(TestTarget target, Method method, Proceeding proceed) {
        MethodSignature signature = new TestMethodSignature(method);
        return new TestProceedingJoinPoint(target, signature, proceed);
    }

    interface Proceeding {
        Object proceed() throws Throwable;
    }

    static class TestTarget {

        @TuCacheClear(key = "clear:key")
        public void afterSuccess() {
        }

        @TuCacheClear(key = "clear:key")
        public void afterFailure() {
        }

        @TuCacheClear(key = "clear:key", beforeInvocation = true)
        public void beforeInvocation() {
        }
    }

    static class TestProceedingJoinPoint implements ProceedingJoinPoint {

        private final Object target;
        private final MethodSignature signature;
        private final Proceeding proceeding;

        TestProceedingJoinPoint(Object target, MethodSignature signature, Proceeding proceeding) {
            this.target = target;
            this.signature = signature;
            this.proceeding = proceeding;
        }

        @Override
        public Object proceed() throws Throwable {
            return proceeding.proceed();
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return proceeding.proceed();
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }

        @Override
        public Signature getSignature() {
            return signature;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return "method-execution";
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String toShortString() {
            return signature.toShortString();
        }

        @Override
        public String toLongString() {
            return signature.toLongString();
        }

        @Override
        public String toString() {
            return signature.toString();
        }

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {
        }
    }

    static class TestMethodSignature implements MethodSignature {

        private final Method method;

        TestMethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return new String[0];
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }
    }
}
