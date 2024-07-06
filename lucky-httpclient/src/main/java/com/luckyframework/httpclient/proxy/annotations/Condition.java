package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件描述注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/05/27 09:30
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Condition {

    /**
     * 条件
     */
    String when() default "";

    /**
     * 满足条件时
     */
    String then() default "";

    /**
     * 不满足条件时
     */
    String not() default "";
}
