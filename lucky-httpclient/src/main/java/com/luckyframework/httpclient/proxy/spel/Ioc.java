package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Var(type = VarType.IOC)
public @interface Ioc {

    @AliasFor(annotation = Var.class, attribute = "value")
    String value() default "";
}
