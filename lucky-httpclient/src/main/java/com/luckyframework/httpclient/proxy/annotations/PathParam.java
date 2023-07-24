package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 04:16
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpParam(location = ParamLocation.PATH)
public @interface PathParam {

    @AliasFor(annotation = HttpParam.class, attribute = "name")
    String value() default "";

}
