package com.luckyframework.proxy.conversion;

import com.luckyframework.annotations.Plugin;
import com.luckyframework.conversion.Interconversion;
import com.luckyframework.conversion.UseConversion;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Plugin
@UseConversion
public @interface Conversion {

    @AliasFor(annotation = Plugin.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = UseConversion.class, attribute = "classes")
    Class<?>[] useClasses() default Interconversion.class;

    @AliasFor(annotation = UseConversion.class, attribute = "names")
    String[] useNames() default {};

    @AliasFor(annotation = UseConversion.class, attribute = "importPackages")
    String[] importPackages() default {};


}
