package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * Lookup
 * @author FK-7075
 * @version 1.0.0
 * @date 2022/5/13 23:03
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

    String value() default "";

}
