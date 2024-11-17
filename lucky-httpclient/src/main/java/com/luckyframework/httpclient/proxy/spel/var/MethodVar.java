package com.luckyframework.httpclient.proxy.spel.var;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用域为{@link VarScope#METHOD}普通变量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Variate(scope = VarScope.METHOD, type = VarType.NORMAL)
public @interface MethodVar {

    /**
     * 变量名
     */
    @AliasFor(annotation = Variate.class, attribute = "value")
    String value() default "";

    /**
     * 是否将变量展开
     */
    @AliasFor(annotation = Variate.class, attribute = "unfold")
    boolean unfold() default false;

}
