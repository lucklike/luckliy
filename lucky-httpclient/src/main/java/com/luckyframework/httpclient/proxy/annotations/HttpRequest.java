package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.httpclient.proxy.impl.SpELURLGetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 定义一个http请求方法的注解
 *
 * @see Connect
 * @see Trace
 * @see Get
 * @see Post
 * @see Put
 * @see Delete
 * @see Options
 * @see Head
 * @see Patch
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HttpRequest {

    /**
     * 定义http请求的Url信息，同url()
     */
    @AliasFor("url")
    String value() default "";

    /**
     * 定义http请求的Url信息，支持SpEL表达式，SpEL表达式部分需要写在#{}中
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
    @AliasFor("value")
    String url() default "";

    /**
     * 定义http请求的Method
     */
    RequestMethod method() default RequestMethod.NON;

    /**
     * URL路径获取器
     */
    Class<? extends URLGetter> urlGetter() default SpELURLGetter.class;

    /**
     * 用于创建URL路径获取器的额外信息
     */
    String urlGetterMsg() default "";

}
