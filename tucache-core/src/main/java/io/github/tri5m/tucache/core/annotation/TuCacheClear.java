package io.github.tri5m.tucache.core.annotation;

import io.github.tri5m.tucache.core.aspect.TuCacheAspect;

import java.lang.annotation.*;

/**
 * This annotation on a method that specifies that the key or keys will clean up the response cache
 * 该注解在方法上，指定key或者keys将会清理响应的缓存
 *
 * @author wangxudong
 * @date 2020/08/28
 * @see TuCacheAspect
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface TuCacheClear {

    /**
     * delete the corresponding cache, keys means delete exact keys unless the value contains wildcard "*".
     * If there is a cache with a key of example:123:keys, use keys = "example:*".
     * all caches starting with example: will be deleted
     * You can specify multiple keys and keys.
     * <p></p>
     * Use explicit wildcard patterns for fuzzy deletion, e.g.
     * <pre>key = "aaa:bbb:ccc"</pre> using <pre>keys = aaa:bbb:*</pre> deletes children under aaa:bbb.
     * <p></p>
     * Try not to use keys mirroring cache deletion, which leads to very slow deletion,
     * or async = true to enable asynchronous deletion, which does not avoid memory pressure if there are too many keys.
     * Without wildcard "*", keys deletes only the exact key.
     * Alias for {@link #key()}
     * <p></p>
     * 尽量不使用keys镜像缓存删除，这会导致删除的速度非常缓慢，或者async = true开启异步删除
     * ，但无法避免如果key过多导致的内存压力。
     * 注意，模糊删除需要显式使用 "*"，不带 "*" 时只删除精确 key。
     */
    String[] value() default {};

    /**
     * Alias for {@link #value()}.
     */
    String[] key() default {};

    /**
     * @see #value
     */
    String[] keys() default {};

    /**
     * Whether to perform cache clearing asynchronously. If the real-time performance of cache deletion is not strictly
     * required, you can use asynchronous cache clearing to improve performance
     * 是否异步执行缓存清理，如果不严格要求缓存删除的实时性，提高性能可以使用异步方式进行清除缓存
     */
    boolean async() default false;

    /**
     * Whether to clear cache before method invocation.
     * <p>
     * Default false means clear cache only after the annotated method succeeds.
     * 设置为 true 时在方法执行前清理缓存；默认为 false，方法成功执行后才清理缓存。
     */
    boolean beforeInvocation() default false;

    String condition() default "true";

}
