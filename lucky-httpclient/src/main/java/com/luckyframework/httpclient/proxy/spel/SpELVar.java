package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpEL表达式，变量声明
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 15:49
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SpELVar {

    @AliasFor("root")
    String[] value() default {};

    @AliasFor("value")
    String[] root() default {};

    String[] var() default {};

}
