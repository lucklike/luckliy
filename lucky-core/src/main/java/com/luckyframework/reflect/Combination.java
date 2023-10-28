package com.luckyframework.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组合注解标识
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/19 16:02
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Combination {

    Class<? extends Annotation>[] value();
}
