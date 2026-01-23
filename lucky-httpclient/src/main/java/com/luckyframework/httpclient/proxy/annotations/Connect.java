package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个Connect请求
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@HttpRequest(method = RequestMethod.CONNECT)
public @interface Connect {

    /**
     * 定义http请求的Url信息，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";

    /**
     * 指定用于获取URL的函数
     */
    @AliasFor(annotation = HttpRequest.class, attribute = "func")
    String func() default "";

}
