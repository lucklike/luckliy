package com.luckyframework.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpEL表达式，包导入
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 15:49
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SpELImport {

    @AliasFor("packages")
    String[] value() default {};

    @AliasFor("value")
    String[] packages() default {};

}
