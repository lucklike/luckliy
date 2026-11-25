package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

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
     * <p>变量的作用域由{@link #type()}决定，本属性只声明它是一个参数注入引用名，
     * 不静态声明具体的变量域。仅允许出现在static方法（SpEL函数）的参数上，
     * 缺省时回退参数名。
     */
    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";

    /**
     * 变量类型
     */
    VarType type() default VarType.NORMAL;
}
