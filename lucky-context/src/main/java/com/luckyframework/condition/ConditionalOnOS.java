package com.luckyframework.condition;

import com.luckyframework.annotations.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作系统条件器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/26 20:02
 */
@Target({ElementType.TYPE, ElementType.METHOD,  ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnOSCondition.class)
public @interface ConditionalOnOS {

    String value();

}
