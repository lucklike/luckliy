package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为某个SpEl函数工具类制定一个固定的前缀
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/1514 15:23
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FunctionPrefix {

    /**
     * 方法前缀
     */
    @AliasFor("prefix")
    String value() default "";

    /**
     * 方法前缀
     */
    @AliasFor("value")
    String prefix() default "";
}
