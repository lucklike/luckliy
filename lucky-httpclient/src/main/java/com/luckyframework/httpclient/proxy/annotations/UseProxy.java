package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.setter.ProxyParameterSetter;
import com.luckyframework.httpclient.proxy.statics.ProxyStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Proxy;

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
@StaticParam(setter = @ObjectGenerate(ProxyParameterSetter.class), resolver = @ObjectGenerate(ProxyStaticParamResolver.class))
public @interface UseProxy {

    /**
     * 代理类型，默认为HTTP代理
     */
    Proxy.Type type() default Proxy.Type.HTTP;

    /**
     * IP,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String host();

    /**
     * 端口,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String port();

    /**
     * 用户名
     */
    String username() default "";

    /**
     * 密码
     */
    String password() default "";

}
