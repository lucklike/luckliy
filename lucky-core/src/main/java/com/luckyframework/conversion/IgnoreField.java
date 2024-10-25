package com.luckyframework.conversion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 转化过程中指定被忽略信息的注解
 * @author fk7075
 * @version 1.0
 * @date 2020/8/19 15:22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreField {

    /** source对象中被忽略的属性*/
    String[] sources() default {};

    /** target对象中被忽略的属性*/
    String[] targets() default {};
}
