package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Web层组件
 * @author fk
 * @version 1.0
 * @date 2021/3/19 0019 15:32
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {

    @AliasFor(annotation = Component.class)
    String value() default "";
}
