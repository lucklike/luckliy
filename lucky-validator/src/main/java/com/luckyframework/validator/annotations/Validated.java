package com.luckyframework.validator.annotations;

import java.lang.annotation.*;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/21 11:09
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

    Class<?>[] value() default {};
}
