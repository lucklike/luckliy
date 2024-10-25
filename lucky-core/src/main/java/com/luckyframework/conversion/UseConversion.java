package com.luckyframework.conversion;

import com.luckyframework.spel.SpELImport;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在一个转换器中带入其他转换器的注解
 */
@Target({ElementType.METHOD,ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpELImport
@Documented
public @interface UseConversion {

    /** 其他转换器的名称*/
    String[] names() default {};

    /** 其他转换器的类型Class*/
    Class<?>[] classes() default {};

    @AliasFor(annotation = SpELImport.class, attribute = "packages")
    String[] importPackages() default {};

}
