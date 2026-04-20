---
name: tucache-caching
description: Use this skill when writing or modifying Spring Boot application code that should use TuCache for method-result caching or cache invalidation with `@TuCache` and `@TuCacheClear`.
---

# TuCache Caching

Use this skill when the codebase already uses TuCache, or when a Spring Boot service/controller needs lightweight annotation-based caching.

Read [`README.md`](README.md) first for public usage, then confirm behavior from source when making non-trivial changes.

## What TuCache Does

TuCache caches method return values with `@TuCache` and clears exact or prefix-based keys with `@TuCacheClear`.

Core behavior from source:

- `@TuCache` only applies when the method returns a value. `void` methods are never cached.
- If a cache read fails, the original method still executes.
- `null` results are not written to cache.
- If no `key`/`value` is provided, the default key is `fully.qualified.ClassName:methodName:arg1:arg2...`.
- `condition` is raw SpEL and must evaluate to `boolean`.
- `key`/`value` support SpEL templates with `#{...}`.
- `keys` in `@TuCacheClear` means prefix deletion, not exact deletion.

Primary implementation points:

- [`README.md`](README.md)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/aspect/TuCacheAspect.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/aspect/TuCacheAspect.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/annotation/TuCache.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/annotation/TuCache.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/annotation/TuCacheClear.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/annotation/TuCacheClear.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/bean/impl/DefaultTuKeyGenerate.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/bean/impl/DefaultTuKeyGenerate.java)

## Before Writing Code

Check these first:

1. The project is Spring Boot based.
2. TuCache starter dependency is available:
   ```xml
   <dependency>
       <groupId>io.github.tri5m</groupId>
       <artifactId>tucache-spring-boot-starter</artifactId>
       <version>1.0.6</version>
   </dependency>
   ```
3. `tucache.enabled` is not disabled.
4. A cache backend is available.

Backend selection is auto-configured in this order:

- custom `TuCacheService`
- `RedissonClient`
- `RedisTemplate`
- local in-memory cache

Relevant source:

- [`tucache-spring-boot-autoconfigure/src/main/java/io/github/tri5m/tucache/autoconfigure/configure/TuCacheAutoConfigure.java`](tucache-spring-boot-autoconfigure/src/main/java/io/github/tri5m/tucache/autoconfigure/configure/TuCacheAutoConfigure.java)
- [`tucache-spring-boot-autoconfigure/src/main/java/io/github/tri5m/tucache/autoconfigure/configure/TuCacheProfilesConfigure.java`](tucache-spring-boot-autoconfigure/src/main/java/io/github/tri5m/tucache/autoconfigure/configure/TuCacheProfilesConfigure.java)

## Default Coding Strategy

When adding caching to business code, prefer this order:

1. Put `@TuCache` on service methods that are read-heavy, deterministic for the same input, and side-effect free.
2. Put `@TuCacheClear` on the write path that changes the same data.
3. Use explicit, stable, namespaced keys instead of relying on the default key.
4. Use SpEL only for the minimal identifying fields.
5. Set a finite `timeout` unless the data is effectively immutable.

Default key format to generate manually:

```java
@TuCache(key = "user:detail:#{#userId}", timeout = 5, timeUnit = TimeUnit.MINUTES)
public UserDTO getUserDetail(Long userId) {
    ...
}
```

This is better than the implicit key because it is readable, stable across refactors, and easier to invalidate.

## Key Design Rules

When generating keys:

- Start with a business namespace like `user:detail`, `order:list`, `product:search`.
- Append only the fields that actually affect the result.
- Keep the same namespace family for reads and invalidation.
- Prefer scalar identifiers over serializing whole objects into the key.
- If multiple methods share the same cached resource, standardize one key format.

Good examples:

```java
@TuCache(key = "user:detail:#{#id}", timeout = 10, timeUnit = TimeUnit.MINUTES)
public User getById(Long id) { ... }

@TuCache(key = "order:list:#{#customerId}:#{#status}", timeout = 30, timeUnit = TimeUnit.SECONDS)
public List<Order> listOrders(Long customerId, String status) { ... }
```

Avoid:

- keys based on `toString()` of complex request objects
- keys that omit a result-shaping argument
- opaque keys like `"cache1"` with no namespace

## Invalidation Rules

Use exact deletion when the changed data maps to a specific cache entry.

```java
@TuCacheClear(key = "user:detail:#{#id}")
public void updateUser(Long id, UpdateUserCommand command) { ... }
```

Use prefix deletion with `keys` when one write affects a family of cache entries.

```java
@TuCacheClear(
    key = "user:detail:#{#id}",
    keys = "user:list",
    async = true
)
public void deleteUser(Long id) { ... }
```

Important limitation:

- Local cache prefix deletion is hierarchical by `:` segments.
- Use complete namespace levels such as `user:list` or `user:list:tenantA`.
- Do not rely on partial fragment matching like `user:li`.

Relevant source:

- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/LocalCacheService.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/LocalCacheService.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/localcache/TuTreeCache.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/localcache/TuTreeCache.java)

## SpEL Rules

TuCache supports:

- method parameters: `#{#id}`
- nested fields: `#{#user.name}`
- current object: `#{#this.methodName()}`
- Spring beans: `#{@someBean.method()}`

For `condition`, write plain SpEL without `#{}`:

```java
@TuCache(
    key = "user:search:#{#keyword}",
    condition = "#keyword != null && #keyword.length() >= 2",
    timeout = 1,
    timeUnit = TimeUnit.MINUTES
)
public List<User> search(String keyword) { ... }
```

Prefer `condition` when:

- very short or invalid input should bypass cache
- per-user highly random requests should not be cached
- the method is only worth caching for part of the input domain

## Async And Expiration

Use `async = true` only when eventual cache write/delete is acceptable.

- `@TuCache(async = true)` makes cache writes asynchronous.
- `@TuCacheClear(async = true)` makes invalidation asynchronous.

Use `resetExpire = true` only for hot keys that should stay alive on repeated reads.

```java
@TuCache(
    key = "config:public",
    timeout = 10,
    timeUnit = TimeUnit.MINUTES,
    resetExpire = true
)
public ConfigDTO getPublicConfig() { ... }
```

## Backend Guidance

Prefer Redis or Redisson for shared caches across instances.

Use local cache only when all of these are true:

- single-node deployment is acceptable, or per-node cache divergence is acceptable
- data volume is limited
- prefix invalidation volume is small

If Redis is used, prefer a `RedisTemplate<String, Object>` with string keys and JSON-friendly value serialization, as shown in [`README.md`](README.md).

## Code Generation Checklist

When asked to add caching, do this:

1. Find the read method.
2. Confirm it is safe to cache.
3. Design a readable key namespace.
4. Choose a timeout.
5. Add `@TuCache`.
6. Find the write/delete path that mutates the same data.
7. Add matching `@TuCacheClear`.
8. If the codebase has `tucache.profiles.cache-prefix`, do not duplicate the global prefix inside annotation keys.

## Patterns To Prefer

Cache detail queries:

```java
@TuCache(key = "article:detail:#{#id}", timeout = 15, timeUnit = TimeUnit.MINUTES)
public ArticleDTO getArticle(Long id) { ... }
```

Cache paged or filtered lists only when the filter set is explicit and bounded:

```java
@TuCache(
    key = "article:list:#{#category}:#{#page}:#{#size}",
    timeout = 30,
    timeUnit = TimeUnit.SECONDS
)
public Page<ArticleDTO> listArticles(String category, int page, int size) { ... }
```

Clear both detail and list caches after mutation:

```java
@TuCacheClear(
    key = "article:detail:#{#command.id}",
    keys = "article:list",
    async = true
)
public void updateArticle(UpdateArticleCommand command) { ... }
```

## Patterns To Avoid

Do not generate TuCache code in these cases:

- methods with side effects
- highly volatile values where stale reads are unacceptable
- methods that often return `null` if you expect negative caching, because TuCache does not cache `null`
- keys derived from unstable object string representations
- broad `keys` invalidation on very large keyspaces without a strong reason

## If You Need To Go Deeper

Inspect these files before changing framework-level behavior:

- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/aspect/TuCacheAspect.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/aspect/TuCacheAspect.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/TuCacheService.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/TuCacheService.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/RedisCacheService.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/RedisCacheService.java)
- [`tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/RedissonCacheService.java`](tucache-core/src/main/java/io/github/tri5m/tucache/core/cache/impl/RedissonCacheService.java)
- [`example/src/main/java/io/github/tri5m/tucache/example/controller/BaseTestController.java`](example/src/main/java/io/github/tri5m/tucache/example/controller/BaseTestController.java)
