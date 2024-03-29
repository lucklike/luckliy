package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.RequestMethod;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@HttpRequest(method = RequestMethod.TRACE)
public @interface Trace {

    @AliasFor(annotation = HttpRequest.class, value = "url")
    String value() default "";

}
