package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Root变量
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@VarName
public @interface RootVar {

    @AliasFor(annotation = VarName.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = VarName.class, attribute = "literal")
    boolean literal() default false;

    @AliasFor(annotation = VarName.class, attribute = "scope")
    VarScope scope() default VarScope.CLASS;
}
