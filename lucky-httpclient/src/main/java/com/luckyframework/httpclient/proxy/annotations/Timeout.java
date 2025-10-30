package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.setter.TimeoutSetter;
import com.luckyframework.httpclient.proxy.statics.TimeoutStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 超时时间参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(
        setter = @ObjectGenerate(TimeoutSetter.class),
        resolver = @ObjectGenerate(TimeoutStaticParamResolver.class)
)
public @interface Timeout {


    /**
     * 连接超时时间
     */
    int connectionTimeout() default -1;

    /**
     * 连接超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String connectionTimeoutExp() default "";

    /**
     * 读取超时时间
     */
    int readTimeout() default -1;

    /**
     * 读取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String readTimeoutExp() default "";

    /**
     * 写超时时间
     */
    int writeTimeout() default -1;

    /**
     * 写超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String writeTimeoutExp() default "";

}
