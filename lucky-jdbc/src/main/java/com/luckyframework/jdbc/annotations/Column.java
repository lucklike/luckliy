package com.luckyframework.jdbc.annotations;

import com.luckyframework.conversion.TargetField;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.FIELD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TargetField
public @interface Column {

    @AliasFor(annotation = TargetField.class, attribute = "value")
    String value() default "";
}
