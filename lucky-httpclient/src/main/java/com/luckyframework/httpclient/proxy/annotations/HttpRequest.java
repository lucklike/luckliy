package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.httpclient.proxy.impl.SpELURLGetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个http请求方法的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HttpRequest {

    /**
     * 定义http请求的Url信息
     */
    @AliasFor("url")
    String value() default "";

    /**
     * 定义http请求的Url信息，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor("value")
    String url() default "";

    /**
     * 定义http请求的Method
     */
    RequestMethod method() default RequestMethod.NON;

    /**
     * URL路径获取器
     */
    Class<? extends URLGetter> urlGetter() default SpELURLGetter.class;

    /**
     * 用于创建URL路径获取器的额外信息
     */
    String urlGetterMsg() default "";

}
