package com.luckyframework.jdbc.annotations;

import com.luckyframework.conversion.TargetField;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TargetField
public @interface Column {

    @AliasFor(annotation = TargetField.class, attribute = "value")
    String value() default "";
}
