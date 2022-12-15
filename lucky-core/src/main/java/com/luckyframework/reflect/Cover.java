package com.luckyframework.reflect;

import java.lang.annotation.*;

/**
 * 覆盖父类的属性或方法
 * 在使用ClassUtils.getAllFields()和ClassUtils.getAllMethod()方法时
 * 会忽略父类的同名属性或同名方法
 * @author fk7075
 * @version 1.0
 * @date 2020/10/23 1:03
 */
@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cover {
    String value() default "";
}
