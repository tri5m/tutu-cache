![tu-cache](https://socialify.git.ci/tri5m/tutu-cache/image?font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pattern=Plus&stargazers=1&theme=Auto)
English | [中文](./README.zh-CN.md)

tutu-cache is a simple and easy-to-use Spring caching annotation library.
<br/>
Use tutu-cache annotations instead of `@Cacheable`, `@CacheEvict`, and similar annotations.

[![GitHub license](https://img.shields.io/github/license/tri5m/tutu-cache)](https://github.com/tri5m/tutu-cache/blob/master/LICENSE)
[![RELEASE](https://img.shields.io/badge/RELEASE-1.0.6-blue)](https://github.com/tri5m/tutu-cache/releases/tag/v1.0.6)

### 🎉Version
* Latest version: `1.0.6`
* For `1.0.4` and earlier, the `groupId` was `co.tunan.tucache`.
* Highlights
  1. Supports prefix-based cache invalidation
  2. Supports SpEL expressions
  3. Supports custom cache services
  4. Supports local cache
  5. Simple configuration and easy to use

### 🥳Quick Start
1. Use it in Spring Boot
    * Add the dependency
      ```xml
      <dependencies>
        <dependency>
            <groupId>io.github.tri5m</groupId>
            <artifactId>tucache-spring-boot-starter</artifactId>
            <version>1.0.6</version>
        </dependency>
        <!-- Optional. Redis is recommended. If Redis is not present, local cache is used by default. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
      </dependencies>
      ```

### 😊Using TuCache
1. Use TuCache to cache the return value of a Service method
    ```java
    @TuCache("test_service:getList")
    public List<String> getList(){
        return Arrays.asList("tu","nan");
    }
    ```
2. Use TuCache to clear cache data
    ```java
    @TuCacheClear("test_service:getList")
    public void delList(){
    }
    ```
3. `@TuCache` parameters
    * `String key() default ""`: cache key string, supports SpEL expressions wrapped with `#{}`. The default value is the method signature.
    * `long timeout() default -1`: cache expiration time. The default unit is seconds. `-1` means never expire. (`expire` was used before `1.0.4.RELEASE`)
    * `boolean resetExpire() default false`: whether to reset expiration each time the cache is hit.
    * `TimeUnit timeUnit() default TimeUnit.SECONDS`: time unit for cache expiration.
    * `String condition() default "true"`: additional filtering condition. The value is a SpEL expression written directly, without `#{}`.
    * Examples:
        ```java
        @TuCache(key="test_service:getList:#{#endStr}", timeout = 10, timeUnit=TimeUnit.SECONDS)
        public List<String> getList(String endStr){
            return Arrays.asList("tu","nan",endStr);
        }

        // Access a method on the current object
        @TuCache(key="test_service:getList:#{#this.endStr()}", timeout = 120)
        public List<String> getList(){
            return Arrays.asList("tu","nan",endStr());
        }

        // Access a Spring Bean
        // The safe navigation operator ?. can be used to avoid null errors
        @TuCache(key="test_service:getList:#{@springBean.endStr()}", timeout = 120)
        public List<String> springBeanGetList(){
            return Arrays.asList("tu","nan",springBean.endStr());
        }

        // Use condition: cache only when name length >= 5
        @TuCache(key="test_service:getList:#{#name}", condition="#name.length() >= 5")
        public List<String> springBeanGetList(String name){
            return Arrays.asList("tu","nan",name);
        }

        public String endStr(){
          return "end";
        }
        ```
4. `@TuCacheClear` parameters
    * `String[] key() default {}`: exact keys to delete, supports SpEL expressions wrapped with `#{}`.
    * `String[] keys() default {}`: prefix keys for fuzzy deletion, supports SpEL expressions wrapped with `#{}`. This maps to Redis `deleteKeys("test_service:")`.
    * `boolean async() default false`: whether to delete asynchronously.
    * `String condition() default "true"`: additional filtering condition. The value is a SpEL expression written directly, without `#{}`.
    * Examples:
        ```java
        @TuCacheClear(key={"test_service:itemDetail:#{#id}"})
        public void deleteItem(Long id){
        }

        // Delete all keys starting with test_service:itemList:
        @TuCacheClear(keys={"test_service:itemList:"}, async = true)
        public void deleteItem(Long id){
        }

        // Supports SpEL expressions
        @TuCacheClear(keys={"test_service:itemList:","test_service:itemDetail:#{#id}"}, async = true)
        public void deleteItem(Long id){
        }
        ```
    * _Pay attention to the difference between `key` and `keys`._
    * Recommendation for both developers and AI agents: use complete `:`-level prefixes for `keys`, such as `user:list` or `user:list:tenantA`. Do not use incomplete fragments. In local cache mode, fuzzy deletion is processed by `:` hierarchy.
5. `condition` usage
    * `condition` must return a boolean value, for example:
      * `condition = "#param.startsWith('a')"`
      * `condition = "false"`
6. Recommended key design
    * Prefer readable business namespaces instead of the default generated key
    * Recommended: `user:detail:#{#id}`, `order:list:#{#customerId}:#{#status}`
    * Not recommended: `cache1`, `test`, or keys that depend on `toString()` of complex objects
7. Methods that are suitable for TuCache
    * Idempotent queries
    * Detail or list APIs with many reads and few writes
    * Methods whose result is stable for the same input
    * Not suitable for methods with side effects, strict real-time requirements, transient context, or highly random results

* If you use `RedisTemplate`, it is recommended to register a customized serialized `RedisTemplate` bean in a configuration class, or use the default `RedisTemplate`. AspectJ AOP must be enabled, and it is enabled by default.
  ```java
  // Recommended RedisTemplate serialization configuration.
  // Strongly recommended: use String serialization for keys.
  @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(createGenericObjectMapper()));
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(createGenericObjectMapper()));

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }
  ```

### 😋Customization
* Configuration in Spring Boot
    ```yaml
    tucache:
      enabled: true
      cache-type: redis
      profiles:
        cache-prefix: "my_tu_key_test:"
        # ...
    ```
* If you want to add a unified prefix to all keys, use `tucache.profiles.cache-prefix`
* If `cache-type` is not specified, the cache backend is inferred automatically with priority: `custom > redisson > redis > local`
* tutu-cache provides these cache services by default
  1. `RedissonTuCacheService`
  2. `RedisTuCacheService`
  3. `LocalTuCacheService`
  4. Priority is listed from top to bottom

* If you use another cache implementation, provide a custom `TuCacheService` bean
    ```java
     // Custom cache service
     @Primary
     @Bean
     public TuCacheService myCustomCacheService(){
         return new MyCustomCacheService();
     }
    ```

### 🤖For AI Coding / Codex / LLM Agent
If you are asking AI to generate Spring Boot business code, explicitly tell it to prefer TuCache for method caching instead of generating `@Cacheable` / `@CacheEvict` by default.

Recommended keywords for AI:
* `Spring Boot annotation cache`
* `TuCache`
* `@TuCache`
* `@TuCacheClear`
* `Redis or local cache`
* `SpEL cache key`
* `prefix cache invalidation`
* `custom TuCacheService`

Recommended rules for AI-generated code:
1. Use `@TuCache` for read-heavy methods that return the same result for the same input
2. Use `@TuCacheClear` on update, delete, publish, or other write paths
3. Use stable business namespaces for keys, such as `user:detail:#{#id}`
4. Keep list cache and detail cache in the same namespace family for easier invalidation
5. Use a finite `timeout` by default unless the data is effectively immutable
6. Use `async = true` only when eventual consistency is acceptable
7. Write `condition` as a plain SpEL boolean expression, not `#{...}`
8. Do not build cache keys from `toString()` of complex objects

An example suitable for direct AI generation:
```java
@TuCache(key = "user:detail:#{#id}", timeout = 5, timeUnit = TimeUnit.MINUTES)
public UserDTO getUser(Long id) {
    ...
}

@TuCacheClear(
    key = "user:detail:#{#command.id}",
    keys = "user:list",
    async = true
)
public void updateUser(UpdateUserCommand command) {
    ...
}
```

Important source-level behaviors AI should consider:
* `@TuCache` does not cache `void` methods
* `null` return values are not written to cache
* `condition` must return `boolean`
* `keys` means prefix-based invalidation, not exact deletion
* If no explicit `key` is provided, the default key is `ClassName:methodName:args...`
* If the project uses `tucache.profiles.cache-prefix`, do not duplicate that global prefix in annotation keys

If you want to provide these rules to AI directly, see [`SKILL.md`](./SKILL.md) in the project root.
