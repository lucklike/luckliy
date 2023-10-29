package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.setter.BasicAuthParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.BasicAuthStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     */
    String username();

    /**
     * 密码,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    String password();
}
