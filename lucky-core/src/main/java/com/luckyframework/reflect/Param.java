package com.luckyframework.reflect;

import java.lang.annotation.*;

/**
 * @author fk7075
 * @version 1.0
 * @date 2020/11/16 13:07
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    String value();

    String def() default "null";
}
