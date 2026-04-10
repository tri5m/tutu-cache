package io.github.tri5m.tucache.autoconfigure.configure;

import io.github.tri5m.tucache.core.aspect.TuCacheAspect;
import io.github.tri5m.tucache.core.bean.TuKeyGenerate;
import io.github.tri5m.tucache.core.bean.impl.DefaultTuKeyGenerate;
import io.github.tri5m.tucache.core.cache.AbstractTuCacheService;
import io.github.tri5m.tucache.core.cache.TuCacheService;
import io.github.tri5m.tucache.core.cache.impl.LocalCacheService;
import io.github.tri5m.tucache.core.cache.impl.RedisCacheService;
import io.github.tri5m.tucache.core.cache.impl.RedissonCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * tu-cache springboot auto configuration
 *
 * @author wangxudong
 * @date 2020/08/28
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({TuCacheService.class})
@AutoConfigureAfter({CacheAutoConfiguration.class})
@EnableConfigurationProperties(TuCacheProfilesConfigure.class)
@Import({TuCacheAutoConfigure.TuCacheServiceSelector.class})
@ConditionalOnProperty(prefix = "tucache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TuCacheAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public TuCacheAspect tuCacheAspect(TuCacheProfilesConfigure tuCacheConfigure,
                                       ObjectProvider<TuCacheService> tuCacheServices,
                                       ObjectProvider<TuKeyGenerate> tuKeyGenerates) {

        TuCacheService tuCacheService = selectTuCacheService(tuCacheServices, tuCacheConfigure.getCacheType());
        Assert.notNull(tuCacheService, "tu-cache TuCacheService bean does not exist.");

        log.info("tu-cache service: {}", tuCacheService.getClass().getTypeName());

        TuCacheAspect tuCacheAspect = new TuCacheAspect();
        tuCacheAspect.setTuCacheProfiles(tuCacheConfigure.getProfiles());
        tuCacheAspect.setTuCacheService(tuCacheService);

        tuKeyGenerates.ifAvailable(tuCacheAspect::setTuKeyGenerate);

        return tuCacheAspect;
    }

    @Bean
    @ConditionalOnMissingBean
    public TuKeyGenerate tuKeyGenerate(BeanFactory beanFactory) {

        return new DefaultTuKeyGenerate(beanFactory);
    }

    /**
     * TuCacheService bean选择器
     * 根据 <custom> > REDISSON > REDIS > LOCAL 的优先级选用
     * 如果用户指定了cache-type，但是并没有自动注入相应的缓存组件，则抛出异常。
     */
    private TuCacheService selectTuCacheService(ObjectProvider<TuCacheService> tuCacheServices,
                                                TuCacheProfilesConfigure.CacheType cacheType) {

        TuCacheService tuCacheService;

        switch (cacheType) {
            case LOCAL:
                tuCacheService = tuCacheServices.stream().filter(cs -> cs instanceof LocalCacheService)
                        .findFirst().orElseThrow(() -> new BeanCreationException("LocalCacheService",
                                "LocalCacheService bean does not exist, but tucache.cache-type=" + cacheType));
                break;
            case REDIS:
                tuCacheService = tuCacheServices.stream().filter(cs -> cs instanceof RedisCacheService)
                        .findFirst().orElseThrow(() -> new BeanCreationException("RedisCacheService",
                                "RedisCacheService bean does not exist, but tucache.cache-type=" + cacheType));
                break;
            case REDISSON:
                tuCacheService = tuCacheServices.stream().filter(cs -> cs instanceof RedissonCacheService)
                        .findFirst().orElseThrow(() -> new BeanCreationException("RedissonCacheService",
                                "RedissonCacheService bean does not exist, but tucache.cache-type=" + cacheType));
                break;
            default:
                // 根据 <custom> > REDISSON > REDIS > LOCAL 的优先级选用，
                // 如果有@Primary注解的或者是唯一的TuCacheService则直接选择
                tuCacheService = tuCacheServices.getIfUnique();
                if (tuCacheService != null) {
                    break;
                }

                // 用户自定义优先选择
                tuCacheService = tuCacheServices.stream().filter(cs -> !(cs instanceof AbstractTuCacheService))
                        .findFirst().orElse(null);
                if (tuCacheService != null) {
                    break;
                }

                tuCacheService = tuCacheServices.stream().filter(cs -> (cs instanceof RedissonCacheService))
                        .findFirst().orElse(null);
                if (tuCacheService != null) {
                    break;
                }

                tuCacheService = tuCacheServices.stream().filter(cs -> (cs instanceof RedisCacheService))
                        .findFirst().orElse(null);
                if (tuCacheService != null) {
                    break;
                }

                tuCacheService = tuCacheServices.stream().filter(cs -> (cs instanceof LocalCacheService))
                        .findFirst().orElse(null);
                if (tuCacheService != null) {
                    break;
                }
        }

        return tuCacheService;
    }

    /**
     * 注入所有的缓存组件配置Configure
     * 简化@Import注解
     */
    static class TuCacheServiceSelector implements ImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{
                    "io.github.tri5m.tucache.autoconfigure.configure.cache.RedissonCacheServiceConfigure",
                    "io.github.tri5m.tucache.autoconfigure.configure.cache.RedisCacheServiceConfigure",
                    "io.github.tri5m.tucache.autoconfigure.configure.cache.LocalCacheServiceConfigure"
            };
        }
    }
}
