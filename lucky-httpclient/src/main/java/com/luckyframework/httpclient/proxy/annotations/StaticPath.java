package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ClassContext;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.impl.setter.PathParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.URLEncodeStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 静态路径参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(paramSetter = PathParameterSetter.class, paramResolver = URLEncodeStaticParamResolver.class)
public @interface StaticPath {

    /**
     * <pre>
     * 路径配置
     * 格式为：key=value，
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
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
    String[] value();

    /**
     * 是否进行URL编码
     */
    boolean urlEncode() default false;

    /**
     * 进行URL编码时采用的编码方式
     */
    String charset() default "UTF-8";
}
