package com.luckyframework.httpclient.proxy.typeparser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 禁止使用{@link PackTypeParser}来进行类型包装
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/29 03:59
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeWrapProhibition {
}
