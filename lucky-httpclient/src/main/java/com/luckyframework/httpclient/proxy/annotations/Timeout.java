package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.impl.BasicAuthStaticParamResolver;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.httpclient.proxy.impl.TimeoutSetter;
import com.luckyframework.httpclient.proxy.impl.TimeoutStaticParamResolver;

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
public @interface Timeout {

    /**
     * 连接超时时间
     */
    int connectionTimeout() default -1;

    /**
     * 读取超时时间
     */
    int readTimeout() default -1;

    /**
     * 写超时时间
     */
    int writeTimeout() default -1;


    //----------------------------------------------------------------
    //                   @StaticParam注解规范必要参数
    //----------------------------------------------------------------

    Class<? extends ParameterSetter> paramSetter() default TimeoutSetter.class;

    String paramSetterMsg() default "";

    Class<? extends StaticParamResolver> paramResolver() default TimeoutStaticParamResolver.class;

    String paramResolverMsg() default "";
}
