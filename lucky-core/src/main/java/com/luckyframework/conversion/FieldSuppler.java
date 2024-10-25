package com.luckyframework.conversion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 属性初始化注解
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/2 08:50
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(FieldSupplers.class)
public @interface FieldSuppler {

    /** 属性名*/
    String name();

    /** 初始化该属性的SpEL表达式*/
    String suppler();
}
