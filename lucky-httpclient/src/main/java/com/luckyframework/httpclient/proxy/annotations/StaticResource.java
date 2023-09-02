package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.httpclient.proxy.impl.ResourceParameterSetter;
import com.luckyframework.httpclient.proxy.impl.ResourceStaticParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态资源参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@StaticParam
public @interface StaticResource {

    /**
     * 资源参数配置,格式为：key=value,支持SpEL表达式
     */
    String[] value();

    boolean urlEncode() default false;

    String charset() default "UTF-8";

    //----------------------------------------------------------------
    //                   @StaticParam注解规范必要参数
    //----------------------------------------------------------------

    Class<? extends ParameterSetter> paramSetter() default ResourceParameterSetter.class;

    String paramSetterMsg() default "";

    Class<? extends StaticParamResolver> paramResolver() default ResourceStaticParamResolver.class;

    String paramResolverMsg() default "";
}
