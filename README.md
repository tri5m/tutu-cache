![tu-cache](https://socialify.git.ci/tri5m/tutu-cache/image?font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pattern=Plus&stargazers=1&theme=Auto)
tutu-cache 是一个简单易用的Spring缓存注解。
<br/>
使用tutu-cache注解来代替@Cacheable和@CacheEvict等注解

[![GitHub license](https://img.shields.io/github/license/tri5m/tutu-cache)](https://github.com/tri5m/tutu-cache/blob/master/LICENSE)
[![RELEASE](https://img.shields.io/badge/RELEASE-1.0.5-blue)](https://github.com/tri5m/tutu-cache/releases/tag/v1.0.5)

### 🎉Version
* 最新版本 1.0.5
* 注意1.0.5以前的版本，groupId为co.tunan.tucache。
* 几大亮点
  1. 支持模糊删除缓存
  2. 支持spEl表达式
  3. 支持自定义缓存服务
  4. 支持本地缓存
  5. 配置简单，使用方便
  
### 🥳Quick Start
1. 在springBoot中的使用
    * 引入jar依赖包
      ```xml
      <dependencies>
        <dependency>
            <groupId>io.github.tri5m</groupId>
            <artifactId>tucache-spring-boot-starter</artifactId>
            <version>1.0.5</version>
        </dependency>
        <!-- 可选，建议使用redis,如有没redis依赖默认使用本地缓存 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
      </dependencies>
      ```
      
### 😊使用tu-cache
1. 使用tu-cache对service中的方法返回的数据进行缓存
    ```java
    @TuCache("test_service:getList")
    public List<String> getList(){
        return Arrays.asList("tu","nan");
    }
    ```
2. 使用tu-cache删除缓存中的数据
    ```java
    @TuCacheClear("test_service:getList")
    public void delList(){
    }
    ```
3. @TuCache参数
    * `String key() default ""` 缓存的字符串格式key,支持spEl表达式(使用#{}包裹spEl表达式)，默认值为方法签名
    * `long timeout() default -1` 缓存的过期时间，单位(秒),默认永不过期. (在1.0.4.RELEASE以前版本中使用 `expire`)
    * `boolean resetExpire() default false` 每次获取数据是否重置过期时间.
    * `TimeUnit timeUnit() default TimeUnit.SECONDS` 缓存的时间单位.
    * `String condition() default "true"` 扩展的条件过滤，值为spEl表达式(直接编写表达式不需要使用#{}方式声明为spEl)
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
        
        // 使用springBean, (使用安全访问符号?.，可以规避null错误，具体用法请查看spEl表达式)
        @TuCache(key="test_service:getList:#{@springBean.endStr()}", timeout = 120)
        public List<String> springBeanGetList(){
            return Arrays.asList("tu","nan",springBean.endStr());
        }
        
        // 使用condition,当name的长度>=5时进行缓存
        @TuCache(key="test_service:getList:#{#name}", condition="#name.length() >= 5")
        public List<String> springBeanGetList(String name){
            return Arrays.asList("tu","nan",name);
        }
        
        public String endStr(){
          return "end";
        }
        ```
4. @TuCacheClear参数
    * `String[] key() default {}` 删除的key数组，支持spEl表达式(使用#{}包裹spEl表达式)
    * `String[] keys() default {}` 模糊删除的缓存key数组,支持spEl表达式(使用#{}包裹spEl表达式),对应redis中**deleteKeys**("test_service:")
    * `boolean async() default false` 是否异步删除，无需等待删除的结果
    * `String condition() default "true"` 扩展的条件过滤，值为spEl表达式(直接编写表达式不需要使用#{}方式声明为spEl)
    * 样例:
        ```java
        @TuCacheClear(key={"test_service:itemDetail:#{#id}"})
        public void deleteItem(Long id){
        }
        
        // 模糊删除 test_service:itemList:开头的所有key
        @TuCacheClear(keys={"test_service:itemList:"}, async = true)
        public void deleteItem(Long id){
        }
      
        // 支持spEl表达式
        @TuCacheClear(keys={"test_service:itemList:","test_service:itemDetail:#{#id}"}, async = true)
        public void deleteItem(Long id){
        }
        ```
    * _注意key和keys的区别_
5. condition 的用法
    * condition要求spEL返回一个boolean类型的值，例如：
      * condition = "#param.startsWith('a')"
      * condition = "false"

* 如果使用redisTemplate, 建议自定义序列化在Configure类中注册javaBean redisTemplate或者使用默认的redisTemplate，必须开启aspectj的aop功能(默认是开启的)
  ```java
  // 建议的redisTemplate序列化配置， 强烈建议使用对key使用String方式序列化
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
* **springBoot中配置**
    ```yaml
    tucache:
      enabled: true
      cache-type: redis
      profiles:
        cache-prefix: "my_tu_key_test:"
        # ...
    ```
* 如果用户需要每个缓存前面添加同意的keyPrefix，TuCacheBean的prefixKey参数
* 如果不指定cache-type，则会自动推断使用的缓存工具，优先级为 custom > redisson > redis > local
* tutu-cache默认提供了一下缓存服务
  1. `RedissonTuCacheService`
  2. `RedisTuCacheService`
  3. `LocalTuCacheService`
  4. 优先级从前往后
  
* 用户使用其他缓存，则需要自定义`TuCacheService`，并配置为spring bean
    ```java
     // 自定义缓存服务
     @Primary
     @Bean
     public TuCacheService myCustomCacheService(){
         return new MyCustomCacheService();
     }
    ```

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