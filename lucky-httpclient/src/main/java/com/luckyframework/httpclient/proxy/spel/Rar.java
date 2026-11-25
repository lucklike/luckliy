package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Var(type = VarType.ROOT)
public @interface Rar {


    /**
     * 变量名
     * <p>仅允许出现在static方法（SpEL函数）的参数上，缺省时回退参数名。
     */
    @AliasFor(annotation = Var.class, attribute = "value")
    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";
}
