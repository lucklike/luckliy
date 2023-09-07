package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.httpclient.proxy.impl.PathParameterSetter;
import com.luckyframework.httpclient.proxy.impl.URLEncodeStaticParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
@StaticParam
public @interface StaticPath {

    /**
     * 路径配置,格式为：key=value,支持SpEL表达式
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

    //----------------------------------------------------------------
    //                   @StaticParam注解规范必要参数
    //----------------------------------------------------------------

    Class<? extends ParameterSetter> paramSetter() default PathParameterSetter.class;

    String paramSetterMsg() default "";

    Class<? extends StaticParamResolver> paramResolver() default URLEncodeStaticParamResolver.class;

    String paramResolverMsg() default "";
}
