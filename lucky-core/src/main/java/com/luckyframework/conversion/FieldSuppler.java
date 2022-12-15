package com.luckyframework.conversion;

import java.lang.annotation.*;

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
