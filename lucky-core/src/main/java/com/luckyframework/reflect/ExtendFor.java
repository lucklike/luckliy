package com.luckyframework.reflect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解属性扩展注解，通过该注解可以在原有属性的基础上扩展出多个与子值相同的其他额外的注解属性
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/7
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtendFor {

    /**
     * 需要扩展出的属性名
     */
    String[] value();
}
