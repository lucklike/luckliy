package com.luckyframework.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Windows操作系统条件器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/26 20:17
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnOS("windows")
public @interface ConditionalOnWindows {
}
