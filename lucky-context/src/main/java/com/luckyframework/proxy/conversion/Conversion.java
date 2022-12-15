package com.luckyframework.proxy.conversion;

import com.luckyframework.annotations.Plugin;
import com.luckyframework.conversion.UseConversion;
import com.luckyframework.conversion.Interconversion;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

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
