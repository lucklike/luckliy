package com.luckyframework.condition;

import com.luckyframework.annotations.Conditional;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnEnvironmentPropertyCondition.class)
public @interface ConditionalOnEnvironmentProperty {

    @AliasFor("propertyName")
    String value() default "";

    @AliasFor("value")
    String propertyName() default "";
}
