package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.handle.SpELHttpExceptionHandle;
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
 * 支持使用SpEL表达式进行异常处理的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExceptionHandle(handle = SpELHttpExceptionHandle.class)
public @interface SpELExceptionHandle {

    String ATTRIBUTE_HANDLE_EXPRESSION = "handleExpression";

    /**
     * 同{@link #handleExpression()}
     */
    @AliasFor("handleExpression")
    String value() default "";


    /**
     * 用于处理异常的表达式，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *     pn:              参数列表第n个参数
     *     an:              参数列表第n个参数
     *     argsn:           参数列表第n个参数
     *     paramName:       参数名称为paramName的参数
     *     $elEnv$:         通过{@link HttpClientProxyObjectFactory#addExpressionParams(Map)}、{@link HttpClientProxyObjectFactory#addExpressionParam(String, Object)}方法设置的参数
     *     $this$:          当前接口的代理对象{@link MethodContext#getProxyObject()}
     *     $throwable$:     异常实例对象{@link Throwable}
     *     $req$:           当前请求对象{@link Request}
     *     $mc$:            当前方法上下文{@link MethodContext}
     *     $cc$:            当前类上下文{@link ClassContext}
     *     $class$:         当前执行的接口所在类{@link Class}
     *     $method$:        当前执行的接口方法实例{@link Method}
     *
     * }
     * </pre>
     */
    @AliasFor("value")
    String handleExpression() default "";

}
