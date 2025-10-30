package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

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
@UseProxy(type = Proxy.Type.SOCKS, host = "", port = "")
@Combination({UseProxy.class})
public @interface SocksProxy {

    /**
     * IP,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @AliasFor(annotation = UseProxy.class, attribute = "host")
    String host();

    /**
     * 端口,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @AliasFor(annotation = UseProxy.class, attribute = "port")
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
