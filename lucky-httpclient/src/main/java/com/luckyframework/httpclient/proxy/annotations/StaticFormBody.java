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
@Combination({StaticBody.class})
@StaticBody(mimeType = "application/x-www-form-urlencoded")
public @interface StaticFormBody {


    /**
     * Body配置，支持SpEL表达式，表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @AliasFor(annotation = StaticBody.class, attribute = "body")
    String value();

    /**
     * 进行URL编码时采用的编码方式
     */
    @AliasFor(annotation = StaticBody.class, attribute = "charset")
    String charset() default "UTF-8";

}
