package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.url.SpELURLGetter;
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
 * @see Connect
 * @see Trace
 * @see Get
 * @see Post
 * @see Put
 * @see Delete
 * @see Options
 * @see Head
 * @see Patch
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@FactoryUnpack
public @interface HttpRequest {

    /**
     * 定义http请求的Url信息，同url()
     */
    @AliasFor("url")
    String value() default "";

    /**
     * 定义http请求的Url信息，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @AliasFor("value")
    String url() default "";

    /**
     * 指定用于获取URL的函数
     */
    String func() default "";

    /**
     * 定义http请求的Method
     */
    RequestMethod method() default RequestMethod.NON;

    /**
     * URL路径获取器生成器
     */
    ObjectGenerate urlGetter() default @ObjectGenerate(SpELURLGetter.class);

}
