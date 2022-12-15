package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/14 上午2:39
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {
    String[] value();
}
