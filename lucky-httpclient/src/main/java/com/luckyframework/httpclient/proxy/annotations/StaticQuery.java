package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.setter.QueryParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.URLEncodeStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态Query参数配置注解
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
@StaticParam(paramSetter = QueryParameterSetter.class, paramResolver = URLEncodeStaticParamResolver.class)
public @interface StaticQuery {

    /**
     * Query配置,格式为：key=value,支持SpEL表达式，SpEL表达式部分需要写在#{}中
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
