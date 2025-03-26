package com.luckyframework.httpclient.generalapi.openai;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将某个方法声明为一个Function Calling的工具函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * 工具方法的表述信息
     */
    @AliasFor("desc")
    String value() default "";

    /**
     * 工具方法的表述信息
     */
    @AliasFor("value")
    String desc() default "";

    /**
     * 工具方法的名称，不配置时默认使用方法名
     */
    String name() default "";

    /**
     * 工具类型
     */
    String type() default "function";


}
