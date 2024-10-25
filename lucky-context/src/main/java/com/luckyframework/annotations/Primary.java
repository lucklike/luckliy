package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 优先注解，当使用类型匹配出现多个组件时
 * 被本注解标注的将会被优先获取
 * @author fk
 * @version 1.0
 * @date 2021/4/12 0012 15:24
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}
