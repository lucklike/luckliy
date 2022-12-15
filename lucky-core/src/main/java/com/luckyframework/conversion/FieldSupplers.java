package com.luckyframework.conversion;

import java.lang.annotation.*;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/2 08:50
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldSupplers {

    FieldSuppler[] value();

}
