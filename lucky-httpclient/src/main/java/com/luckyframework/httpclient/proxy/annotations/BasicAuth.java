package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.httpclient.proxy.impl.BasicAuthParameterSetter;
import com.luckyframework.httpclient.proxy.impl.BasicAuthStaticParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
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
@StaticParam
public @interface BasicAuth {

    /**
     * 用户名,支持SpEL表达式
     */
    String username();

    /**
     * 密码,支持SpEL表达式
     */
    String password();

    //----------------------------------------------------------------
    //                   @StaticParam注解规范必要参数
    //----------------------------------------------------------------

    Class<? extends ParameterSetter> paramSetter() default BasicAuthParameterSetter.class;

    String paramSetterMsg() default "";

    Class<? extends StaticParamResolver> paramResolver() default BasicAuthStaticParamResolver.class;

    String paramResolverMsg() default "";
}
