package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用ID注入
 * @author fk
 * @version 1.0
 * @date 2021/3/19 0019 15:33
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Qualifier {

    /**
     * 指定一个bean的Name
     */
    String value() default "";

    /**
     * Declares whether the annotated dependency is required.
     * Defaults to {@code true}.
     */
    boolean required() default true;
}
