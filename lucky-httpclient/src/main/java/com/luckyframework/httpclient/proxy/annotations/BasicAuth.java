package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.impl.setter.BasicAuthParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.BasicAuthStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;

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
@StaticParam(paramSetter = BasicAuthParameterSetter.class, paramResolver = BasicAuthStaticParamResolver.class)
public @interface BasicAuth {

    /**
     * 用户名,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     * <pre>
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
     *
     * </pre>
     */
    String username();

    /**
     * 密码,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     * <pre>
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
     *
     * </pre>
     */
    String password();
}
