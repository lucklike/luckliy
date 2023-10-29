package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.setter.ProxyParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.ProxyStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 代理参数配置注解
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
@StaticParam(paramSetter=ProxyParameterSetter.class, paramResolver = ProxyStaticParamResolver.class)
public @interface Proxy {

    /**
     * 代理类型，默认为HTTP代理
     */
    java.net.Proxy.Type type() default java.net.Proxy.Type.HTTP;

    /**
     * IP,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    String ip();

    /**
     * 端口,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    String port();

}
