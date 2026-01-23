package com.luckyframework.httpclient.proxy.spel;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Var {

    /**
     * 变量名
     */
    String value() default "";

    /**
     * 变量类型
     */
    VarType type() default VarType.NORMAL;
}
