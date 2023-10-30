package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.httpclient.proxy.impl.SpELURLGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 域名配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DomainName {

    /**
     * 请求的域名配置，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *     通过{@link HttpClientProxyObjectFactory#addExpressionParams(Map)}、{@link HttpClientProxyObjectFactory#addExpressionParam(String, Object)}方法设置的参数
     *     pn: 参数列表第n个参数
     *     an: 参数列表第n个参数
     *     argsn:参数列表第n个参数
     *     paramName: 参数名称为paramName的参数
     * }
     *
     * $mc$:     当前方法上下文{@link MethodContext}
     * $cc$:     当前类上下文{@link ClassContext}
     * $class$:  当前执行的接口所在类{@link Class}
     * $method$: 当前执行的接口方法实例{@link Method}
     * </pre>
     */
    String value() default "";

    /**
     * URL路径获取器
     */
    Class<? extends URLGetter> getter() default SpELURLGetter.class;

    /**
     * 用于创建URL路径获取器的额外信息
     */
    String getterMsg() default "";

}
