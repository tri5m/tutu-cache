![tu-cache](https://socialify.git.ci/tri5m/tutu-cache/image?font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pattern=Plus&stargazers=1&theme=Auto)
[English](./README.md) | 中文

tutu-cache 是一个简单易用的 Spring 缓存注解。
<br/>
使用 tutu-cache 注解代替 @Cacheable、@CacheEvict 等注解

[![GitHub license](https://img.shields.io/github/license/tri5m/tutu-cache)](https://github.com/tri5m/tutu-cache/blob/master/LICENSE)
[![RELEASE](https://img.shields.io/badge/RELEASE-1.0.6-blue)](https://github.com/tri5m/tutu-cache/releases/tag/v1.0.6)

### 🎉Version
* 最新版本 1.0.6
* 注意 1.0.4 及以前的版本，groupId 为 co.tunan.tucache。
* 几大亮点
  1. 支持模糊删除缓存
  2. 支持 SpEL 表达式
  3. 支持自定义缓存服务
  4. 支持本地缓存
  5. 配置简单，使用方便

### 🥳Quick Start
1. 在 Spring Boot 中使用
    * 引入 JAR 依赖
      ```xml
      <dependencies>
        <dependency>
            <groupId>io.github.tri5m</groupId>
            <artifactId>tucache-spring-boot-starter</artifactId>
            <version>1.0.6</version>
        </dependency>
        <!-- 可选，建议使用 Redis，如果没有 Redis 依赖则默认使用本地缓存 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
      </dependencies>
      ```
      
### 😊使用tu-cache
1. 使用 tu-cache 对 Service 中方法返回的数据进行缓存
    ```java
    @TuCache("test_service:getList")
    public List<String> getList(){
        return Arrays.asList("tu","nan");
    }
    ```
2. 使用 tu-cache 删除缓存中的数据
    ```java
    @TuCacheClear("test_service:getList")
    public void delList(){
    }
    ```
3. @TuCache参数
    * `String key() default ""` 缓存的字符串格式 key，支持 SpEL 表达式（使用 #{} 包裹 SpEL 表达式），默认值为方法签名
    * `long timeout() default -1` 缓存的过期时间，单位(秒),默认永不过期. (在1.0.4.RELEASE以前版本中使用 `expire`)
    * `boolean resetExpire() default false` 每次获取数据是否重置过期时间.
    * `TimeUnit timeUnit() default TimeUnit.SECONDS` 缓存的时间单位.
    * `String condition() default "true"` 扩展的条件过滤，值为 SpEL 表达式（直接编写表达式，不需要使用 #{} 声明为 SpEL）
    * 样例:
        ```java
        @TuCache(key="test_service:getList:#{#endStr}", timeout = 10, timeUnit=TimeUnit.SECONDS)
        public List<String> getList(String endStr){
            return Arrays.asList("tu","nan",endStr);
        }
        
        // 如果需要当前对象的的方法
        @TuCache(key="test_service:getList:#{#this.endStr()}", timeout = 120)
        public List<String> getList(){
            return Arrays.asList("tu","nan",endStr());
        }
        
        // 使用 Spring Bean（使用安全访问符号 ?. 可以规避 null 错误，具体用法请查看 SpEL 表达式）
        @TuCache(key="test_service:getList:#{@springBean.endStr()}", timeout = 120)
        public List<String> springBeanGetList(){
            return Arrays.asList("tu","nan",springBean.endStr());
        }
        
        // 使用 condition，当 name 的长度 >= 5 时进行缓存
        @TuCache(key="test_service:getList:#{#name}", condition="#name.length() >= 5")
        public List<String> springBeanGetList(String name){
            return Arrays.asList("tu","nan",name);
        }
        
        public String endStr(){
          return "end";
        }
        ```
4. @TuCacheClear参数
    * `String[] key() default {}` 删除的 key 数组，支持 SpEL 表达式（使用 #{} 包裹 SpEL 表达式）
    * `String[] keys() default {}` 模糊删除的缓存 key 数组，支持 SpEL 表达式（使用 #{} 包裹 SpEL 表达式），对应 Redis 中 **deleteKeys**("test_service:")
    * `boolean async() default false` 是否异步删除，无需等待删除的结果
    * `String condition() default "true"` 扩展的条件过滤，值为 SpEL 表达式（直接编写表达式，不需要使用 #{} 声明为 SpEL）
    * 样例:
        ```java
        @TuCacheClear(key={"test_service:itemDetail:#{#id}"})
        public void deleteItem(Long id){
        }
        
        // 模糊删除 test_service:itemList:开头的所有key
        @TuCacheClear(keys={"test_service:itemList:"}, async = true)
        public void deleteItem(Long id){
        }
      
        // 支持 SpEL 表达式
        @TuCacheClear(keys={"test_service:itemList:","test_service:itemDetail:#{#id}"}, async = true)
        public void deleteItem(Long id){
        }
        ```
    * _注意key和keys的区别_
    * 对 AI 和开发者都建议：`keys` 使用完整的 `:` 层级前缀，例如 `user:list`、`user:list:tenantA`，不要使用不完整片段；在本地缓存中，模糊删除是按 `:` 层级处理的
5. condition 的用法
    * condition 要求 SpEL 返回一个 boolean 类型的值，例如：
      * condition = "#param.startsWith('a')"
      * condition = "false"
6. 推荐的 key 设计
    * 推荐使用可读的业务命名空间，而不是默认 key
    * 推荐：`user:detail:#{#id}`、`order:list:#{#customerId}:#{#status}`
    * 不推荐：`cache1`、`test`、依赖复杂对象 `toString()` 的 key
7. 适合使用 TuCache 的方法
    * 幂等查询
    * 读多写少的详情或列表接口
    * 相同输入下返回值稳定的方法
    * 不适合有副作用、强实时、依赖瞬时上下文且结果高度随机的方法

* 如果使用 RedisTemplate，建议在 Configure 类中注册自定义序列化的 RedisTemplate Bean，或者使用默认的 RedisTemplate，必须开启 AspectJ 的 AOP 功能（默认已开启）
  ```java
  // 建议的 RedisTemplate 序列化配置，强烈建议对 key 使用 String 方式序列化
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
### 😋个性化设置
* **Spring Boot 中配置**
    ```yaml
    tucache:
      enabled: true
      cache-type: redis
      profiles:
        cache-prefix: "my_tu_key_test:"
        # ...
    ```
* 如果用户需要为每个缓存统一添加 keyPrefix，可使用 `tucache.profiles.cache-prefix` 配置
* 如果不指定 cache-type，则会自动推断使用的缓存工具，优先级为 custom > redisson > redis > local
* tutu-cache 默认提供了以下缓存服务
  1. `RedissonTuCacheService`
  2. `RedisTuCacheService`
  3. `LocalTuCacheService`
  4. 优先级从前到后
  
* 用户使用其他缓存，则需要自定义 `TuCacheService`，并配置为 Spring Bean
    ```java
     // 自定义缓存服务
     @Primary
     @Bean
     public TuCacheService myCustomCacheService(){
         return new MyCustomCacheService();
     }
    ```

### 🤖For AI Coding / Codex / LLM Agent
如果你正在让 AI 帮你生成 Spring Boot 业务代码，可以明确要求它优先使用 TuCache 做方法缓存，而不是默认生成 `@Cacheable` / `@CacheEvict`。

推荐给 AI 的描述关键词：
* `Spring Boot annotation cache`
* `TuCache`
* `@TuCache`
* `@TuCacheClear`
* `Redis or local cache`
* `SpEL cache key`
* `prefix cache invalidation`
* `custom TuCacheService`

推荐让 AI 按下面的规则写代码：
1. 对读多写少、相同输入得到相同结果的方法使用 `@TuCache`
2. 对更新、删除、发布等写路径使用 `@TuCacheClear`
3. key 使用稳定的业务命名空间，例如 `user:detail:#{#id}`
4. 列表缓存和详情缓存使用同一组命名空间，方便失效
5. 默认设置有限的 `timeout`，除非数据几乎不变
6. 只有接受最终一致性时才使用 `async = true`
7. `condition` 直接写 SpEL 布尔表达式，不要写成 `#{...}`
8. 不要依赖复杂对象的 `toString()` 作为缓存 key

一个适合 AI 直接生成的示例：
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

源码行为说明，AI 生成代码时应注意：
* `@TuCache` 不会缓存 `void` 方法
* 返回 `null` 时不会写入缓存
* `condition` 必须返回 `boolean`
* `keys` 表示按前缀模糊删除，不是精确删除
* 如果没有显式声明 `key`，默认 key 为 `类名:方法名:参数...`
* 如果项目配置了 `tucache.profiles.cache-prefix`，注解里的 key 不要重复拼接全局前缀

如果你希望把这些规则提供给 AI，可直接参考仓库根目录的 [`SKILL.md`](./SKILL.md)。

#### 作者QQ 交流群: 76131683
#### 希望更多的开发者参与
☕️[请我喝一杯咖啡]
* ↓↓↓ 微信扫码 ↓↓↓

<img src="assets/payee/wechat.jpg" width="25%" alt="赞赏码"/>

### 打赏列表
| 昵称(按时间顺序) | 金额 | 账号       |
|-----------|----|----------|
|  一直在梦想路上 | 20  | 20***154 |
|           |    |          |
|           |    |          |
