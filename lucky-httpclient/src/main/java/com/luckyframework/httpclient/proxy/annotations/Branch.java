package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.MethodContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 条件选择器中的分支定义
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:22
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Branch {

    /**
     * <pre>
     * 断言SpEL表达式, <b>SpEL表达式部分需要写在#{}中</b>
     * 返回值必须是{@link Boolean}类型
     * </pre>
     * <pre>
     * SpEL表达式内置参数有：
     *
     * root:             当前响应的响应体部分{@link Response#getEntity(Class)}
     * $req$:            当前响应对应的请求信息{@link Request}
     * $resp$:           当前响应信息{@link Response}
     * $status$:         当前响应的状态码{@link Integer}
     * $contentType$:    当前响应的Content-Type{@link Integer}
     * $contentLength$:  当前响应的Content-Length{@link Integer}
     * $headers$:        当前响应头信息{@link HttpHeaderManager#getHeaderMap()}
     * $mc$:             当前方法上下文{@link MethodContext}
     * $cc$:             当前类上下文{@link ClassContext}
     * $class$:          当前执行的接口所在类{@link Class}
     * $method$:         当前执行的接口方法实例{@link Method}
     * $ann$:            当前{@link ResultSelect @ResultSelect}注解实例
     * pn:               参数列表第n个参数
     * an:               参数列表第n个参数
     * argsn:            参数列表第n个参数
     * paramName:        参数名称为paramName的参数
     *
     * </pre>
     */
    String assertion();

    /**
     * 结果表达式，这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     * <pre>
     * SpEL表达式内置参数有：
     *
     * root:             当前响应的响应体部分{@link Response#getEntity(Class)}
     * $req$:            当前响应对应的请求信息{@link Request}
     * $resp$:           当前响应信息{@link Response}
     * $status$:         当前响应的状态码{@link Integer}
     * $contentType$:    当前响应的Content-Type{@link Integer}
     * $contentLength$:  当前响应的Content-Length{@link Integer}
     * $headers$:        当前响应头信息{@link HttpHeaderManager#getHeaderMap()}
     * $mc$:             当前方法上下文{@link MethodContext}
     * $cc$:             当前类上下文{@link ClassContext}
     * $class$:          当前执行的接口所在类{@link Class}
     * $method$:         当前执行的接口方法实例{@link Method}
     * $ann$:            当前{@link ResultSelect @ResultSelect}注解实例
     * pn:               参数列表第n个参数
     * an:               参数列表第n个参数
     * argsn:            参数列表第n个参数
     * paramName:        参数名称为paramName的参数
     *
     * </pre>
     */
    String result();


    /**
     * 返回值类型
     */
    Class<?> returnType() default Object.class;
}
