package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 排除某些类型的组件
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/24 下午11:05
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Exclude {

    @AliasFor(attribute = "inheritedFrom")
    Class<?>[] value() default {};

    @AliasFor(attribute = "value")
    Class<?>[] inheritedFrom() default {};

    Class<?>[] equals() default {};
}
