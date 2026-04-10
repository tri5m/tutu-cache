package io.github.tri5m.tucache.core.bean.impl;

import io.github.tri5m.tucache.core.bean.TuKeyGenerate;
import io.github.tri5m.tucache.core.config.TuCacheProfiles;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * tu-cache key default generator
 *
 * @author wangxudong
 * @date 2020/08/28
 */
public class DefaultTuKeyGenerate implements TuKeyGenerate {

    private final BeanFactory beanFactory;

    public DefaultTuKeyGenerate(BeanFactory beanFactory){

        this.beanFactory = beanFactory;
    }

    @Override
    public String generate(TuCacheProfiles profiles, String originKey, Object rootObject, Method method, Object[] arguments) {

        // SpEL表达式为空默认返回方法名
        if (!StringUtils.hasLength(originKey)) {
            // 生成默认的key
            return defaultKey(method, arguments);
        }
        ExpressionParser parser = new SpelExpressionParser();
        ParserContext parserContext = new ParserContext() {
            @Override
            public boolean isTemplate() {
                return true;
            }

            @Override
            public String getExpressionPrefix() {
                return "#{";
            }

            @Override
            public String getExpressionSuffix() {
                return "}";
            }
        };
        StandardEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, arguments,
                new DefaultParameterNameDiscoverer());

        // 加入使用@符号访问bean能力
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));

        String keyPrefix = "";
        if (profiles.getCachePrefix() != null) {
            keyPrefix = profiles.getCachePrefix();
        }

        return keyPrefix + parser.parseExpression(originKey, parserContext).getValue(context, String.class);
    }

    private String defaultKey(Method method, Object[] args) {
        String defaultKey = method.getDeclaringClass().getName() + ":" + method.getName();
        StringBuilder builder = new StringBuilder(defaultKey);
        for (Object arg : args) {
            builder.append(":").append(ObjectUtils.nullSafeToString(arg));
        }

        return builder.toString();
    }

}
