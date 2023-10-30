package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.impl.setter.TimeoutSetter;
import com.luckyframework.httpclient.proxy.impl.statics.TimeoutStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Basic Auth 参数配置注解
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
@StaticParam(paramSetter = TimeoutSetter.class, paramResolver = TimeoutStaticParamResolver.class)
public @interface Timeout {

    /**
     * 连接超时时间
     */
    int connectionTimeout() default -1;

    /**
     * <pre>
     * 连接超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *
     * $mc$:      当前方法上下文{@link MethodContext}
     * $cc$:      当前类上下文{@link ClassContext}
     * $class$:   当前执行的接口所在类{@link Class}
     * $method$:  当前执行的接口方法实例{@link Method}
     * $ann$:     当前{@link StaticParam @StaticParam}注解实例
     * pn:        参数列表第n个参数
     * an:        参数列表第n个参数
     * argsn:     参数列表第n个参数
     * paramName: 参数名称为paramName的参数
     * </pre>
     */
    String connectionTimeoutExp() default "";

    /**
     * 读取超时时间
     */
    int readTimeout() default -1;

    /**
     * <pre>
     * 读取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *
     * $mc$:      当前方法上下文{@link MethodContext}
     * $cc$:      当前类上下文{@link ClassContext}
     * $class$:   当前执行的接口所在类{@link Class}
     * $method$:  当前执行的接口方法实例{@link Method}
     * $ann$:     当前{@link StaticParam @StaticParam}注解实例
     * pn:        参数列表第n个参数
     * an:        参数列表第n个参数
     * argsn:     参数列表第n个参数
     * paramName: 参数名称为paramName的参数
     * </pre>
     */
    String readTimeoutExp() default "";

    /**
     * 写超时时间
     */
    int writeTimeout() default -1;

    /**
     * <pre>
     * 写超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *
     * $mc$:      当前方法上下文{@link MethodContext}
     * $cc$:      当前类上下文{@link ClassContext}
     * $class$:   当前执行的接口所在类{@link Class}
     * $method$:  当前执行的接口方法实例{@link Method}
     * $ann$:     当前{@link StaticParam @StaticParam}注解实例
     * pn:        参数列表第n个参数
     * an:        参数列表第n个参数
     * argsn:     参数列表第n个参数
     * paramName: 参数名称为paramName的参数
     * </pre>
     */
    String writeTimeoutExp() default "";

}
