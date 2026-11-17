package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.configapi.EnableConfigurationParser;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;

import java.lang.annotation.*;

/**
 * 注册拦截器的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 02:53
 *
 * @see AutoRedirect
 * @see EnableConfigurationParser
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(InterceptorMetas.class)
public @interface InterceptorMeta {

    /**
     * 用于生成{@link Interceptor} 拦截器对象的生成器
     */
    ObjectGenerate intercept() default @ObjectGenerate(Interceptor.class);

    /**
     * 拦截器Class，使用此配置默认创建单例拦截器
     */
    Class<? extends Interceptor> clazz() default Interceptor.class;

    /**
     * 用于获取拦截器实例的SpEL表达式
     * <pre>
     *     表达式结果必须是如下：
     *     1.{@link Interceptor}实例
     *     2.{@link Class Class&lt;Interceptor&gt;} 拦截器的Class
     * </pre>
     */
    @SpelAttribute(result = {Interceptor.class, Class.class})
    String expression() default "";

    /**
     * 当方法上存在该注解时不执行此拦截器的逻辑
     */
    Class<? extends Annotation> prohibition() default InterceptorProhibition.class;

    /**
     * 优先级，数值越高优先级越低
     */
    int priority() default Integer.MAX_VALUE;

}
