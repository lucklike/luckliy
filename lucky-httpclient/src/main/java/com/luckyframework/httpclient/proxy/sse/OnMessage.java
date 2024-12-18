package com.luckyframework.httpclient.proxy.sse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/19 01:10
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface OnMessage {

    String value();

    int priority() default Integer.MAX_VALUE;

}
